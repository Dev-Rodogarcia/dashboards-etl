package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.PerformanceEntregaOverviewDTO;
import com.dashboard.api.model.VisaoFretesEntity;
import com.dashboard.api.model.VisaoLocalizacaoCargasEntity;
import com.dashboard.api.repository.VisaoFretesRepository;
import com.dashboard.api.repository.VisaoLocalizacaoCargasRepository;
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

    @Mock
    private VisaoLocalizacaoCargasRepository localizacaoRepository;

    private PerformanceEntregaIndicadorService service;

    @BeforeEach
    void setUp() {
        service = new PerformanceEntregaIndicadorService(
                new ValidadorPeriodoService(),
                fretesRepository,
                localizacaoRepository,
                escopoSemRestricao(),
                PeriodoOffsetDateTimeHelper.padrao()
        );
    }

    @Test
    void buscarOverviewDeveCalcularPercentualESuportarFallbackDeResponsavel() {
        when(fretesRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                frete(101L, "SPO", null, LocalDate.of(2026, 4, 2), "NO PRAZO", 0, LocalDateTime.of(2026, 4, 3, 9, 0)),
                frete(102L, "SPO", "REC", LocalDate.of(2026, 4, 2), "FORA DO PRAZO", 2, LocalDateTime.of(2026, 4, 3, 9, 0)),
                frete(103L, "SPO", null, null, null, null, LocalDateTime.of(2026, 4, 3, 9, 0))
        ));
        when(localizacaoRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                localizacao(101L, "CWB")
        ));

        PerformanceEntregaOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(overview.totalEntregas()).isEqualTo(3);
        assertThat(overview.entregasNoPrazo()).isEqualTo(1);
        assertThat(overview.entregasSemDados()).isEqualTo(1);
        assertThat(overview.pctNoPrazo()).isEqualTo(33.3);
    }

    private static VisaoFretesEntity frete(
            Long numeroMinuta,
            String filialEmissora,
            String responsavel,
            LocalDate dataFinalizacao,
            String status,
            Integer diferencaDias,
            LocalDateTime dataExtracao
    ) {
        VisaoFretesEntity entity = TestReflectionUtils.novaInstancia(VisaoFretesEntity.class);
        TestReflectionUtils.setField(entity, "id", numeroMinuta);
        TestReflectionUtils.setField(entity, "numeroMinuta", numeroMinuta);
        TestReflectionUtils.setField(entity, "dataFrete", OffsetDateTime.parse("2026-04-02T10:00:00-03:00"));
        TestReflectionUtils.setField(entity, "filialEmissora", filialEmissora);
        TestReflectionUtils.setField(entity, "responsavelRegiaoDestino", responsavel);
        TestReflectionUtils.setField(entity, "previsaoEntrega", LocalDate.of(2026, 4, 1));
        TestReflectionUtils.setField(entity, "dataFinalizacao", dataFinalizacao);
        TestReflectionUtils.setField(entity, "performanceStatus", status);
        TestReflectionUtils.setField(entity, "performanceDiferencaDias", diferencaDias);
        TestReflectionUtils.setField(entity, "dataExtracao", dataExtracao);
        return entity;
    }

    private static VisaoLocalizacaoCargasEntity localizacao(Long minuta, String responsavel) {
        VisaoLocalizacaoCargasEntity entity = TestReflectionUtils.novaInstancia(VisaoLocalizacaoCargasEntity.class);
        TestReflectionUtils.setField(entity, "sequenceNumber", minuta);
        TestReflectionUtils.setField(entity, "responsavelRegiaoDestino", responsavel);
        TestReflectionUtils.setField(entity, "dataFrete", OffsetDateTime.parse("2026-04-02T10:00:00-03:00"));
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
