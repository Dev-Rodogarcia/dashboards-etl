package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemMercadoriasOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemMercadoriasRowDTO;
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
                PeriodoOffsetDateTimeHelper.padrao(),
                ""
        );
    }

    @Test
    void buscarOverviewDeveCalcularCubagemEPesoReal() {
        when(fretesRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                frete(1L, "SPO", new BigDecimal("1.20"), BigDecimal.ZERO, new BigDecimal("10"), "emitido", "03902443000166", LocalDateTime.of(2026, 4, 3, 8, 0)),
                frete(2L, "SPO", BigDecimal.ZERO, new BigDecimal("25"), new BigDecimal("8"), "emitido", "44519650000113", LocalDateTime.of(2026, 4, 3, 8, 0)),
                frete(3L, "SPO", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "emitido", "55183248001018", LocalDateTime.of(2026, 4, 3, 8, 0))
        ));

        CubagemMercadoriasOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(overview.totalFretes()).isEqualTo(3);
        assertThat(overview.fretesCubados()).isEqualTo(2);
        assertThat(overview.fretesComPesoReal()).isEqualTo(2);
        assertThat(overview.pctCubagem()).isEqualTo(66.7);
    }

    @Test
    void buscarOverviewDeveIgnorarCanceladosEDocumentosExcluidosENaoUsarPesoComoCriterioPrincipal() {
        service = new CubagemMercadoriasIndicadorService(
                new ValidadorPeriodoService(),
                fretesRepository,
                escopoSemRestricao(),
                PeriodoOffsetDateTimeHelper.padrao(),
                "43.996.693/0001-27; 55183248001018"
        );

        when(fretesRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                frete(10L, "SPO", new BigDecimal("1.20"), BigDecimal.ZERO, new BigDecimal("5"), "emitido", "43.996.693/0001-27", LocalDateTime.of(2026, 4, 3, 8, 0)),
                frete(11L, "SPO", new BigDecimal("2.10"), BigDecimal.ZERO, new BigDecimal("8"), "cancelado", "03902443000166", LocalDateTime.of(2026, 4, 3, 8, 0)),
                frete(12L, "SPO", BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("11"), "emitido", "03902443000166", LocalDateTime.of(2026, 4, 3, 8, 0)),
                frete(13L, "SPO", BigDecimal.ZERO, new BigDecimal("25"), BigDecimal.ZERO, "EMITIDO", "38948235000182", LocalDateTime.of(2026, 4, 3, 8, 0))
        ));

        CubagemMercadoriasOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(overview.totalFretes()).isEqualTo(2);
        assertThat(overview.fretesCubados()).isEqualTo(1);
        assertThat(overview.fretesComPesoReal()).isEqualTo(1);
        assertThat(overview.pctCubagem()).isEqualTo(50.0);
    }

    @Test
    void buscarTabelaDeveExporDocumentoDoRemetenteNormalizado() {
        when(fretesRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                frete(20L, "CWB", new BigDecimal("1.20"), BigDecimal.ZERO, new BigDecimal("5"), "emitido", "43.996.693/0001-27", LocalDateTime.of(2026, 4, 3, 8, 0))
        ));

        List<CubagemMercadoriasRowDTO> tabela = service.buscarTabela(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of()),
                100
        );

        assertThat(tabela).singleElement().satisfies(row -> {
            assertThat(row.numeroMinuta()).isEqualTo(20L);
            assertThat(row.remetenteDocumento()).isEqualTo("43996693000127");
            assertThat(row.cubado()).isTrue();
        });
    }

    @Test
    void buscarOverviewDeveExigirValorPositivoParaCubagem() {
        when(fretesRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                frete(30L, "REC", new BigDecimal("-1.00"), BigDecimal.ZERO, new BigDecimal("4"), "emitido", "03902443000166", LocalDateTime.of(2026, 4, 3, 8, 0))
        ));

        CubagemMercadoriasOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(overview.totalFretes()).isEqualTo(1);
        assertThat(overview.fretesCubados()).isZero();
        assertThat(overview.fretesComPesoReal()).isEqualTo(1);
        assertThat(overview.pctCubagem()).isZero();
    }

    @Test
    void buscarOverviewDeveUsarTotalM3SemFallbackParaM3() {
        VisaoFretesEntity entity = frete(
                40L,
                "POA",
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "emitido",
                "44519650000113",
                LocalDateTime.of(2026, 4, 3, 8, 0)
        );
        TestReflectionUtils.setField(entity, "m3Total", new BigDecimal("9.90"));

        when(fretesRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(entity));

        CubagemMercadoriasOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(overview.totalFretes()).isEqualTo(1);
        assertThat(overview.fretesCubados()).isZero();
        assertThat(overview.pctCubagem()).isZero();
    }

    private static VisaoFretesEntity frete(
            Long numeroMinuta,
            String filialEmissora,
            BigDecimal totalM3,
            BigDecimal pesoCubado,
            BigDecimal pesoReal,
            String status,
            String remetenteDocumento,
            LocalDateTime dataExtracao
    ) {
        VisaoFretesEntity entity = TestReflectionUtils.novaInstancia(VisaoFretesEntity.class);
        TestReflectionUtils.setField(entity, "id", numeroMinuta);
        TestReflectionUtils.setField(entity, "numeroMinuta", numeroMinuta);
        TestReflectionUtils.setField(entity, "dataFrete", OffsetDateTime.parse("2026-04-02T10:00:00-03:00"));
        TestReflectionUtils.setField(entity, "filialNome", filialEmissora);
        TestReflectionUtils.setField(entity, "filialEmissora", filialEmissora);
        TestReflectionUtils.setField(entity, "totalM3", totalM3);
        TestReflectionUtils.setField(entity, "pesoCubado", pesoCubado);
        TestReflectionUtils.setField(entity, "pesoTaxado", pesoReal);
        TestReflectionUtils.setField(entity, "pesoReal", pesoReal);
        TestReflectionUtils.setField(entity, "status", status);
        TestReflectionUtils.setField(entity, "remetenteDocumento", remetenteDocumento);
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
