package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemMercadoriasOverviewDTO;
import com.dashboard.api.model.VisaoFretesEntity;
import com.dashboard.api.repository.VisaoFretesRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CubagemMercadoriasIndicadorServiceTest {

    @Mock
    private VisaoFretesRepository fretesRepository;

    private CubagemMercadoriasIndicadorService service;

    @BeforeEach
    void setUp() {
        service = new CubagemMercadoriasIndicadorService(
                new ValidadorPeriodoService(),
                fretesRepository,
                escopoSemRestricao(),
                PeriodoOffsetDateTimeHelper.padrao()
        );
    }

    @Test
    void buscarOverviewDeveCalcularCubagemEPesoReal() {
        when(fretesRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                frete(1L, "SPO", new BigDecimal("1.20"), BigDecimal.ZERO, new BigDecimal("10"), LocalDateTime.of(2026, 4, 3, 8, 0)),
                frete(2L, "SPO", BigDecimal.ZERO, new BigDecimal("25"), new BigDecimal("8"), LocalDateTime.of(2026, 4, 3, 8, 0)),
                frete(3L, "SPO", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDateTime.of(2026, 4, 3, 8, 0))
        ));

        CubagemMercadoriasOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(overview.totalFretes()).isEqualTo(3);
        assertThat(overview.fretesCubados()).isEqualTo(2);
        assertThat(overview.fretesComPesoReal()).isEqualTo(2);
        assertThat(overview.pctCubagem()).isEqualTo(66.7);
    }

    private static VisaoFretesEntity frete(
            Long numeroMinuta,
            String filialEmissora,
            BigDecimal totalM3,
            BigDecimal pesoCubado,
            BigDecimal pesoReal,
            LocalDateTime dataExtracao
    ) {
        VisaoFretesEntity entity = TestReflectionUtils.novaInstancia(VisaoFretesEntity.class);
        TestReflectionUtils.setField(entity, "id", numeroMinuta);
        TestReflectionUtils.setField(entity, "numeroMinuta", numeroMinuta);
        TestReflectionUtils.setField(entity, "dataFrete", OffsetDateTime.parse("2026-04-02T10:00:00-03:00"));
        TestReflectionUtils.setField(entity, "filialEmissora", filialEmissora);
        TestReflectionUtils.setField(entity, "totalM3", totalM3);
        TestReflectionUtils.setField(entity, "pesoCubado", pesoCubado);
        TestReflectionUtils.setField(entity, "pesoReal", pesoReal);
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
