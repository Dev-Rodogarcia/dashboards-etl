package com.dashboard.api.service.acesso;

import com.dashboard.api.dto.acesso.UsuarioAcessoDTO;
import com.dashboard.api.dto.acesso.UsuarioRequestDTO;
import com.dashboard.api.model.acesso.*;
import com.dashboard.api.repository.acesso.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
            PoliticaSenhaService politicaSenhaService
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
        validarLoginUnico(request.login(), null);
        validarEmailUnico(request.email(), null);

        if (request.senha() == null || request.senha().isBlank()) {
            throw new IllegalArgumentException("A senha é obrigatória para novos usuários.");
        }
        politicaSenhaService.validar(request.senha());

        SetorEntity setor = buscarSetor(request.setorId());

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setLogin(request.login().trim().toLowerCase());
        usuario.setNome(request.nome().trim());
        usuario.setEmail(request.email().trim().toLowerCase());
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setExigeTrocaSenha(true);
        usuario.setSetor(setor);
        usuario.setAtivo(request.ativo() == null || request.ativo());
        usuario = usuarioRepository.save(usuario);

        // Atribuir papel baseado no flag admin
        atribuirPapelInicial(usuario, Boolean.TRUE.equals(request.admin()));

        auditService.registrar(AcaoAudit.USUARIO_CRIADO, usuario.getId(), usuario.getLogin(), "usuario", null);
        return mapearUsuario(usuario);
    }

    @Transactional
    public UsuarioAcessoDTO atualizarUsuario(Long usuarioId, UsuarioRequestDTO request) {
        Long usuarioIdNonNull = Objects.requireNonNull(usuarioId, "usuarioId é obrigatório.");

        UsuarioEntity usuario = usuarioRepository.findById(usuarioIdNonNull)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        validarLoginUnico(request.login(), usuarioIdNonNull);
        validarEmailUnico(request.email(), usuarioIdNonNull);

        SetorEntity setor = buscarSetor(request.setorId());

        boolean novoAdmin = Boolean.TRUE.equals(request.admin());
        boolean novoAtivo = request.ativo() == null || request.ativo();
        boolean eraAdmin = permissaoResolver.ehAdminPlataforma(usuario.getId());

        // Proteger ultimo admin
        if (eraAdmin && (!novoAdmin || !novoAtivo)) {
            if (usuarioRepository.countAdminsAtivos() <= 1) {
                throw new IllegalStateException("É obrigatório manter pelo menos um administrador ativo.");
            }
        }

        usuario.setLogin(request.login().trim().toLowerCase());
        usuario.setNome(request.nome().trim());
        usuario.setEmail(request.email().trim().toLowerCase());
        usuario.setSetor(setor);
        usuario.setAtivo(novoAtivo);

        if (request.senha() != null && !request.senha().isBlank()) {
            politicaSenhaService.validar(request.senha());
            usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
            usuario.setSenhaAlteradaEm(Instant.now());
            usuario.setExigeTrocaSenha(true);
        }

        usuario = usuarioRepository.save(usuario);

        // Atualizar papel se mudou o flag admin
        if (novoAdmin != eraAdmin) {
            papelVinculoRepository.deleteAllByUsuarioId(usuarioIdNonNull);
            atribuirPapelInicial(usuario, novoAdmin);
        }

        auditService.registrar(AcaoAudit.USUARIO_ATUALIZADO, usuario.getId(), usuario.getLogin(), "usuario", null);
        return mapearUsuario(usuario);
    }

    @Transactional
    public void excluirUsuario(Long usuarioId) {
        Long usuarioIdNonNull = Objects.requireNonNull(usuarioId, "usuarioId é obrigatório.");

        UsuarioEntity usuario = usuarioRepository.findById(usuarioIdNonNull)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        boolean eraAdmin = permissaoResolver.ehAdminPlataforma(usuario.getId());
        if (eraAdmin && usuarioRepository.countAdminsAtivos() <= 1) {
            throw new IllegalStateException("É obrigatório manter pelo menos um administrador ativo.");
        }

        overrideRepository.deleteAllByUsuarioId(usuarioIdNonNull);
        papelVinculoRepository.deleteAllByUsuarioId(usuarioIdNonNull);
        usuarioRepository.delete(usuario);
        auditService.registrar(AcaoAudit.USUARIO_EXCLUIDO, usuarioIdNonNull, usuario.getLogin(), "usuario", null);
    }

    @Transactional
    public void atribuirPapeis(Long usuarioId, List<Long> papelIds) {
        Long usuarioIdNonNull = Objects.requireNonNull(usuarioId, "usuarioId é obrigatório.");

        UsuarioEntity usuario = usuarioRepository.findById(usuarioIdNonNull)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        papelVinculoRepository.deleteAllByUsuarioId(usuarioIdNonNull);

        for (Long papelId : papelIds) {
            Long papelIdNonNull = Objects.requireNonNull(papelId, "papelId é obrigatório.");
            PapelEntity papel = papelRepository.findById(papelIdNonNull)
                    .orElseThrow(() -> new IllegalArgumentException("Papel não encontrado: " + papelId));
            UsuarioPapelVinculo vinculo = new UsuarioPapelVinculo();
            vinculo.setUsuario(usuario);
            vinculo.setPapel(papel);
            papelVinculoRepository.save(vinculo);
        }

        auditService.registrar(AcaoAudit.PAPEL_CONCEDIDO, usuarioIdNonNull, usuario.getLogin(), "papel",
                "{\"papelIds\":" + papelIds + "}");
    }

    @Transactional(readOnly = true)
    public List<OverrideDTO> buscarOverrides(Long usuarioId) {
        return overrideRepository.findAllByUsuarioId(usuarioId).stream()
                .map(o -> new OverrideDTO(
                        o.getPermissao().getChaveLegado() != null ? o.getPermissao().getChaveLegado() : o.getPermissao().getChave(),
                        o.getTipo()
                ))
                .toList();
    }

    @Transactional
    public void salvarOverrides(Long usuarioId, List<OverrideDTO> overrides) {
        Long usuarioIdNonNull = Objects.requireNonNull(usuarioId, "usuarioId é obrigatório.");

        UsuarioEntity usuario = usuarioRepository.findById(usuarioIdNonNull)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        overrideRepository.deleteAllByUsuarioId(usuarioIdNonNull);

        for (OverrideDTO dto : overrides) {
            PermissaoEntity perm = permissaoRepository.findByChaveLegado(dto.permissaoChave())
                    .orElseThrow(() -> new IllegalArgumentException("Permissão não encontrada: " + dto.permissaoChave()));

            UsuarioPermissaoOverride override = new UsuarioPermissaoOverride();
            override.setUsuario(usuario);
            override.setPermissao(perm);
            override.setTipo(dto.tipo());
            overrideRepository.save(override);
        }

        auditService.registrar(AcaoAudit.PERMISSAO_OVERRIDE_ALTERADA, usuarioIdNonNull, usuario.getLogin(), "override", null);
    }

    public record OverrideDTO(String permissaoChave, String tipo) {}

    private void atribuirPapelInicial(UsuarioEntity usuario, boolean admin) {
        String nomePapel = admin ? "admin_plataforma" : "usuario_comum";
        papelRepository.findByNome(nomePapel).ifPresent(papel -> {
            UsuarioPapelVinculo vinculo = new UsuarioPapelVinculo();
            vinculo.setUsuario(usuario);
            vinculo.setPapel(papel);
            papelVinculoRepository.save(vinculo);
        });
    }

    private UsuarioAcessoDTO mapearUsuario(UsuarioEntity usuario) {
        Long usuarioId = Objects.requireNonNull(usuario.getId(), "usuario.id é obrigatório.");
        Map<String, Boolean> permissoes = permissaoResolver.permissoesEfetivas(usuario);
        boolean isAdmin = permissaoResolver.ehAdminPlataforma(usuarioId);
        List<String> papeis = permissaoResolver.papeis(usuarioId);

        return new UsuarioAcessoDTO(
                String.valueOf(usuario.getId()),
                usuario.getLogin(),
                usuario.getNome(),
                usuario.getEmail(),
                isAdmin,
                usuario.isAtivo(),
                String.valueOf(usuario.getSetor().getId()),
                usuario.getSetor().getNome(),
                permissoes,
                papeis
        );
    }

    private SetorEntity buscarSetor(String setorId) {
        try {
            Long id = Objects.requireNonNull(Long.valueOf(setorId), "setorId é obrigatório.");
            return setorRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Setor não encontrado."));
        } catch (NumberFormatException e) {
            // Tentar por chave legado
            return setorRepository.findByChave(setorId)
                    .orElseThrow(() -> new IllegalArgumentException("Setor não encontrado."));
        }
    }

    private void validarLoginUnico(String login, Long excludeId) {
        if (excludeId == null) {
            if (usuarioRepository.existsByLoginIgnoreCase(login.trim())) {
                throw new IllegalStateException("Já existe um usuário com este login.");
            }
        } else {
            Long excludeIdNonNull = Objects.requireNonNull(excludeId, "excludeId é obrigatório.");
            if (usuarioRepository.existsByLoginIgnoreCaseAndIdNot(login.trim(), excludeIdNonNull)) {
                throw new IllegalStateException("Já existe um usuário com este login.");
            }
        }
    }

    private void validarEmailUnico(String email, Long excludeId) {
        if (excludeId == null) {
            if (usuarioRepository.existsByEmailIgnoreCase(email.trim())) {
                throw new IllegalStateException("Já existe um usuário com este e-mail.");
            }
        } else {
            Long excludeIdNonNull = Objects.requireNonNull(excludeId, "excludeId é obrigatório.");
            if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email.trim(), excludeIdNonNull)) {
                throw new IllegalStateException("Já existe um usuário com este e-mail.");
            }
        }
    }
}
