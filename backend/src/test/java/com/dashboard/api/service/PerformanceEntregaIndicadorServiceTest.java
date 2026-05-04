package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.PerformanceEntregaOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.PerformanceEntregaSeriePointDTO;
import com.dashboard.api.model.VisaoFretesEntity;
import com.dashboard.api.repository.VisaoFretesRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformanceEntregaIndicadorServiceTest {

    @Mock
    private VisaoFretesRepository fretesRepository;

    private PerformanceEntregaIndicadorService service;

    @BeforeEach
    void setUp() {
        service = new PerformanceEntregaIndicadorService(
                new ValidadorPeriodoService(),
                fretesRepository,
                escopoSemRestricao()
        );
    }

    @Test
    void buscarOverviewDeveConsiderarEmAbertoNoDenominadorEIgnorarCancelados() {
        VisaoFretesEntity cortesia = frete(105L, "SPO", "CWB", "finalizado", OffsetDateTime.parse("2026-04-06T10:00:00-03:00"), LocalDate.of(2026, 4, 7), "NO PRAZO", 0, LocalDateTime.of(2026, 4, 3, 9, 0));
        TestReflectionUtils.setField(cortesia, "cortesiaFlag", true);

        VisaoFretesEntity pendente = frete(106L, "SPO", "CWB", "finalizado", OffsetDateTime.parse("2026-04-07T10:00:00-03:00"), LocalDate.of(2026, 4, 8), "NO PRAZO", 0, LocalDateTime.of(2026, 4, 3, 9, 0));
        TestReflectionUtils.setField(pendente, "documentoOficialTipo", "Pendente/Não Emitido");

        when(fretesRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                frete(101L, "SPO", "CWB", "finalizado", OffsetDateTime.parse("2026-04-02T10:00:00-03:00"), LocalDate.of(2026, 4, 3), "NO PRAZO", 0, LocalDateTime.of(2026, 4, 3, 9, 0)),
                frete(102L, "SPO", "CWB", "finalizado", OffsetDateTime.parse("2026-04-03T10:00:00-03:00"), LocalDate.of(2026, 4, 4), "FORA DO PRAZO", 2, LocalDateTime.of(2026, 4, 3, 9, 0)),
                frete(103L, "SPO", "CWB", "cancelado", OffsetDateTime.parse("2026-04-04T10:00:00-03:00"), LocalDate.of(2026, 4, 5), "NO PRAZO", 0, LocalDateTime.of(2026, 4, 3, 9, 0)),
                frete(104L, "SPO", "CWB", "finalizado", OffsetDateTime.parse("2026-04-05T10:00:00-03:00"), LocalDate.of(2026, 4, 6), null, null, LocalDateTime.of(2026, 4, 3, 9, 0)),
                cortesia,
                pendente
        ));

        PerformanceEntregaOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(overview.totalEntregas()).isEqualTo(4);
        assertThat(overview.entregasNoPrazo()).isEqualTo(2);
        assertThat(overview.entregasForaDoPrazo()).isEqualTo(1);
        assertThat(overview.pctNoPrazo()).isEqualTo(50.0);
    }

    @Test
    void buscarOverviewDeveFiltrarPelaFilialPerformanceEmVezDaFilialEmissora() {
        when(fretesRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                frete(201L, "SPO", "CWB", "finalizado", OffsetDateTime.parse("2026-04-02T10:00:00-03:00"), LocalDate.of(2026, 4, 3), "NO PRAZO", 0, LocalDateTime.of(2026, 4, 3, 9, 0)),
                frete(202L, "CWB", "SPO", "finalizado", OffsetDateTime.parse("2026-04-02T10:00:00-03:00"), LocalDate.of(2026, 4, 3), "NO PRAZO", 0, LocalDateTime.of(2026, 4, 3, 9, 0))
        ));

        PerformanceEntregaOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of("filiais", List.of("CWB")))
        );

        assertThat(overview.totalEntregas()).isEqualTo(1);
        assertThat(overview.entregasNoPrazo()).isEqualTo(1);
        assertThat(overview.entregasForaDoPrazo()).isZero();
    }

    @Test
    void buscarSerieDeveUsarPrevisaoEntregaComoEixoEFilialPerformanceComoAgrupador() {
        VisaoFretesEntity frete = frete(301L, "SPO", "REC", "finalizado", OffsetDateTime.parse("2026-04-10T10:00:00-03:00"), LocalDate.of(2026, 4, 6), "NO PRAZO", 0, LocalDateTime.of(2026, 4, 3, 9, 0));
        TestReflectionUtils.setField(frete, "previsaoEntrega", LocalDate.of(2026, 4, 5));
        when(fretesRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                frete
        ));

        List<PerformanceEntregaSeriePointDTO> serie = service.buscarSerie(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(serie).singleElement().satisfies(point -> {
            assertThat(point.date()).isEqualTo("2026-04-05");
            assertThat(point.filialPerformance()).isEqualTo("REC");
            assertThat(point.totalEntregas()).isEqualTo(1);
            assertThat(point.entregasNoPrazo()).isEqualTo(1);
            assertThat(point.entregasForaDoPrazo()).isZero();
            assertThat(point.pctNoPrazo()).isEqualTo(100.0);
        });
    }

    private static VisaoFretesEntity frete(
            Long numeroMinuta,
            String filialEmissora,
            String filialPerformance,
            String statusFrete,
            OffsetDateTime dataFrete,
            LocalDate dataFinalizacao,
            String performanceStatus,
            Integer diferencaDias,
            LocalDateTime dataExtracao
    ) {
        VisaoFretesEntity entity = TestReflectionUtils.novaInstancia(VisaoFretesEntity.class);
        TestReflectionUtils.setField(entity, "id", numeroMinuta);
        TestReflectionUtils.setField(entity, "numeroMinuta", numeroMinuta);
        TestReflectionUtils.setField(entity, "dataFrete", dataFrete);
        TestReflectionUtils.setField(entity, "filialNome", filialEmissora);
        TestReflectionUtils.setField(entity, "filialEmissora", filialEmissora);
        TestReflectionUtils.setField(entity, "responsavelRegiaoDestino", filialPerformance);
        TestReflectionUtils.setField(entity, "status", statusFrete);
        TestReflectionUtils.setField(entity, "documentoOficialTipo", "CT-e");
        TestReflectionUtils.setField(entity, "cortesiaFlag", false);
        TestReflectionUtils.setField(entity, "valorTotal", java.math.BigDecimal.TEN);
        TestReflectionUtils.setField(entity, "previsaoEntrega", LocalDate.of(2026, 4, 1));
        TestReflectionUtils.setField(entity, "dataFinalizacao", dataFinalizacao);
        TestReflectionUtils.setField(entity, "performanceStatus", performanceStatus);
        TestReflectionUtils.setField(entity, "performanceDiferencaDias", diferencaDias);
        TestReflectionUtils.setField(entity, "dataExtracao", dataExtracao);
        return entity;
    }

    private static EscopoFilialService escopoSemRestricao() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }
}
