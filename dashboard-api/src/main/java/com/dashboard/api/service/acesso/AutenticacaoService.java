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
import java.util.List;
import java.util.Map;

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

    public AutenticacaoService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            GerenciadorTokenJwt gerenciadorToken,
            PermissaoResolverService permissaoResolver,
            AuditService auditService,
            PoliticaSenhaService politicaSenhaService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.gerenciadorToken = gerenciadorToken;
        this.permissaoResolver = permissaoResolver;
        this.auditService = auditService;
        this.politicaSenhaService = politicaSenhaService;
    }

    @Transactional
    public LoginResponseDTO autenticar(String loginOuEmail, String senha) {
        UsuarioEntity usuario = usuarioRepository.findByLoginOrEmail(loginOuEmail.trim()).orElse(null);

        if (usuario == null || !usuario.isAtivo()) {
            auditService.registrarSync(AcaoAudit.LOGIN_FALHA, null, loginOuEmail, "auth", null);
            throw new IllegalArgumentException("Usuário ou senha inválidos.");
        }

        // Lockout check
        if (usuario.getBloqueadoAte() != null && Instant.now().isBefore(usuario.getBloqueadoAte())) {
            auditService.registrarSync(AcaoAudit.LOGIN_FALHA, usuario.getId(), usuario.getLogin(), "auth", "{\"motivo\":\"conta_bloqueada\"}");
            throw new IllegalArgumentException("Conta temporariamente bloqueada. Tente novamente mais tarde.");
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
            throw new IllegalArgumentException("Usuário ou senha inválidos.");
        }

        // Login bem-sucedido — reset lockout
        usuario.setTentativasFalha(0);
        usuario.setBloqueadoAte(null);
        usuarioRepository.save(usuario);

        String token = gerenciadorToken.gerarToken(usuario.getLogin());
        auditService.registrar(AcaoAudit.LOGIN, usuario.getId(), usuario.getLogin(), "auth", null);

        SessaoUsuarioDTO sessao = mapearSessao(usuario);
        return new LoginResponseDTO(sessao, token, usuario.isExigeTrocaSenha());
    }

    @Transactional
    public void alterarSenha(String login, String senhaAtual, String novaSenha) {
        UsuarioEntity usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }

        politicaSenhaService.validar(novaSenha);
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuario.setSenhaAlteradaEm(Instant.now());
        usuario.setExigeTrocaSenha(false);
        usuarioRepository.save(usuario);

        auditService.registrar(AcaoAudit.SENHA_ALTERADA, usuario.getId(), usuario.getLogin(), "auth", null);
    }

    @Transactional(readOnly = true)
    public SessaoUsuarioDTO buscarSessaoAtual(String login) {
        UsuarioEntity usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));

        if (!usuario.isAtivo()) {
            throw new IllegalArgumentException("Usuário autenticado não encontrado.");
        }

        return mapearSessao(usuario);
    }

    @Transactional(readOnly = true)
    public List<SimpleGrantedAuthority> authoritiesFor(String login) {
        UsuarioEntity usuario = usuarioRepository.findByLogin(login).orElse(null);
        if (usuario == null || !usuario.isAtivo()) {
            return List.of();
        }

        Map<String, Boolean> permissoes = permissaoResolver.permissoesEfetivas(usuario);
        boolean isAdmin = permissaoResolver.ehAdmin(usuario.getId());

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(isAdmin ? "ROLE_ADMIN" : "ROLE_USER"));

        // Add role-specific authorities
        for (String papel : permissaoResolver.papeis(usuario.getId())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + papel.toUpperCase()));
        }

        permissoes.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(entry -> PermissaoCatalogo.authorityForKey(entry.getKey()))
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return authorities;
    }

    private SessaoUsuarioDTO mapearSessao(UsuarioEntity usuario) {
        Map<String, Boolean> permissoes = permissaoResolver.permissoesEfetivas(usuario);
        boolean isAdmin = permissaoResolver.ehAdminPlataforma(usuario.getId());
        List<String> papeis = permissaoResolver.papeis(usuario.getId());

        SetorSessaoDTO setor = new SetorSessaoDTO(
                String.valueOf(usuario.getSetor().getId()),
                usuario.getSetor().getNome(),
                permissoes
        );

        return new SessaoUsuarioDTO(
                String.valueOf(usuario.getId()),
                usuario.getLogin(),
                usuario.getNome(),
                usuario.getEmail(),
                isAdmin,
                setor,
                papeis,
                usuario.isExigeTrocaSenha()
        );
    }
}
