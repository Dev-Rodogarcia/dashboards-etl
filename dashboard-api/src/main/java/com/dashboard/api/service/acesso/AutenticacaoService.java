package com.dashboard.api.service.acesso;

import com.dashboard.api.dto.LoginResponseDTO;
import com.dashboard.api.dto.SessaoUsuarioDTO;
import com.dashboard.api.dto.SetorSessaoDTO;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import com.dashboard.api.security.GerenciadorTokenJwt;
import com.dashboard.api.security.PermissaoCatalogo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AutenticacaoService {

    private static final Logger log = LoggerFactory.getLogger(AutenticacaoService.class);
    private static final int MAX_TENTATIVAS = 5;
    private static final int LOCKOUT_MINUTOS = 15;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final GerenciadorTokenJwt gerenciadorToken;
    private final PermissaoResolverService permissaoResolver;
    private final AuditService auditService;
    private final PoliticaSenhaService politicaSenhaService;
    private final RefreshTokenService refreshTokenService;

    public AutenticacaoService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            GerenciadorTokenJwt gerenciadorToken,
            PermissaoResolverService permissaoResolver,
            AuditService auditService,
            PoliticaSenhaService politicaSenhaService,
            RefreshTokenService refreshTokenService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.gerenciadorToken = gerenciadorToken;
        this.permissaoResolver = permissaoResolver;
        this.auditService = auditService;
        this.politicaSenhaService = politicaSenhaService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public LoginResponseDTO autenticar(String email, String senha) {
        UsuarioEntity usuario = usuarioRepository.findByEmailIgnoreCase(email.trim()).orElse(null);

        if (usuario == null || !usuario.isAtivo()) {
            auditService.registrarSync(AcaoAudit.LOGIN_FALHA, null, email, "auth", null);
            throw new CredencialInvalidaException("Usuário ou senha inválidos.");
        }

        if (usuario.getBloqueadoAte() != null && Instant.now().isBefore(usuario.getBloqueadoAte())) {
            auditService.registrarSync(AcaoAudit.LOGIN_FALHA, usuario.getId(), usuario.getLogin(), "auth", "{\"motivo\":\"conta_bloqueada\"}");
            throw new CredencialInvalidaException("Conta temporariamente bloqueada. Tente novamente mais tarde.");
        }

        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            usuario.setTentativasFalha(usuario.getTentativasFalha() + 1);
            if (usuario.getTentativasFalha() >= MAX_TENTATIVAS) {
                usuario.setBloqueadoAte(Instant.now().plus(LOCKOUT_MINUTOS, ChronoUnit.MINUTES));
                auditService.registrarSync(AcaoAudit.CONTA_BLOQUEADA, usuario.getId(), usuario.getLogin(), "auth",
                        "{\"tentativas\":" + usuario.getTentativasFalha() + "}");
                log.warn("Conta bloqueada por excesso de tentativas: {}", usuario.getLogin());
            }
            usuarioRepository.save(usuario);
            auditService.registrarSync(AcaoAudit.LOGIN_FALHA, usuario.getId(), usuario.getLogin(), "auth", null);
            throw new CredencialInvalidaException("Usuário ou senha inválidos.");
        }

        usuario.setTentativasFalha(0);
        usuario.setBloqueadoAte(null);
        usuarioRepository.save(usuario);

        auditService.registrar(AcaoAudit.LOGIN, usuario.getId(), usuario.getLogin(), "auth", null);
        return gerarSessaoParaUsuario(usuario);
    }

    @Transactional
    public void alterarSenha(String email, String senhaAtual, String novaSenha) {
        UsuarioEntity usuario = carregarUsuarioAtivoPorEmail(email);

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }

        politicaSenhaService.validar(novaSenha);
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuario.setSenhaAlteradaEm(Instant.now());
        usuario.setExigeTrocaSenha(false);
        usuarioRepository.save(usuario);
        refreshTokenService.revogarTodosDoUsuario(Objects.requireNonNull(usuario.getId(), "usuario.id é obrigatório."));

        auditService.registrar(AcaoAudit.SENHA_ALTERADA, usuario.getId(), usuario.getLogin(), "auth", null);
    }

    @Transactional(readOnly = true)
    public SessaoUsuarioDTO buscarSessaoAtual(String email) {
        return mapearSessao(carregarUsuarioAtivoPorEmail(email));
    }

    @Transactional(readOnly = true)
    public List<SimpleGrantedAuthority> authoritiesFor(String email) {
        UsuarioEntity usuario = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
        if (usuario == null || !usuario.isAtivo()) {
            return List.of();
        }

        Map<String, Boolean> permissoes = permissaoResolver.permissoesEfetivas(usuario);
        Long usuarioId = Objects.requireNonNull(usuario.getId(), "usuario.id é obrigatório.");
        String papel = permissaoResolver.papel(usuarioId);
        boolean isAdmin = permissaoResolver.ehAdmin(usuarioId);

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(isAdmin ? "ROLE_ADMIN" : "ROLE_USER"));
        authorities.add(new SimpleGrantedAuthority("ROLE_" + papel.toUpperCase()));

        permissoes.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(entry -> PermissaoCatalogo.authorityForKey(entry.getKey()))
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return authorities;
    }

    @Transactional(readOnly = true)
    public UsuarioEntity carregarUsuarioAtivoPorEmail(String email) {
        UsuarioEntity usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));

        if (!usuario.isAtivo()) {
            throw new IllegalArgumentException("Usuário autenticado não encontrado.");
        }

        return usuario;
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO gerarSessaoParaUsuario(UsuarioEntity usuario) {
        return new LoginResponseDTO(
                mapearSessao(usuario),
                gerenciadorToken.gerarToken(usuario.getEmail()),
                usuario.isExigeTrocaSenha()
        );
    }

    private SessaoUsuarioDTO mapearSessao(UsuarioEntity usuario) {
        Long usuarioId = Objects.requireNonNull(usuario.getId(), "usuario.id é obrigatório.");
        Map<String, Boolean> permissoes = permissaoResolver.permissoesEfetivas(usuario);
        String papel = permissaoResolver.papel(usuarioId);
        List<String> filiaisPermitidas = usuario.getSetor().getFiliaisPermitidas().stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .sorted(Comparator.comparing(String::toLowerCase))
                .toList();

        return new SessaoUsuarioDTO(
                String.valueOf(usuario.getId()),
                usuario.getNome(),
                usuario.getEmail(),
                papel,
                new SetorSessaoDTO(String.valueOf(usuario.getSetor().getId()), usuario.getSetor().getNome()),
                permissoes,
                permissaoResolver.ehAdminPlataforma(usuarioId) ? List.of() : filiaisPermitidas,
                usuario.isExigeTrocaSenha()
        );
    }
}
