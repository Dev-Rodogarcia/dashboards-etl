package com.dashboard.api.controller;

import com.dashboard.api.dto.manifestos.ManifestosCostGoalConfigDTO;
import com.dashboard.api.dto.manifestos.ManifestosCostGoalConfigRequestDTO;
import com.dashboard.api.model.acesso.ManifestosCostGoalEntity;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.ManifestosCostGoalRepository;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/painel/manifestos/metas")
@PreAuthorize("@acessoSeguranca.podeAcessar('manifestos')")
public class ManifestosCostGoalController {

    private static final String GLOBAL_BRANCH_ID = "GLOBAL";
    private static final String DEFAULT_CONTRACT_TYPE = "Geral";
    private static final String DEFAULT_CONTRACT_TYPE_KEY = "geral";

    private final ManifestosCostGoalRepository repository;
    private final UsuarioRepository usuarioRepository;

    public ManifestosCostGoalController(ManifestosCostGoalRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ManifestosCostGoalConfigDTO>> listar(
            @RequestParam int ano,
            @RequestParam int mes
    ) {
        LocalDate competencia = competencia(ano, mes);
        List<ManifestosCostGoalConfigDTO> metas = repository.findAllByYearMonthOrdered(competencia).stream()
                .map(ManifestosCostGoalConfigDTO::from)
                .toList();

        if (metas.isEmpty()) {
            return ResponseEntity.ok(List.of(ManifestosCostGoalConfigDTO.fallback(
                    ano,
                    mes,
                    "Orçamento de custo operacional não cadastrado"
            )));
        }
        return ResponseEntity.ok(metas);
    }

    @PostMapping
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    @Transactional
    public ResponseEntity<ManifestosCostGoalConfigDTO> salvar(
            @RequestBody ManifestosCostGoalConfigRequestDTO request,
            Authentication authentication
    ) {
        Objects.requireNonNull(request, "Dados da meta são obrigatórios.");
        LocalDate competencia = competencia(request.ano(), request.mes());
        String branchId = normalizarBranchId(request.branchId());
        ContractType contrato = normalizarContrato(request.contractType(), request.contractTypeKey());
        BigDecimal costGoal = normalizarMeta(request.costGoal());
        UsuarioEntity usuario = usuarioAutenticado(authentication);

        ManifestosCostGoalEntity entity = buscarExistente(branchId, competencia, contrato.key())
                .orElseGet(ManifestosCostGoalEntity::new);
        entity.setBranchId(branchId);
        entity.setYearMonth(competencia);
        entity.setContractType(contrato.label());
        entity.setContractTypeKey(contrato.key());
        entity.setCostGoal(costGoal);
        entity.setUpdatedByUser(usuario);

        return ResponseEntity.ok(ManifestosCostGoalConfigDTO.from(repository.saveAndFlush(entity)));
    }

    @DeleteMapping
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    @Transactional
    public ResponseEntity<Void> remover(
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) String contractTypeKey,
            @RequestParam int ano,
            @RequestParam int mes
    ) {
        LocalDate competencia = competencia(ano, mes);
        String normalizedBranchId = normalizarBranchId(branchId);
        ContractType contrato = normalizarContrato(null, contractTypeKey);
        buscarExistente(normalizedBranchId, competencia, contrato.key()).ifPresent(repository::delete);
        return ResponseEntity.noContent().build();
    }

    private java.util.Optional<ManifestosCostGoalEntity> buscarExistente(
            String branchId,
            LocalDate competencia,
            String contractTypeKey
    ) {
        if (branchId == null) {
            return repository.findGlobalByYearMonthAndContractTypeKey(competencia, contractTypeKey);
        }
        return repository.findByBranchIdAndYearMonthAndContractTypeKey(branchId, competencia, contractTypeKey);
    }

    private LocalDate competencia(int ano, int mes) {
        if (ano < 2000 || ano > 2100) {
            throw new IllegalArgumentException("Ano da meta deve estar entre 2000 e 2100.");
        }
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mês da meta deve estar entre 1 e 12.");
        }
        return LocalDate.of(ano, mes, 1);
    }

    private String normalizarBranchId(String branchId) {
        String normalized = branchId == null || branchId.isBlank() ? GLOBAL_BRANCH_ID : branchId.trim();
        if (GLOBAL_BRANCH_ID.equalsIgnoreCase(normalized)) {
            return null;
        }
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("Filial da meta excede 120 caracteres.");
        }
        return normalized;
    }

    private ContractType normalizarContrato(String contractType, String contractTypeKey) {
        String key = contractTypeKey == null || contractTypeKey.isBlank()
                ? normalizarContractTypeKey(contractType)
                : contractTypeKey.trim().toLowerCase(Locale.ROOT);
        if (key.isBlank()) {
            key = DEFAULT_CONTRACT_TYPE_KEY;
        }
        String label = contractType == null || contractType.isBlank()
                ? labelContrato(key)
                : contractType.trim();
        if (label.length() > 100) {
            throw new IllegalArgumentException("Tipo de contrato da meta excede 100 caracteres.");
        }
        if (key.length() > 100) {
            throw new IllegalArgumentException("Chave do tipo de contrato da meta excede 100 caracteres.");
        }
        return new ContractType(label, key);
    }

    private String normalizarContractTypeKey(String contractType) {
        if (contractType == null || contractType.isBlank()) {
            return DEFAULT_CONTRACT_TYPE_KEY;
        }
        return contractType.trim().toLowerCase(Locale.ROOT);
    }

    private String labelContrato(String contractTypeKey) {
        return switch (contractTypeKey) {
            case "frota" -> "Frota";
            case "agregado" -> "Agregado";
            case "terceiro" -> "Terceiro";
            case "frota + px" -> "Frota + PX";
            default -> DEFAULT_CONTRACT_TYPE_KEY.equals(contractTypeKey) ? DEFAULT_CONTRACT_TYPE : contractTypeKey;
        };
    }

    private BigDecimal normalizarMeta(BigDecimal costGoal) {
        BigDecimal normalized = Objects.requireNonNullElse(costGoal, BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Meta mensal de custo não pode ser negativa.");
        }
        return normalized;
    }

    private UsuarioEntity usuarioAutenticado(Authentication authentication) {
        String email = authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "";
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
    }

    private record ContractType(String label, String key) {
    }
}
