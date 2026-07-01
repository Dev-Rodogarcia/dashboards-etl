package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.manifestos.ManifestosCostGoalConfigDTO;
import com.dashboard.api.dto.manifestos.ManifestosCostGoalConfigRequestDTO;
import com.dashboard.api.dto.manifestos.ManifestosCustosEvolucaoDTO;
import com.dashboard.api.dto.manifestos.ManifestosCustosEvolucaoDTO.CustoDiarioDTO;
import com.dashboard.api.model.acesso.ManifestosCostGoalEntity;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.ManifestosCostDataRepository;
import com.dashboard.api.repository.acesso.ManifestosCostGoalRepository;
import com.dashboard.api.repository.acesso.ManifestosCostGoalRepository.GoalAggregateProjection;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.service.acesso.EscopoFilialService.EscopoFilial;
import com.dashboard.api.util.ConsultaFiltroUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManifestosCostGoalService {

    private static final Logger log = LoggerFactory.getLogger(ManifestosCostGoalService.class);
    private static final ZoneId ZONE_ID_BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final String GLOBAL_BRANCH_ID = "GLOBAL";
    private static final String DEFAULT_CONTRACT_TYPE = "Geral";
    private static final String DEFAULT_CONTRACT_TYPE_KEY = "geral";
    private static final List<String> FILTROS_SEM_DIMENSAO_ORCAMENTARIA = List.of(
            "status",
            "motoristas",
            "veiculos",
            "numeroManifesto",
            "tiposCarga",
            "tipoMotorista"
    );

    private final ManifestosCostGoalRepository goalRepository;
    private final ManifestosCostDataRepository performanceRepository;
    private final EscopoFilialService escopoFilialService;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    @Autowired
    public ManifestosCostGoalService(
            ManifestosCostGoalRepository goalRepository,
            ManifestosCostDataRepository performanceRepository,
            EscopoFilialService escopoFilialService,
            UsuarioRepository usuarioRepository
    ) {
        this(
                goalRepository,
                performanceRepository,
                escopoFilialService,
                usuarioRepository,
                Clock.system(ZONE_ID_BRASILIA)
        );
    }

    ManifestosCostGoalService(
            ManifestosCostGoalRepository goalRepository,
            ManifestosCostDataRepository performanceRepository,
            EscopoFilialService escopoFilialService,
            Clock clock
    ) {
        this(goalRepository, performanceRepository, escopoFilialService, null, clock);
    }

    ManifestosCostGoalService(
            ManifestosCostGoalRepository goalRepository,
            ManifestosCostDataRepository performanceRepository,
            EscopoFilialService escopoFilialService,
            UsuarioRepository usuarioRepository,
            Clock clock
    ) {
        this.goalRepository = goalRepository;
        this.performanceRepository = performanceRepository;
        this.escopoFilialService = escopoFilialService;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock != null ? clock : Clock.system(ZONE_ID_BRASILIA);
    }

    @Transactional(readOnly = true)
    public List<ManifestosCostGoalConfigDTO> listar(int ano, int mes) {
        LocalDate competencia = competencia(ano, mes);
        List<ManifestosCostGoalConfigDTO> metas = goalRepository.findAllByYearMonthOrdered(competencia).stream()
                .map(ManifestosCostGoalConfigDTO::from)
                .toList();

        if (metas.isEmpty()) {
            return List.of(ManifestosCostGoalConfigDTO.fallback(
                    ano,
                    mes,
                    "Orçamento de custo operacional não cadastrado"
            ));
        }
        return metas;
    }

    @Transactional
    public ManifestosCostGoalConfigDTO salvar(
            ManifestosCostGoalConfigRequestDTO request,
            String authenticationName
    ) {
        Objects.requireNonNull(request, "Dados da meta são obrigatórios.");
        LocalDate competencia = competencia(request.ano(), request.mes());
        String branchId = normalizarBranchId(request.branchId());
        ContractType contrato = normalizarContrato(request.contractType(), request.contractTypeKey());
        String classificationKey = normalizarClassificationKey(request.classificationKey());
        BigDecimal costGoal = normalizarMeta(request.costGoal());
        UsuarioEntity usuario = usuarioAutenticado(authenticationName);

        ManifestosCostGoalEntity entity = buscarExistente(
                branchId,
                competencia,
                contrato.key(),
                classificationKey
        ).orElseGet(ManifestosCostGoalEntity::new);
        entity.setBranchId(branchId);
        entity.setYearMonth(competencia);
        entity.setContractType(contrato.label());
        entity.setContractTypeKey(contrato.key());
        entity.setClassificationKey(classificationKey);
        entity.setCostGoal(costGoal);
        entity.setUpdatedByUser(usuario);

        return ManifestosCostGoalConfigDTO.from(goalRepository.saveAndFlush(entity));
    }

    @Transactional
    public void remover(
            String branchId,
            String contractTypeKey,
            String classificationKey,
            int ano,
            int mes
    ) {
        LocalDate competencia = competencia(ano, mes);
        String normalizedBranchId = normalizarBranchId(branchId);
        ContractType contrato = normalizarContrato(null, contractTypeKey);
        String normalizedClassificationKey = normalizarClassificationKey(classificationKey);
        buscarExistente(
                normalizedBranchId,
                competencia,
                contrato.key(),
                normalizedClassificationKey
        ).ifPresent(goalRepository::delete);
    }

    @Transactional(readOnly = true)
    public ManifestosCustosEvolucaoDTO calcular(FiltroConsultaDTO filtro, BigDecimal custoRealPeriodo) {
        BigDecimal custoReal = moeda(custoRealPeriodo);
        List<CustoDiarioDTO> serieDiaria = performanceRepository.buscarCustosDiarios(filtro);
        LocalDate inicioCompetencia = filtro.dataInicio().withDayOfMonth(1);
        LocalDate fimCompetenciaExclusivo = filtro.dataFim().withDayOfMonth(1).plusMonths(1);
        LocalDate fimCompetencia = fimCompetenciaExclusivo.minusDays(1);
        LocalDate referenciaFechada = referenciaFechada(filtro.dataFim());
        LocalDate referenciaLimitada = referenciaFechada.isAfter(fimCompetencia)
                ? fimCompetencia
                : referenciaFechada;

        int totalDiasUteis = Math.max(1, contarDiasUteis(inicioCompetencia, fimCompetencia));
        int diasUteisDecorridos = referenciaLimitada.isBefore(inicioCompetencia)
                ? 0
                : contarDiasUteis(inicioCompetencia, referenciaLimitada);
        int diasUteisRestantes = Math.max(0, totalDiasUteis - diasUteisDecorridos);

        BigDecimal custoFechado = buscarCustoFechado(filtro, referenciaFechada);
        BigDecimal custoMedioDiarioReal = dividir(custoFechado, Math.max(1, diasUteisDecorridos));
        BigDecimal tendenciaCusto = custoReal
                .add(custoMedioDiarioReal.multiply(BigDecimal.valueOf(diasUteisRestantes)))
                .setScale(2, RoundingMode.HALF_UP);

        AplicabilidadeOrcamento aplicabilidade = resolverAplicabilidade(filtro);
        if (!aplicabilidade.aplicavel()) {
            return montarRespostaSemOrcamento(
                    false,
                    aplicabilidade.observacao(),
                    totalDiasUteis,
                    diasUteisDecorridos,
                    diasUteisRestantes,
                    custoReal,
                    custoMedioDiarioReal,
                    tendenciaCusto,
                    serieDiaria
            );
        }

        GoalAggregateProjection aggregate = aplicabilidade.usarMetaGlobal()
                ? goalRepository.aggregateGlobalOrBranches(
                        inicioCompetencia,
                        fimCompetenciaExclusivo,
                        aplicabilidade.contractTypeKeys(),
                        aplicabilidade.contractTypeFilterActive()
                )
                : goalRepository.aggregateByBranches(
                        inicioCompetencia,
                        fimCompetenciaExclusivo,
                        aplicabilidade.filiais(),
                        aplicabilidade.contractTypeKeys(),
                        aplicabilidade.contractTypeFilterActive()
        );
        BigDecimal orcamentoCusto = moeda(aggregate == null ? null : aggregate.getCostGoal());
        boolean configurado = aggregate != null && aggregate.getConfiguredGoals() > 0;
        if (!configurado) {
            return montarRespostaSemOrcamento(
                    true,
                    "Orçamento de custo não configurado para as competências e filiais selecionadas.",
                    totalDiasUteis,
                    diasUteisDecorridos,
                    diasUteisRestantes,
                    custoReal,
                    custoMedioDiarioReal,
                    tendenciaCusto,
                    serieDiaria
            );
        }
        BigDecimal limiteDiarioBase = dividir(orcamentoCusto, totalDiasUteis);
        BigDecimal saldoOrcamentario = orcamentoCusto.subtract(custoFechado)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal saldoDisponivelParaLimite = saldoOrcamentario.max(BigDecimal.ZERO);
        BigDecimal limiteDiarioDinamico = dividir(
                saldoDisponivelParaLimite,
                Math.max(1, diasUteisRestantes)
        );
        BigDecimal consumoOrcamento = percentual(custoReal, orcamentoCusto);

        return new ManifestosCustosEvolucaoDTO(
                true,
                true,
                null,
                totalDiasUteis,
                diasUteisDecorridos,
                diasUteisRestantes,
                orcamentoCusto,
                custoReal,
                limiteDiarioBase,
                custoMedioDiarioReal,
                saldoOrcamentario,
                limiteDiarioDinamico,
                tendenciaCusto,
                consumoOrcamento,
                serieDiaria
        );
    }

    private Optional<ManifestosCostGoalEntity> buscarExistente(
            String branchId,
            LocalDate competencia,
            String contractTypeKey,
            String classificationKey
    ) {
        if (branchId == null) {
            return goalRepository.findGlobalByYearMonthAndContractTypeKeyAndClassificationKey(
                    competencia,
                    contractTypeKey,
                    classificationKey
            );
        }
        return goalRepository.findByBranchIdAndYearMonthAndContractTypeKeyAndClassificationKey(
                branchId,
                competencia,
                contractTypeKey,
                classificationKey
        );
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

    private String normalizarClassificationKey(String classificationKey) {
        if (classificationKey == null || classificationKey.isBlank()) {
            return null;
        }
        String normalized = classificationKey.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("Chave da classificação da meta excede 120 caracteres.");
        }
        return normalized;
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

    private UsuarioEntity usuarioAutenticado(String authenticationName) {
        if (usuarioRepository == null) {
            throw new IllegalStateException("Repositório de usuários não configurado para persistir metas.");
        }
        String email = authenticationName == null ? "" : authenticationName;
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
    }

    private ManifestosCustosEvolucaoDTO montarRespostaSemOrcamento(
            boolean aplicavel,
            String observacao,
            int totalDiasUteis,
            int diasUteisDecorridos,
            int diasUteisRestantes,
            BigDecimal custoReal,
            BigDecimal custoMedioDiarioReal,
            BigDecimal tendenciaCusto,
            List<CustoDiarioDTO> serieDiaria
    ) {
        BigDecimal zero = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return new ManifestosCustosEvolucaoDTO(
                aplicavel,
                false,
                observacao,
                totalDiasUteis,
                diasUteisDecorridos,
                diasUteisRestantes,
                zero,
                custoReal,
                zero,
                custoMedioDiarioReal,
                zero,
                zero,
                tendenciaCusto,
                zero,
                serieDiaria
        );
    }

    private BigDecimal buscarCustoFechado(FiltroConsultaDTO filtro, LocalDate referenciaFechada) {
        LocalDate fimFechado = referenciaFechada.isAfter(filtro.dataFim())
                ? filtro.dataFim()
                : referenciaFechada;
        if (fimFechado.isBefore(filtro.dataInicio())) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return performanceRepository.buscarCustoTotal(new FiltroConsultaDTO(
                filtro.dataInicio(),
                fimFechado,
                filtro.filtros()
        ));
    }

    private AplicabilidadeOrcamento resolverAplicabilidade(FiltroConsultaDTO filtro) {
        List<String> filtrosIncompativeis = FILTROS_SEM_DIMENSAO_ORCAMENTARIA.stream()
                .filter(filtro::temFiltro)
                .toList();
        if (!filtrosIncompativeis.isEmpty()) {
            return AplicabilidadeOrcamento.indisponivel(
                    "Orçamento indisponível com filtros sem dimensão orçamentária: "
                            + String.join(", ", filtrosIncompativeis) + "."
            );
        }

        EscopoFilial escopo = escopoFilialService.escopoAtual();
        if (!escopo.acessoTotal() && escopo.filiaisOrdenadas().isEmpty()) {
            return AplicabilidadeOrcamento.indisponivel(
                    "Orçamento indisponível porque o usuário não possui filial autorizada."
            );
        }

        List<String> contractTypeKeys = normalizar(filtro.valores("tiposContrato"));
        List<String> filiaisSelecionadas = normalizar(filtro.valores("filiais"));
        if (escopo.acessoTotal() && filiaisSelecionadas.isEmpty()) {
            return AplicabilidadeOrcamento.global(contractTypeKeys);
        }

        List<String> filiaisPermitidas = escopo.acessoTotal()
                ? filiaisSelecionadas
                : intersecao(filiaisSelecionadas, escopo.filiaisOrdenadas());
        if (!escopo.acessoTotal() && filiaisSelecionadas.isEmpty()) {
            filiaisPermitidas = normalizar(escopo.filiaisOrdenadas());
        }
        if (filiaisPermitidas.isEmpty()) {
            return AplicabilidadeOrcamento.indisponivel(
                    "Orçamento indisponível porque as filiais selecionadas estão fora do escopo autorizado."
            );
        }
        return AplicabilidadeOrcamento.porFiliais(filiaisPermitidas, contractTypeKeys);
    }

    private List<String> intersecao(Collection<String> selecionadas, Collection<String> permitidas) {
        Set<String> permitidasNormalizadas = new LinkedHashSet<>(normalizar(permitidas));
        return normalizar(selecionadas).stream()
                .filter(permitidasNormalizadas::contains)
                .toList();
    }

    private List<String> normalizar(Collection<String> valores) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(valor -> valor.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private LocalDate referenciaFechada(LocalDate dataFimFiltro) {
        LocalDate ultimoDiaUtilFechado = buscarUltimoDiaUtilFechado(LocalDate.now(clock));
        return dataFimFiltro.isAfter(ultimoDiaUtilFechado) ? ultimoDiaUtilFechado : dataFimFiltro;
    }

    private LocalDate buscarUltimoDiaUtilFechado(LocalDate dataReferencia) {
        try {
            LocalDate ultimoDiaUtil = performanceRepository.buscarUltimoDiaUtilFechado(dataReferencia);
            if (ultimoDiaUtil != null) {
                return ultimoDiaUtil;
            }
        } catch (RuntimeException ex) {
            log.warn(
                    "Não foi possível consultar dim_calendario para tendência de custos. "
                            + "Usando fallback por fim de semana. Motivo: {}",
                    ex.getMessage()
            );
        }
        return ultimoDiaUtilAnterior(dataReferencia);
    }

    private int contarDiasUteis(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            return 0;
        }
        try {
            Integer total = performanceRepository.contarDiasUteisCalendario(inicio, fim);
            if (total != null) {
                return total;
            }
        } catch (RuntimeException ex) {
            log.warn(
                    "Não foi possível consultar dias úteis na dim_calendario para custos. "
                            + "Usando fallback por fim de semana. Motivo: {}",
                    ex.getMessage()
            );
        }
        return contarDiasUteisPorFimDeSemana(inicio, fim);
    }

    private LocalDate ultimoDiaUtilAnterior(LocalDate data) {
        LocalDate candidato = data.minusDays(1);
        while (!isDiaUtil(candidato)) {
            candidato = candidato.minusDays(1);
        }
        return candidato;
    }

    private int contarDiasUteisPorFimDeSemana(LocalDate inicio, LocalDate fim) {
        int total = 0;
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            if (isDiaUtil(data)) {
                total++;
            }
        }
        return total;
    }

    private boolean isDiaUtil(LocalDate data) {
        DayOfWeek dia = data.getDayOfWeek();
        return dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY;
    }

    private BigDecimal dividir(BigDecimal valor, int divisor) {
        if (divisor <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return moeda(valor).divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentual(BigDecimal valor, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return moeda(valor)
                .multiply(BigDecimal.valueOf(100))
                .divide(base, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal moeda(BigDecimal valor) {
        return ConsultaFiltroUtils.zeroSeNulo(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private record ContractType(String label, String key) {
    }

    private record AplicabilidadeOrcamento(
            boolean aplicavel,
            boolean usarMetaGlobal,
            List<String> filiais,
            List<String> contractTypeKeys,
            String observacao
    ) {
        static AplicabilidadeOrcamento global(List<String> contractTypeKeys) {
            return new AplicabilidadeOrcamento(
                    true,
                    true,
                    List.of(),
                    filtroContrato(contractTypeKeys),
                    null
            );
        }

        static AplicabilidadeOrcamento porFiliais(List<String> filiais, List<String> contractTypeKeys) {
            return new AplicabilidadeOrcamento(
                    true,
                    false,
                    filiais,
                    filtroContrato(contractTypeKeys),
                    null
            );
        }

        static AplicabilidadeOrcamento indisponivel(String observacao) {
            return new AplicabilidadeOrcamento(false, false, List.of(), List.of("__all_contract_types__"), observacao);
        }

        int contractTypeFilterActive() {
            return contractTypeKeys.size() == 1 && "__all_contract_types__".equals(contractTypeKeys.get(0)) ? 0 : 1;
        }

        private static List<String> filtroContrato(List<String> contractTypeKeys) {
            return contractTypeKeys == null || contractTypeKeys.isEmpty()
                    ? List.of("__all_contract_types__")
                    : contractTypeKeys;
        }
    }
}
