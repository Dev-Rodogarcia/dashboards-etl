package com.dashboard.api.service.acesso;

import com.dashboard.api.dto.acesso.PapelDTO;
import com.dashboard.api.dto.acesso.UsuarioAcessoDTO;
import com.dashboard.api.dto.acesso.UsuarioRequestDTO;
import com.dashboard.api.model.acesso.PapelEntity;
import com.dashboard.api.model.acesso.PermissaoEntity;
import com.dashboard.api.model.acesso.SetorEntity;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.model.acesso.UsuarioPapelVinculo;
import com.dashboard.api.model.acesso.UsuarioPermissaoOverride;
import com.dashboard.api.repository.acesso.PapelRepository;
import com.dashboard.api.repository.acesso.PermissaoRepository;
import com.dashboard.api.repository.acesso.SetorRepository;
import com.dashboard.api.repository.acesso.UsuarioPapelVinculoRepository;
import com.dashboard.api.repository.acesso.UsuarioPermissaoOverrideRepository;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class GestaoUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SetorRepository setorRepository;
    private final PapelRepository papelRepository;
    private final UsuarioPapelVinculoRepository papelVinculoRepository;
    private final UsuarioPermissaoOverrideRepository overrideRepository;
    private final PermissaoRepository permissaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissaoResolverService permissaoResolver;
    private final AuditService auditService;
    private final PoliticaSenhaService politicaSenhaService;
    private final RefreshTokenService refreshTokenService;

    public GestaoUsuarioService(
            UsuarioRepository usuarioRepository,
            SetorRepository setorRepository,
            PapelRepository papelRepository,
            UsuarioPapelVinculoRepository papelVinculoRepository,
            UsuarioPermissaoOverrideRepository overrideRepository,
            PermissaoRepository permissaoRepository,
            PasswordEncoder passwordEncoder,
            PermissaoResolverService permissaoResolver,
            AuditService auditService,
            PoliticaSenhaService politicaSenhaService,
            RefreshTokenService refreshTokenService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.setorRepository = setorRepository;
        this.papelRepository = papelRepository;
        this.papelVinculoRepository = papelVinculoRepository;
        this.overrideRepository = overrideRepository;
        this.permissaoRepository = permissaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.permissaoResolver = permissaoResolver;
        this.auditService = auditService;
        this.politicaSenhaService = politicaSenhaService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public List<UsuarioAcessoDTO> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
                .map(this::mapearUsuario)
                .toList();
    }

    @Transactional
    public UsuarioAcessoDTO criarUsuario(UsuarioRequestDTO request) {
        if (request.senha() == null || request.senha().isBlank()) {
            throw new IllegalArgumentException("A senha é obrigatória para novos usuários.");
        }

        validarConfirmacaoSenha(request.senha(), request.confirmacaoSenha());
        politicaSenhaService.validar(request.senha());
        validarEmailUnico(request.email(), null);

        SetorEntity setor = buscarSetor(request.setorId());
        PapelEntity papel = validarGovernancaDePapel(null, request.papel());

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNome(request.nome().trim());
        usuario.setEmail(normalizarEmail(request.email()));
        usuario.setLogin(normalizarEmail(request.email()));
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setExigeTrocaSenha(true);
        usuario.setSetor(setor);
        usuario.setAtivo(request.ativo() == null || request.ativo());
        usuario = usuarioRepository.save(usuario);

        salvarPapelUnico(usuario, papel);
        salvarOverrides(usuario, request.permissoesNegadas(), request.permissoesConcedidas());

        auditService.registrar(AcaoAudit.USUARIO_CRIADO, usuario.getId(), usuario.getLogin(), "usuario", null);
        return mapearUsuario(usuario);
    }

    @Transactional
    public UsuarioAcessoDTO atualizarUsuario(Long usuarioId, UsuarioRequestDTO request) {
        Long usuarioIdNonNull = Objects.requireNonNull(usuarioId, "usuarioId é obrigatório.");

        UsuarioEntity usuario = usuarioRepository.findById(usuarioIdNonNull)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        validarEmailUnico(request.email(), usuarioIdNonNull);

        SetorEntity setor = buscarSetor(request.setorId());
        String papelAtual = permissaoResolver.papel(usuarioIdNonNull);
        PapelEntity novoPapel = validarGovernancaDePapel(usuario, request.papel());
        boolean novoAtivo = request.ativo() == null || request.ativo();

        if (PermissaoResolverService.PAPEL_ADMIN_PLATAFORMA.equals(papelAtual)
                && (!PermissaoResolverService.PAPEL_ADMIN_PLATAFORMA.equals(novoPapel.getNome()) || !novoAtivo)
                && usuarioRepository.countAdminsAtivos() <= 1) {
            throw new IllegalStateException("É obrigatório manter pelo menos um administrador ativo.");
        }

        usuario.setNome(request.nome().trim());
        usuario.setEmail(normalizarEmail(request.email()));
        usuario.setLogin(normalizarEmail(request.email()));
        usuario.setSetor(setor);
        usuario.setAtivo(novoAtivo);

        boolean senhaAlterada = false;
        if (request.senha() != null && !request.senha().isBlank()) {
            validarConfirmacaoSenha(request.senha(), request.confirmacaoSenha());
            politicaSenhaService.validar(request.senha());
            usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
            usuario.setSenhaAlteradaEm(Instant.now());
            usuario.setExigeTrocaSenha(true);
            senhaAlterada = true;
        }

        usuario = usuarioRepository.save(usuario);

        boolean papelAlterado = !Objects.equals(papelAtual, novoPapel.getNome());
        if (papelAlterado) {
            salvarPapelUnico(usuario, novoPapel);
        }
        salvarOverrides(usuario, request.permissoesNegadas(), request.permissoesConcedidas());

        if (!novoAtivo || senhaAlterada) {
            refreshTokenService.revogarTodosDoUsuario(usuarioIdNonNull);
        }

        auditService.registrar(AcaoAudit.USUARIO_ATUALIZADO, usuario.getId(), usuario.getLogin(), "usuario", null);
        return mapearUsuario(usuario);
    }

    @Transactional
    public void inativarUsuario(Long usuarioId) {
        Long usuarioIdNonNull = Objects.requireNonNull(usuarioId, "usuarioId é obrigatório.");

        UsuarioEntity usuario = usuarioRepository.findById(usuarioIdNonNull)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        validarGovernancaDePapel(usuario, permissaoResolver.papel(usuarioIdNonNull));

        if (PermissaoResolverService.PAPEL_ADMIN_PLATAFORMA.equals(permissaoResolver.papel(usuarioIdNonNull))
                && usuarioRepository.countAdminsAtivos() <= 1) {
            throw new IllegalStateException("É obrigatório manter pelo menos um administrador ativo.");
        }

        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
        refreshTokenService.revogarTodosDoUsuario(usuarioIdNonNull);
        auditService.registrar(AcaoAudit.USUARIO_DESATIVADO, usuarioIdNonNull, usuario.getLogin(), "usuario", null);
    }

    @Transactional(readOnly = true)
    public List<PapelDTO> listarPapeisDisponiveis() {
        UsuarioEntity operador = usuarioAutenticado();
        boolean adminPlataforma = permissaoResolver.ehAdminPlataforma(Objects.requireNonNull(operador.getId(), "usuario.id é obrigatório."));

        return papelRepository.findAll().stream()
                .filter(PapelEntity::isAtivo)
                .filter(papel -> adminPlataforma || PermissaoResolverService.PAPEL_USUARIO_COMUM.equals(papel.getNome()))
                .sorted(Comparator.comparingInt(PapelEntity::getNivel).reversed())
                .map(papel -> new PapelDTO(papel.getId(), papel.getNome(), papel.getDescricao(), papel.getNivel()))
                .toList();
    }

    private UsuarioAcessoDTO mapearUsuario(UsuarioEntity usuario) {
        Long usuarioId = Objects.requireNonNull(usuario.getId(), "usuario.id é obrigatório.");
        Map<String, Boolean> permissoesEfetivas = permissaoResolver.permissoesEfetivas(usuario);
        String papel = permissaoResolver.papel(usuarioId);
        List<UsuarioPermissaoOverride> todosOverrides = overrideRepository.findAllByUsuarioId(usuarioId);
        List<String> permissoesNegadas = todosOverrides.stream()
                .filter(override -> "DENY".equals(override.getTipo()))
                .map(override -> override.getPermissao().getChaveLegado() != null
                        ? override.getPermissao().getChaveLegado()
                        : override.getPermissao().getChave())
                .sorted()
                .toList();
        List<String> permissoesConcedidas = todosOverrides.stream()
                .filter(override -> "GRANT".equals(override.getTipo()))
                .map(override -> override.getPermissao().getChaveLegado() != null
                        ? override.getPermissao().getChaveLegado()
                        : override.getPermissao().getChave())
                .sorted()
                .toList();
        List<String> filiaisPermitidas = usuario.getSetor().getFiliaisPermitidas().stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return new UsuarioAcessoDTO(
                String.valueOf(usuario.getId()),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.isAtivo(),
                String.valueOf(usuario.getSetor().getId()),
                usuario.getSetor().getNome(),
                papel,
                permissoesEfetivas,
                permissaoResolver.ehAdminPlataforma(usuarioId) ? List.of() : filiaisPermitidas,
                permissoesNegadas,
                permissoesConcedidas
        );
    }

    private SetorEntity buscarSetor(String setorId) {
        try {
            Long id = Objects.requireNonNull(Long.valueOf(setorId), "setorId é obrigatório.");
            return setorRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Setor não encontrado."));
        } catch (NumberFormatException ex) {
            return setorRepository.findByChave(setorId)
                    .orElseThrow(() -> new IllegalArgumentException("Setor não encontrado."));
        }
    }

    private PapelEntity validarGovernancaDePapel(UsuarioEntity alvoExistente, String nomePapelSolicitado) {
        UsuarioEntity operador = usuarioAutenticado();
        Long operadorId = Objects.requireNonNull(operador.getId(), "usuario.id é obrigatório.");
        boolean operadorAdminPlataforma = permissaoResolver.ehAdminPlataforma(operadorId);

        String papelAlvoAtual = alvoExistente != null && alvoExistente.getId() != null
                ? permissaoResolver.papel(alvoExistente.getId())
                : null;

        if (!operadorAdminPlataforma) {
            if (papelAlvoAtual != null && !PermissaoResolverService.PAPEL_USUARIO_COMUM.equals(papelAlvoAtual)) {
                throw new AccessDeniedException("Admin de acesso só pode operar usuários comuns.");
            }
            if (!PermissaoResolverService.PAPEL_USUARIO_COMUM.equals(nomePapelSolicitado)) {
                throw new AccessDeniedException("Admin de acesso só pode atribuir o papel usuario_comum.");
            }
        }

        return papelRepository.findByNome(nomePapelSolicitado)
                .orElseThrow(() -> new IllegalArgumentException("Papel inválido: " + nomePapelSolicitado));
    }

    private void salvarPapelUnico(UsuarioEntity usuario, PapelEntity papel) {
        Long usuarioId = Objects.requireNonNull(usuario.getId(), "usuario.id é obrigatório.");
        papelVinculoRepository.deleteAllByUsuarioId(usuarioId);

        UsuarioPapelVinculo vinculo = new UsuarioPapelVinculo();
        vinculo.setUsuario(usuario);
        vinculo.setPapel(papel);
        papelVinculoRepository.save(vinculo);
    }

    private void salvarOverrides(UsuarioEntity usuario, List<String> permissoesNegadas, List<String> permissoesConcedidas) {
        Long usuarioId = Objects.requireNonNull(usuario.getId(), "usuario.id é obrigatório.");
        List<String> negadas = normalizarPermissoes(permissoesNegadas);
        List<String> concedidas = normalizarPermissoes(permissoesConcedidas);

        Set<String> conflitantes = new LinkedHashSet<>(negadas);
        conflitantes.retainAll(concedidas);
        if (!conflitantes.isEmpty()) {
            throw new IllegalArgumentException("A mesma permissão não pode ser negada e concedida ao mesmo tempo: " + String.join(", ", conflitantes));
        }

        overrideRepository.deleteAllByUsuarioId(usuarioId);
        overrideRepository.flush();

        for (String chave : negadas) {
            PermissaoEntity permissao = permissaoRepository.findByChaveLegado(chave)
                    .orElseThrow(() -> new IllegalArgumentException("Permissão não encontrada: " + chave));
            UsuarioPermissaoOverride override = new UsuarioPermissaoOverride();
            override.setUsuario(usuario);
            override.setPermissao(permissao);
            override.setTipo("DENY");
            overrideRepository.save(override);
        }

        for (String chave : concedidas) {
            PermissaoEntity permissao = permissaoRepository.findByChaveLegado(chave)
                    .orElseThrow(() -> new IllegalArgumentException("Permissão não encontrada: " + chave));
            UsuarioPermissaoOverride override = new UsuarioPermissaoOverride();
            override.setUsuario(usuario);
            override.setPermissao(permissao);
            override.setTipo("GRANT");
            overrideRepository.save(override);
        }
    }

    private List<String> normalizarPermissoes(List<String> permissoes) {
        return permissoes == null ? List.of() : permissoes.stream()
                .filter(chave -> chave != null && !chave.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .toList();
    }

    private void validarEmailUnico(String email, Long excludeId) {
        String emailNormalizado = normalizarEmail(email);
        if (excludeId == null) {
            if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)
                    || usuarioRepository.existsByLoginIgnoreCase(emailNormalizado)) {
                throw new IllegalStateException("Já existe um usuário com este e-mail.");
            }
            return;
        }

        Long excludeIdNonNull = Objects.requireNonNull(excludeId, "excludeId é obrigatório.");
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(emailNormalizado, excludeIdNonNull)
                || usuarioRepository.existsByLoginIgnoreCaseAndIdNot(emailNormalizado, excludeIdNonNull)) {
            throw new IllegalStateException("Já existe um usuário com este e-mail.");
        }
    }

    private void validarConfirmacaoSenha(String senha, String confirmacaoSenha) {
        if (!Objects.equals(senha, confirmacaoSenha)) {
            throw new IllegalArgumentException("A confirmação de senha não confere.");
        }
    }

    private UsuarioEntity usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AccessDeniedException("Usuário autenticado não encontrado.");
        }

        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuário autenticado não encontrado."));
    }

    private String normalizarEmail(String email) {
        return Objects.requireNonNull(email, "email é obrigatório.").trim().toLowerCase();
    }
}
