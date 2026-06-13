package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.manifestos.ManifestosCustosEvolucaoDTO;
import com.dashboard.api.dto.manifestos.ManifestosCustosEvolucaoDTO.CustoDiarioDTO;
import com.dashboard.api.repository.ManifestosCostDataRepository;
import com.dashboard.api.repository.acesso.ManifestosCostGoalRepository;
import com.dashboard.api.repository.acesso.ManifestosCostGoalRepository.GoalAggregateProjection;
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
    private static final List<String> FILTROS_SEM_DIMENSAO_ORCAMENTARIA = List.of(
            "status",
            "motoristas",
            "veiculos",
            "tiposCarga",
            "tiposContrato",
            "tipoMotorista"
    );

    private final ManifestosCostGoalRepository goalRepository;
    private final ManifestosCostDataRepository performanceRepository;
    private final EscopoFilialService escopoFilialService;
    private final Clock clock;

    @Autowired
    public ManifestosCostGoalService(
            ManifestosCostGoalRepository goalRepository,
            ManifestosCostDataRepository performanceRepository,
            EscopoFilialService escopoFilialService
    ) {
        this(
                goalRepository,
                performanceRepository,
                escopoFilialService,
                Clock.system(ZONE_ID_BRASILIA)
        );
    }

    ManifestosCostGoalService(
            ManifestosCostGoalRepository goalRepository,
            ManifestosCostDataRepository performanceRepository,
            EscopoFilialService escopoFilialService,
            Clock clock
    ) {
        this.goalRepository = goalRepository;
        this.performanceRepository = performanceRepository;
        this.escopoFilialService = escopoFilialService;
        this.clock = clock != null ? clock : Clock.system(ZONE_ID_BRASILIA);
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
                ? goalRepository.aggregateGlobalOrBranches(inicioCompetencia, fimCompetenciaExclusivo)
                : goalRepository.aggregateByBranches(
                        inicioCompetencia,
                        fimCompetenciaExclusivo,
                        aplicabilidade.filiais()
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

        List<String> filiaisSelecionadas = normalizar(filtro.valores("filiais"));
        if (escopo.acessoTotal() && filiaisSelecionadas.isEmpty()) {
            return AplicabilidadeOrcamento.global();
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
        return AplicabilidadeOrcamento.porFiliais(filiaisPermitidas);
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

    private record AplicabilidadeOrcamento(
            boolean aplicavel,
            boolean usarMetaGlobal,
            List<String> filiais,
            String observacao
    ) {
        static AplicabilidadeOrcamento global() {
            return new AplicabilidadeOrcamento(true, true, List.of(), null);
        }

        static AplicabilidadeOrcamento porFiliais(List<String> filiais) {
            return new AplicabilidadeOrcamento(true, false, filiais, null);
        }

        static AplicabilidadeOrcamento indisponivel(String observacao) {
            return new AplicabilidadeOrcamento(false, false, List.of(), observacao);
        }
    }
}
