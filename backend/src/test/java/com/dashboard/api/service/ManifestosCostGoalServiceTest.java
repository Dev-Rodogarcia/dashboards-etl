package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.manifestos.ManifestosCustosEvolucaoDTO;
import com.dashboard.api.dto.manifestos.ManifestosCustosEvolucaoDTO.CustoDiarioDTO;
import com.dashboard.api.repository.ManifestosCostDataRepository;
import com.dashboard.api.repository.acesso.ManifestosCostGoalRepository;
import com.dashboard.api.repository.acesso.ManifestosCostGoalRepository.GoalAggregateProjection;
import com.dashboard.api.service.acesso.EscopoFilialService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManifestosCostGoalServiceTest {

    private static final ZoneId ZONE_ID_BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate INICIO_MAIO = LocalDate.of(2026, 5, 1);
    private static final LocalDate FIM_MAIO = LocalDate.of(2026, 5, 19);

    @Mock
    private ManifestosCostGoalRepository goalRepository;

    @Mock
    private ManifestosCostDataRepository performanceRepository;

    private ManifestosCostGoalService service;

    @BeforeEach
    void setUp() {
        service = serviceComEscopo(EscopoFilialService.EscopoFilial.comAcessoTotal());
    }

    @Test
    void calculaOrcamentoDiarioSaldoETendenciaComD1() {
        FiltroConsultaDTO filtro = new FiltroConsultaDTO(INICIO_MAIO, FIM_MAIO, Map.of());
        List<CustoDiarioDTO> serie = List.of(
                new CustoDiarioDTO("2026-05-18", new BigDecimal("4400000.00")),
                new CustoDiarioDTO("2026-05-19", new BigDecimal("600000.00"))
        );
        stubCalendarioECustos(filtro, serie);
        when(goalRepository.aggregateGlobalOrBranches(INICIO_MAIO, LocalDate.of(2026, 6, 1)))
                .thenReturn(aggregate("8400000.00", 1));

        ManifestosCustosEvolucaoDTO resultado = service.calcular(
                filtro,
                new BigDecimal("5000000.00")
        );

        assertThat(resultado.orcamentoAplicavel()).isTrue();
        assertThat(resultado.orcamentoConfigurado()).isTrue();
        assertThat(resultado.totalDiasUteis()).isEqualTo(20);
        assertThat(resultado.diasUteisDecorridos()).isEqualTo(11);
        assertThat(resultado.diasUteisRestantes()).isEqualTo(9);
        assertThat(resultado.orcamentoCusto()).isEqualByComparingTo("8400000.00");
        assertThat(resultado.custoReal()).isEqualByComparingTo("5000000.00");
        assertThat(resultado.limiteDiarioBase()).isEqualByComparingTo("420000.00");
        assertThat(resultado.custoMedioDiarioReal()).isEqualByComparingTo("400000.00");
        assertThat(resultado.saldoOrcamentario()).isEqualByComparingTo("4000000.00");
        assertThat(resultado.limiteDiarioDinamico()).isEqualByComparingTo("444444.44");
        assertThat(resultado.tendenciaCusto()).isEqualByComparingTo("8600000.00");
        assertThat(resultado.consumoOrcamento()).isEqualByComparingTo("59.52");
        assertThat(resultado.serieDiaria()).isEqualTo(serie);
    }

    @Test
    void travaLimiteDiarioDinamicoEmZeroQuandoOrcamentoEstaEstourado() {
        FiltroConsultaDTO filtro = new FiltroConsultaDTO(INICIO_MAIO, FIM_MAIO, Map.of());
        stubCalendarioECustos(filtro, List.of());
        when(goalRepository.aggregateGlobalOrBranches(INICIO_MAIO, LocalDate.of(2026, 6, 1)))
                .thenReturn(aggregate("299000.00", 1));

        ManifestosCustosEvolucaoDTO resultado = service.calcular(
                filtro,
                new BigDecimal("5000000.00")
        );

        assertThat(resultado.saldoOrcamentario()).isEqualByComparingTo("-4101000.00");
        assertThat(resultado.limiteDiarioDinamico()).isZero();
    }

    @Test
    void usaMetasPorFilialQuandoEscopoDoUsuarioEhRestrito() {
        service = serviceComEscopo(new EscopoFilialService.EscopoFilial(
                false,
                List.of("SPO", "REC")
        ));
        FiltroConsultaDTO filtro = new FiltroConsultaDTO(INICIO_MAIO, FIM_MAIO, Map.of());
        stubCalendarioECustos(filtro, List.of());
        when(goalRepository.aggregateByBranches(
                INICIO_MAIO,
                LocalDate.of(2026, 6, 1),
                List.of("rec", "spo")
        )).thenReturn(aggregate("3000000.00", 2));

        ManifestosCustosEvolucaoDTO resultado = service.calcular(
                filtro,
                new BigDecimal("1000000.00")
        );

        assertThat(resultado.orcamentoCusto()).isEqualByComparingTo("3000000.00");
        verify(goalRepository, never()).aggregateGlobalOrBranches(any(), any());
    }

    @Test
    void naoAplicaOrcamentoComFiltroSemDimensaoOrcamentaria() {
        FiltroConsultaDTO filtro = new FiltroConsultaDTO(
                INICIO_MAIO,
                FIM_MAIO,
                Map.of("motoristas", List.of("Motorista A"))
        );
        stubCalendarioECustos(filtro, List.of());

        ManifestosCustosEvolucaoDTO resultado = service.calcular(
                filtro,
                new BigDecimal("1000000.00")
        );

        assertThat(resultado.orcamentoAplicavel()).isFalse();
        assertThat(resultado.orcamentoConfigurado()).isFalse();
        assertThat(resultado.observacao()).contains("motoristas");
        assertThat(resultado.orcamentoCusto()).isZero();
        assertThat(resultado.tendenciaCusto()).isEqualByComparingTo("4600000.00");
        verifyNoInteractions(goalRepository);
    }

    @Test
    void metaAusenteNaoProduzSaldoOrcamentarioArtificial() {
        FiltroConsultaDTO filtro = new FiltroConsultaDTO(INICIO_MAIO, FIM_MAIO, Map.of());
        stubCalendarioECustos(filtro, List.of());
        when(goalRepository.aggregateGlobalOrBranches(INICIO_MAIO, LocalDate.of(2026, 6, 1)))
                .thenReturn(aggregate("0.00", 0));

        ManifestosCustosEvolucaoDTO resultado = service.calcular(
                filtro,
                new BigDecimal("1000000.00")
        );

        assertThat(resultado.orcamentoAplicavel()).isTrue();
        assertThat(resultado.orcamentoConfigurado()).isFalse();
        assertThat(resultado.saldoOrcamentario()).isZero();
        assertThat(resultado.limiteDiarioDinamico()).isZero();
        assertThat(resultado.consumoOrcamento()).isZero();
    }

    private void stubCalendarioECustos(FiltroConsultaDTO filtro, List<CustoDiarioDTO> serie) {
        when(performanceRepository.buscarCustosDiarios(filtro)).thenReturn(serie);
        when(performanceRepository.buscarUltimoDiaUtilFechado(FIM_MAIO))
                .thenReturn(LocalDate.of(2026, 5, 18));
        when(performanceRepository.contarDiasUteisCalendario(
                INICIO_MAIO,
                LocalDate.of(2026, 5, 31)
        )).thenReturn(20);
        when(performanceRepository.contarDiasUteisCalendario(
                INICIO_MAIO,
                LocalDate.of(2026, 5, 18)
        )).thenReturn(11);
        when(performanceRepository.buscarCustoTotal(any(FiltroConsultaDTO.class)))
                .thenReturn(new BigDecimal("4400000.00"));
    }

    private ManifestosCostGoalService serviceComEscopo(EscopoFilialService.EscopoFilial escopo) {
        EscopoFilialService escopoService = new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return escopo;
            }
        };
        return new ManifestosCostGoalService(
                goalRepository,
                performanceRepository,
                escopoService,
                Clock.fixed(Instant.parse("2026-05-19T12:00:00Z"), ZONE_ID_BRASILIA)
        );
    }

    private static GoalAggregateProjection aggregate(String valor, long configuradas) {
        return new GoalAggregateProjection() {
            @Override
            public BigDecimal getCostGoal() {
                return new BigDecimal(valor);
            }

            @Override
            public long getConfiguredGoals() {
                return configuradas;
            }
        };
    }
}
