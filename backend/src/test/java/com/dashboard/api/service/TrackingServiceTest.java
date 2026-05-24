package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.tracking.TrackingChartsDTO;
import com.dashboard.api.dto.tracking.TrackingDashboardDTO;
import com.dashboard.api.dto.tracking.TrackingOverviewDTO;
import com.dashboard.api.model.VisaoLocalizacaoCargasEntity;
import com.dashboard.api.repository.VisaoLocalizacaoCargasRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private VisaoLocalizacaoCargasRepository repository;

    private TrackingService service;

    @BeforeEach
    void setUp() {
        service = new TrackingService(new ValidadorPeriodoService(), repository);
    }

    @Test
    void buscarOverviewDeveContarStatusNuloComoAbertoNaPrevisaoVencida() {
        when(repository.findByDataFreteGreaterThanEqualAndDataFreteLessThan(any(), any())).thenReturn(List.of(
                carga(1L, "Em entrega", -2),
                carga(2L, null, -2),
                carga(3L, "Finalizado", -2),
                carga(4L, "Manifestado", 2)
        ));

        TrackingOverviewDTO overview = service.buscarOverview(filtroPadrao());

        assertThat(overview.totalCargas()).isEqualTo(4);
        assertThat(overview.emTransito()).isEqualTo(2);
        assertThat(overview.previsaoVencida()).isEqualTo(2);
        assertThat(overview.valorFreteEmCarteira()).isEqualByComparingTo("400.00");
        assertThat(overview.pesoTaxadoTotal()).isEqualByComparingTo("40.00");
        assertThat(overview.pctFinalizado()).isEqualTo(25.0);
    }

    @Test
    void buscarOverviewDeveExpurgarCanceladosDoDenominadorDeFinalizacao() {
        when(repository.findByDataFreteGreaterThanEqualAndDataFreteLessThan(any(), any())).thenReturn(List.of(
                carga(1L, "Finalizado", -2),
                carga(2L, "Entregue", -2),
                carga(3L, "Cancelado", -2),
                carga(4L, "Manifestado", -2),
                carga(5L, null, -2)
        ));

        TrackingOverviewDTO overview = service.buscarOverview(filtroPadrao());

        assertThat(overview.totalCargas()).isEqualTo(5);
        assertThat(overview.previsaoVencida()).isEqualTo(2);
        assertThat(overview.pctFinalizado()).isEqualTo(50.0);
    }

    @Test
    void buscarOverviewDeveConsiderarPrevisaoVencidaPorDataCivilLocal() {
        LocalDate hojeSaoPaulo = PeriodoOffsetDateTimeHelper.padrao().hoje();
        when(repository.findByDataFreteGreaterThanEqualAndDataFreteLessThan(any(), any())).thenReturn(List.of(
                cargaComPrevisao(1L, "Em entrega", hojeSaoPaulo.atTime(0, 1).atOffset(ZoneOffset.ofHours(-3))),
                cargaComPrevisao(2L, "Manifestado", hojeSaoPaulo.minusDays(1).atTime(23, 59).atOffset(ZoneOffset.ofHours(-3))),
                cargaComPrevisao(3L, "Finalizado", hojeSaoPaulo.minusDays(2).atTime(10, 0).atOffset(ZoneOffset.ofHours(-3)))
        ));

        TrackingOverviewDTO overview = service.buscarOverview(filtroPadrao());

        assertThat(overview.previsaoVencida()).isEqualTo(1);
    }

    @Test
    void buscarGraficosDeveAgruparPrevisaoVencidaSemFilial() {
        when(repository.findByDataFreteGreaterThanEqualAndDataFreteLessThan(any(), any())).thenReturn(List.of(
                carga(1L, "Em entrega", -2, null),
                carga(2L, "Manifestado", -3, "   "),
                carga(3L, "Em entrega", -1, "Filial SP"),
                carga(4L, "Finalizado", -5, null)
        ));

        TrackingChartsDTO graficos = service.buscarGraficos(filtroPadrao());

        assertThat(graficos.previsaoVencidaPorFilialAtual()).extracting(
                dto -> dto.filialAtual(),
                dto -> dto.vencidas(),
                dto -> dto.total()
        ).containsExactly(
                org.assertj.core.groups.Tuple.tuple("Sem filial", 2, 2),
                org.assertj.core.groups.Tuple.tuple("Filial SP", 1, 1)
        );
    }

    @Test
    void buscarOverviewDeveConsultarPeriodoNoFusoDeSaoPaulo() {
        when(repository.findByDataFreteGreaterThanEqualAndDataFreteLessThan(any(), any())).thenReturn(List.of());

        service.buscarOverview(filtroPadrao());

        ArgumentCaptor<OffsetDateTime> inicio = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> fim = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).findByDataFreteGreaterThanEqualAndDataFreteLessThan(inicio.capture(), fim.capture());

        assertThat(inicio.getValue())
                .isEqualTo(OffsetDateTime.of(2026, 2, 21, 0, 0, 0, 0, ZoneOffset.ofHours(-3)));
        assertThat(fim.getValue())
                .isEqualTo(OffsetDateTime.of(2026, 3, 24, 0, 0, 0, 0, ZoneOffset.ofHours(-3)));
    }

    @Test
    void deveExigirFilialAtualParaPerfilComAcessoTotal() {
        TrackingService serviceComAcessoTotal = serviceComEscopo(EscopoFilialService.EscopoFilial.comAcessoTotal());

        assertThatThrownBy(() -> serviceComAcessoTotal.normalizarFiltroComFilialAtualObrigatoria(filtroPadrao()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Filial Atual é obrigatória");
    }

    @Test
    void deveAutoselecionarFilialAtualQuandoEscopoTiverUnicaFilial() {
        TrackingService serviceComFilialUnica = serviceComEscopo(
                new EscopoFilialService.EscopoFilial(false, List.of("SPO - RODOGARCIA"))
        );

        FiltroConsultaDTO filtro = serviceComFilialUnica.normalizarFiltroComFilialAtualObrigatoria(filtroPadrao());

        assertThat(filtro.valores("filialAtual")).containsExactly("SPO - RODOGARCIA");
    }

    @Test
    void buscarDashboardDeveUsarContratoGovernadoDaViewDeTracking() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        TrackingService serviceDashboard = new TrackingService(
                new ValidadorPeriodoService(),
                repository,
                escopoSemRestricao(),
                PeriodoOffsetDateTimeHelper.padrao(),
                jdbcTemplate,
                new DashboardExportSqlBuilder(PeriodoOffsetDateTimeHelper.padrao())
        );

        serviceDashboard.buscarDashboard(new FiltroConsultaDTO(
                LocalDate.of(2026, 2, 21),
                LocalDate.of(2026, 3, 23),
                Map.of("filialAtual", List.of("SPO - RODOGARCIA"))
        ));

        String sqlExecutado = String.join("\n", jdbcTemplate.sqls());
        assertThat(sqlExecutado)
                .contains("[Status Normalizado]")
                .contains("[Peso Taxado Decimal]")
                .contains("[Valor NF Decimal]")
                .contains("TRY_CONVERT(DATETIME2, [Data de extracao])")
                .doesNotContain("MAX([Data de extracao])")
                .contains("TRY_CONVERT(DECIMAL(18, 2), [Valor Frete])")
                .contains("TRY_CONVERT(DECIMAL(18, 2), REPLACE(REPLACE(CONVERT(NVARCHAR(50), [Valor Frete]), '.', ''), ',', '.'))")
                .contains("TRY_CONVERT(INT, [Volumes])")
                .doesNotContain("SUM(COALESCE([Valor Frete], 0))")
                .doesNotContain("SUM(COALESCE([Volumes], 0))")
                .contains("TRY_CONVERT(DECIMAL(18, 3), [Peso Taxado])")
                .contains("TRY_CONVERT(DECIMAL(18, 3), REPLACE(REPLACE(CONVERT(NVARCHAR(50), [Peso Taxado]), '.', ''), ',', '.'))")
                .contains("TRY_CONVERT(DECIMAL(18, 2), [Valor NF])")
                .contains("TRY_CONVERT(DECIMAL(18, 2), REPLACE(REPLACE(CONVERT(NVARCHAR(50), [Valor NF]), '.', ''), ',', '.'))")
                .contains("[Sigla Responsável Região Destino]")
                .contains("'SEM_MAP'")
                .contains("[Responsável pela Região de Destino]");
    }

    @Test
    void buscarDashboardDeveManterFallbackQuandoViewAindaNaoTemColunasGovernadas() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate(List.of(
                "Status Carga",
                "Peso Taxado",
                "Valor NF",
                "Responsável pela Região de Destino",
                "Região Destino"
        ));
        TrackingService serviceDashboard = new TrackingService(
                new ValidadorPeriodoService(),
                repository,
                escopoSemRestricao(),
                PeriodoOffsetDateTimeHelper.padrao(),
                jdbcTemplate,
                new DashboardExportSqlBuilder(PeriodoOffsetDateTimeHelper.padrao())
        );

        serviceDashboard.buscarDashboard(new FiltroConsultaDTO(
                LocalDate.of(2026, 2, 21),
                LocalDate.of(2026, 3, 23),
                Map.of("filialAtual", List.of("SPO - RODOGARCIA"))
        ));

        String sqlExecutado = String.join("\n", jdbcTemplate.sqls());
        assertThat(sqlExecutado)
                .doesNotContain("[Status Normalizado]")
                .doesNotContain("[Peso Taxado Decimal]")
                .doesNotContain("[Valor NF Decimal]")
                .doesNotContain("[Sigla Responsável Região Destino]")
                .contains("[Status Carga]")
                .contains("TRY_CONVERT(DATETIME2, [Data de extracao])")
                .doesNotContain("MAX([Data de extracao])")
                .contains("TRY_CONVERT(DECIMAL(18, 2), [Valor Frete])")
                .contains("TRY_CONVERT(INT, [Volumes])")
                .doesNotContain("SUM(COALESCE([Valor Frete], 0))")
                .doesNotContain("SUM(COALESCE([Volumes], 0))")
                .contains("TRY_CONVERT(DECIMAL(18, 3), [Peso Taxado])")
                .contains("TRY_CONVERT(DECIMAL(18, 2), [Valor NF])")
                .contains("[Responsável pela Região de Destino]")
                .contains("[Região Destino]");
    }

    @Test
    void buscarDashboardDeveUsarFallbackLegadoQuandoAgregadoSqlFalhar() {
        TrackingService serviceDashboard = new TrackingService(
                new ValidadorPeriodoService(),
                repository,
                escopoSemRestricao(),
                PeriodoOffsetDateTimeHelper.padrao(),
                new FalhandoNamedParameterJdbcTemplate(),
                new DashboardExportSqlBuilder(PeriodoOffsetDateTimeHelper.padrao())
        );
        TrackingDashboardDTO dashboard = serviceDashboard.buscarDashboard(new FiltroConsultaDTO(
                LocalDate.of(2026, 4, 23),
                LocalDate.of(2026, 5, 23),
                Map.of("filialAtual", List.of("CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA"))
        ));

        assertThat(dashboard.overview().totalCargas()).isEqualTo(1);
        assertThat(dashboard.matrizRegiaoDestino()).hasSize(1);
        assertThat(dashboard.graficos().statusDistribuicao())
                .extracting(dto -> dto.status(), dto -> dto.total())
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Pendente", 1));
    }

    private static FiltroConsultaDTO filtroPadrao() {
        return new FiltroConsultaDTO(LocalDate.of(2026, 2, 21), LocalDate.of(2026, 3, 23), Map.of());
    }

    private TrackingService serviceComEscopo(EscopoFilialService.EscopoFilial escopo) {
        return new TrackingService(
                new ValidadorPeriodoService(),
                repository,
                new EscopoFilialService(null, null) {
                    @Override
                    public EscopoFilialService.EscopoFilial escopoAtual() {
                        return escopo;
                    }
                },
                PeriodoOffsetDateTimeHelper.padrao(),
                null,
                null
        );
    }

    private EscopoFilialService escopoSemRestricao() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilialService.EscopoFilial escopoAtual() {
                return EscopoFilialService.EscopoFilial.comAcessoTotal();
            }
        };
    }

    private static class CapturandoNamedParameterJdbcTemplate extends NamedParameterJdbcTemplate {
        private final List<String> sqls = new ArrayList<>();
        private final List<String> colunas;

        protected CapturandoNamedParameterJdbcTemplate() {
            this(List.of(
                    "Status Normalizado",
                    "Peso Taxado Decimal",
                    "Valor NF Decimal",
                    "Sigla Responsável Região Destino",
                    "Status Carga",
                    "Peso Taxado",
                    "Valor NF",
                    "Responsável pela Região de Destino",
                    "Região Destino"
            ));
        }

        protected CapturandoNamedParameterJdbcTemplate(List<String> colunas) {
            super(new JdbcTemplate());
            this.colunas = colunas;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> List<T> queryForList(String sql, SqlParameterSource paramSource, Class<T> elementType) {
            sqls.add(sql);
            return (List<T>) colunas;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T queryForObject(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper) {
            sqls.add(sql);
            return (T) new TrackingOverviewDTO(
                    "2026-03-23T12:00:00",
                    0,
                    0,
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0.0
            );
        }

        @Override
        public <T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper) {
            sqls.add(sql);
            return List.of();
        }

        private List<String> sqls() {
            return sqls;
        }
    }

    private static final class FalhandoNamedParameterJdbcTemplate extends CapturandoNamedParameterJdbcTemplate {
        @Override
        public <T> T queryForObject(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper) {
            throw new org.springframework.dao.TransientDataAccessResourceException("falha agregada simulada");
        }

        @Override
        public List<Map<String, Object>> queryForList(String sql, SqlParameterSource paramSource) {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("N° Minuta", 1L);
            row.put("Data do frete", LocalDateTime.of(2026, 5, 20, 10, 0));
            row.put("Volumes", 2);
            row.put("Peso Taxado", "10,5");
            row.put("Valor NF", "1000,00");
            row.put("Valor Frete", "150,00");
            row.put("Filial Atual", "CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA");
            row.put("Região Destino", "Interior");
            row.put("Responsável pela Região de Destino", "CPQ - Interior");
            row.put("Status Carga", "Pendente");
            row.put("Previsão Entrega/Previsão de entrega", LocalDateTime.of(2026, 5, 19, 8, 0));
            row.put("Data de extracao", LocalDateTime.of(2026, 5, 23, 12, 0));
            return List.of(row);
        }
    }

    private static VisaoLocalizacaoCargasEntity carga(Long numeroMinuta, String statusCarga, int diasPrevisao) {
        return carga(numeroMinuta, statusCarga, diasPrevisao, null);
    }

    private static VisaoLocalizacaoCargasEntity carga(Long numeroMinuta, String statusCarga, int diasPrevisao, String filialAtual) {
        return cargaComPrevisao(numeroMinuta, statusCarga, OffsetDateTime.now().plusDays(diasPrevisao), filialAtual);
    }

    private static VisaoLocalizacaoCargasEntity cargaComPrevisao(
            Long numeroMinuta,
            String statusCarga,
            OffsetDateTime previsaoEntrega
    ) {
        return cargaComPrevisao(numeroMinuta, statusCarga, previsaoEntrega, null);
    }

    private static VisaoLocalizacaoCargasEntity cargaComPrevisao(
            Long numeroMinuta,
            String statusCarga,
            OffsetDateTime previsaoEntrega,
            String filialAtual
    ) {
        VisaoLocalizacaoCargasEntity entity = Objects.requireNonNull(novaInstancia(VisaoLocalizacaoCargasEntity.class));
        ReflectionTestUtils.setField(entity, "sequenceNumber", numeroMinuta);
        ReflectionTestUtils.setField(entity, "statusCarga", statusCarga);
        ReflectionTestUtils.setField(entity, "filialAtual", filialAtual);
        ReflectionTestUtils.setField(entity, "previsaoEntrega", previsaoEntrega);
        ReflectionTestUtils.setField(entity, "dataFrete", OffsetDateTime.of(2026, 3, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        ReflectionTestUtils.setField(entity, "valorFrete", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(entity, "pesoTaxado", "10");
        ReflectionTestUtils.setField(entity, "regiaoDestino", "Sudeste");
        ReflectionTestUtils.setField(entity, "dataExtracao", LocalDateTime.of(2026, 3, 23, 12, 0));
        return entity;
    }

    private static <T> T novaInstancia(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Nao foi possivel instanciar " + type.getSimpleName(), ex);
        }
    }
}
