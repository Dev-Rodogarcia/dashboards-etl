package com.dashboard.api.service;

import com.dashboard.api.builder.DashboardExportSqlBuilder;
import com.dashboard.api.dto.coletas.ColetasOverviewDTO;
import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.repository.ColetasAgregadosSqlRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.PeriodoOffsetDateTimeHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColetasAgregadosSqlRepositoryTest {

    @Mock
    private NamedParameterJdbcOperations jdbcTemplate;

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void buscarOverviewDeveDeduplicarEAgregarNoSql() {
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(new ColetasOverviewDTO(
                        "2026-04-30T12:00:00",
                        0,
                        0,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ));

        ColetasAgregadosSqlRepository repository = new ColetasAgregadosSqlRepository(
                jdbcTemplate,
                new DashboardExportSqlBuilder(PeriodoOffsetDateTimeHelper.padrao()),
                escopoSemRestricao()
        );

        repository.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));

        assertThat(sqlCaptor.getValue())
                .contains("ROW_NUMBER() OVER")
                .contains("PARTITION BY [ID]")
                .contains("COUNT(1) AS total_coletas")
                .contains("SUM(CASE WHEN status_normalizado IN (N'finalizada', N'coletada')")
                .contains("AVG(CASE")
                .contains("[Solicitacao] >= :dataInicio AND [Solicitacao] < :dataFimExclusivo")
                .doesNotContain("TRY_CONVERT(date, [Solicitacao])");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void buscarRegioesOrigemDeveAgregarColetasEPesoTaxado() {
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        ColetasAgregadosSqlRepository repository = new ColetasAgregadosSqlRepository(
                jdbcTemplate,
                new DashboardExportSqlBuilder(PeriodoOffsetDateTimeHelper.padrao()),
                escopoSemRestricao()
        );

        repository.buscarRegioesOrigem(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));

        assertThat(sqlCaptor.getValue())
                .contains("COUNT(DISTINCT [Coleta]) AS total_coletas")
                .contains("SUM(COALESCE([Peso Taxado], 0)) AS peso_taxado")
                .contains("GROUP BY COALESCE(NULLIF(LTRIM(RTRIM([Região da Coleta]))");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void buscarStatusDistribuicaoDeveAgruparStatusNoSql() {
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        ColetasAgregadosSqlRepository repository = new ColetasAgregadosSqlRepository(
                jdbcTemplate,
                new DashboardExportSqlBuilder(PeriodoOffsetDateTimeHelper.padrao()),
                escopoSemRestricao()
        );

        repository.buscarStatusDistribuicao(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));

        assertThat(sqlCaptor.getValue())
                .contains("COUNT(1) AS total")
                .contains("GROUP BY COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status])))")
                .contains("ROW_NUMBER() OVER")
                .doesNotContain("TRY_CONVERT(date, [Solicitacao])");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void buscarSlaPorFilialDeveAgruparFinalizadasPorFilialNoSql() {
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        ColetasAgregadosSqlRepository repository = new ColetasAgregadosSqlRepository(
                jdbcTemplate,
                new DashboardExportSqlBuilder(PeriodoOffsetDateTimeHelper.padrao()),
                escopoSemRestricao()
        );

        repository.buscarSlaPorFilial(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));

        assertThat(sqlCaptor.getValue())
                .contains("SELECT TOP (8)")
                .contains("WHERE status_normalizado IN (N'finalizada', N'coletada')")
                .contains("GROUP BY NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), N'')")
                .contains("ORDER BY sla_pct DESC, filial")
                .doesNotContain("TRY_CONVERT(date, [Solicitacao])");
    }

    @Test
    void buscarAgingDeveUsarDataReferenciaParametrizadaEStatusPendentesParametrizados() {
        ColetasAgregadosSqlRepository repository = new ColetasAgregadosSqlRepository(
                jdbcTemplate,
                new DashboardExportSqlBuilder(PeriodoOffsetDateTimeHelper.padrao()),
                escopoSemRestricao()
        );

        repository.buscarAgingAbertas(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of()),
                LocalDate.of(2026, 5, 23)
        );

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowCallbackHandler.class));

        assertThat(sqlCaptor.getValue())
                .contains("DATEDIFF(day, [Solicitacao], :dataReferencia)")
                .contains("IN (:statusPendentes)")
                .doesNotContain("GETDATE()")
                .doesNotContain("TRY_CONVERT(date, [Solicitacao])");
        assertThat(paramsCaptor.getValue().getValue("dataReferencia")).isEqualTo(LocalDate.of(2026, 5, 23));
        assertThat(paramsCaptor.getValue().getValue("statusPendentes")).isEqualTo(List.of("pendente", "em aberto"));
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
