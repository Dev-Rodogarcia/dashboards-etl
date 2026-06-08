package com.dashboard.api.service;

import com.dashboard.api.builder.DashboardExportSqlBuilder;
import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.etl.EtlSaudeOverviewDTO;
import com.dashboard.api.repository.EtlSaudeSqlRepository;
import com.dashboard.api.util.PeriodoOffsetDateTimeHelper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EtlSaudeSqlRepositoryTest {

    @Mock
    private NamedParameterJdbcOperations jdbcTemplate;

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void buscarOverviewDeveAgregarMetricasNoSqlServer() {
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(new EtlSaudeOverviewDTO("2026-03-23T09:00:00", 0.0, 0, 0, 0, 0.0));

        repository().buscarOverview(filtroPadrao());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));

        assertThat(sqlCaptor.getValue())
                .contains("FROM [vw_bi_monitoramento] base")
                .contains("[Data] >= :dataInicio AND [Data] < :dataFimExclusivo")
                .contains("COUNT(1) AS total_execucoes")
                .contains("AVG(CAST(COALESCE(TRY_CONVERT(INT, [Duracao (s)]), 0) AS FLOAT)) AS tempo_medio_execucao_segundos")
                .contains("SUM(CASE WHEN COALESCE(NULLIF(LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status])))), N''), N'') <> N'success' THEN 1 ELSE 0 END) AS execucoes_com_erro")
                .contains("SUM(COALESCE(TRY_CONVERT(INT, [Total Registros]), 0)) AS volume_processado_total")
                .contains("SUM(CASE WHEN COALESCE(NULLIF(LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status])))), N''), N'') = N'success' THEN 1 ELSE 0 END) AS execucoes_sucesso")
                .contains("CAST(NULL AS DATETIME2) AS updated_at")
                .doesNotContain("SYSDATETIME() AS updated_at");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void buscarSerieDeveAgruparPorDataNoSqlServer() {
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        repository().buscarSerie(filtroPadrao());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));

        assertThat(sqlCaptor.getValue())
                .contains("COUNT(1) AS execucoes")
                .contains("SUM(CASE WHEN status_normalizado <> N'success' THEN 1 ELSE 0 END) AS erros")
                .contains("SUM(total_registros) AS volume_processado")
                .contains("AVG(CAST(duracao_segundos AS FLOAT))")
                .contains("GROUP BY data_execucao")
                .contains("ORDER BY data_execucao");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void buscarGraficosDeveAgruparCategoriasErroNoSqlServer() {
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        repository().buscarGraficos(filtroPadrao());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));

        assertThat(sqlCaptor.getValue())
                .contains("NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Categoria Erro]))), N'') AS categoria")
                .contains("COUNT(1) AS total")
                .contains("GROUP BY categoria")
                .contains("ORDER BY total DESC, categoria");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void buscarTabelaDeveAplicarTravaSqlEmLogExtracoes() {
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        repository().buscarTabela(filtroPadrao());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));

        assertThat(sqlCaptor.getValue())
                .contains("SELECT TOP (5000)")
                .contains("log.timestamp_inicio")
                .contains("log.timestamp_fim")
                .contains("log.status_final")
                .contains("log.registros_extraidos")
                .contains("log.paginas_processadas")
                .contains("log.noop_count")
                .contains("log.mensagem")
                .contains("FROM [ETL_SISTEMA].dbo.log_extracoes log")
                .contains("log.timestamp_fim >= :dataInicio")
                .contains("log.timestamp_fim < :dataFimExclusivo")
                .contains("ORDER BY log.timestamp_fim DESC, log.id DESC");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void totalTabelaDeveContarLogExtracoes() {
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(Class.class)))
                .thenReturn(123L);

        long total = repository().totalTabela(filtroPadrao());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(Class.class));

        assertThat(total).isEqualTo(123L);
        assertThat(sqlCaptor.getValue())
                .contains("SELECT COUNT(1)")
                .contains("FROM [ETL_SISTEMA].dbo.log_extracoes log")
                .contains("log.timestamp_fim >= :dataInicio")
                .contains("log.timestamp_fim < :dataFimExclusivo");
    }

    private EtlSaudeSqlRepository repository() {
        return new EtlSaudeSqlRepository(
                jdbcTemplate,
                new DashboardExportSqlBuilder(PeriodoOffsetDateTimeHelper.padrao())
        );
    }

    private static FiltroConsultaDTO filtroPadrao() {
        return new FiltroConsultaDTO(LocalDate.of(2026, 2, 21), LocalDate.of(2026, 3, 23), Map.of());
    }
}
