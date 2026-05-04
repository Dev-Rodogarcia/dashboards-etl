package com.dashboard.api.service.acesso;

import com.dashboard.api.dto.acesso.SetorDTO;
import com.dashboard.api.dto.acesso.SetorRequestDTO;
import com.dashboard.api.model.acesso.PermissaoEntity;
import com.dashboard.api.model.acesso.SetorEntity;
import com.dashboard.api.model.acesso.SetorPermissaoTemplate;
import com.dashboard.api.repository.acesso.PermissaoRepository;
import com.dashboard.api.repository.acesso.SetorPermissaoTemplateRepository;
import com.dashboard.api.repository.acesso.SetorRepository;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import com.dashboard.api.security.PermissaoCatalogo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GestaoSetorService {

    private final SetorRepository setorRepository;
    private final PermissaoRepository permissaoRepository;
    private final SetorPermissaoTemplateRepository templateRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;

    public GestaoSetorService(
            SetorRepository setorRepository,
            PermissaoRepository permissaoRepository,
            SetorPermissaoTemplateRepository templateRepository,
            UsuarioRepository usuarioRepository,
            AuditService auditService
    ) {
        this.setorRepository = setorRepository;
        this.permissaoRepository = permissaoRepository;
        this.templateRepository = templateRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SetorDTO> listarSetores() {
        List<SetorEntity> setores = setorRepository.findAll();
        return setores.stream()
                .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
                .map(this::mapearSetor)
                .toList();
    }

    @Transactional
    public SetorDTO criarSetor(SetorRequestDTO request) {
        if (setorRepository.existsByNomeIgnoreCase(request.nome().trim())) {
            throw new IllegalStateException("Já existe um setor com este nome.");
        }

        SetorEntity setor = new SetorEntity();
        setor.setChave("setor-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        setor.setNome(request.nome().trim());
        setor.setDescricao(limpar(request.descricao()));
        setor.setSistema(false);
        setor.setFiliaisPermitidas(normalizarFiliaisPermitidas(request.filiaisPermitidas()));
        setor = setorRepository.save(setor);

        salvarTemplates(setor, request.permissoes());
        auditService.registrar(AcaoAudit.SETOR_CRIADO, null, null, "setor:" + setor.getChave(), null);

        return mapearSetor(setor);
    }

    @Transactional
    public SetorDTO atualizarSetor(Long setorId, SetorRequestDTO request) {
        Long setorIdNonNull = Objects.requireNonNull(setorId, "setorId é obrigatório.");

        SetorEntity setor = setorRepository.findById(setorIdNonNull)
                .orElseThrow(() -> new IllegalArgumentException("Setor não encontrado."));

        if (setorRepository.existsByNomeIgnoreCaseAndIdNot(request.nome().trim(), setorIdNonNull)) {
            throw new IllegalStateException("Já existe um setor com este nome.");
        }

        setor.setNome(request.nome().trim());
        setor.setDescricao(limpar(request.descricao()));
        setor.setFiliaisPermitidas(normalizarFiliaisPermitidas(request.filiaisPermitidas()));
        setor = setorRepository.save(setor);

        templateRepository.deleteAllBySetorId(setorIdNonNull);
        salvarTemplates(setor, request.permissoes());
        auditService.registrar(AcaoAudit.SETOR_ATUALIZADO, null, null, "setor:" + setor.getChave(), null);

        return mapearSetor(setor);
    }

    @Transactional
    public void excluirSetor(Long setorId) {
        Long setorIdNonNull = Objects.requireNonNull(setorId, "setorId é obrigatório.");

        SetorEntity setor = setorRepository.findById(setorIdNonNull)
                .orElseThrow(() -> new IllegalArgumentException("Setor não encontrado."));

        if (setor.isSistema()) {
            throw new IllegalStateException("Setores do sistema não podem ser excluídos.");
        }

        if (usuarioRepository.countBySetorId(setorIdNonNull) > 0) {
            throw new IllegalStateException("Não é possível excluir um setor que possui usuários vinculados.");
        }

        templateRepository.deleteAllBySetorId(setorIdNonNull);
        setorRepository.delete(setor);
        auditService.registrar(AcaoAudit.SETOR_EXCLUIDO, null, null, "setor:" + setor.getChave(), null);
    }

    private SetorDTO mapearSetor(SetorEntity setor) {
        Long setorId = Objects.requireNonNull(setor.getId(), "setor.id é obrigatório.");
        int totalUsuarios = (int) usuarioRepository.countBySetorId(setorId);
        Map<String, Boolean> permissoes = obterPermissoesTemplate(setorId);

        return new SetorDTO(
                String.valueOf(setor.getId()),
                setor.getNome(),
                setor.getDescricao(),
                setor.isSistema(),
                totalUsuarios,
                permissoes,
                listarFiliaisPermitidas(setor)
        );
    }

    private Map<String, Boolean> obterPermissoesTemplate(Long setorId) {
        Set<Long> templatePermIds = templateRepository.findAllBySetorId(setorId)
                .stream()
                .map(t -> t.getPermissao().getId())
                .collect(Collectors.toSet());

        List<PermissaoEntity> catalogo = permissaoRepository.findAllByAtivoTrue();
        Map<String, Boolean> mapa = new LinkedHashMap<>(PermissaoCatalogo.mapaVazio());
        for (PermissaoEntity perm : catalogo) {
            String chave = perm.getChaveLegado() != null ? perm.getChaveLegado() : perm.getChave();
            mapa.put(chave, templatePermIds.contains(perm.getId()));
        }
        return mapa;
    }

    private void salvarTemplates(SetorEntity setor, Map<String, Boolean> permissoes) {
        if (permissoes == null) return;

        for (Map.Entry<String, Boolean> entry : permissoes.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                permissaoRepository.findByChaveLegado(entry.getKey()).ifPresent(perm -> {
                    templateRepository.save(new SetorPermissaoTemplate(setor, perm));
                });
            }
        }
    }

    private String limpar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }

    private List<String> listarFiliaisPermitidas(SetorEntity setor) {
        return setor.getFiliaisPermitidas().stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(String::trim)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private Set<String> normalizarFiliaisPermitidas(List<String> filiaisPermitidas) {
        if (filiaisPermitidas == null) {
            return new LinkedHashSet<>();
        }

        Set<String> normalizadas = filiaisPermitidas.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(String::trim)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (normalizadas.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos uma filial permitida para o setor.");
        }

        return normalizadas;
    }
}
