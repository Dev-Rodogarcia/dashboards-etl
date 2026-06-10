package com.dashboard.api.repository;

import com.dashboard.api.builder.DashboardExportSqlBuilder;
import com.dashboard.api.definition.DashboardExportDefinition;
import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.etl.EtlCategoriaErroDTO;
import com.dashboard.api.dto.etl.EtlExecucaoTrendPointDTO;
import com.dashboard.api.dto.etl.EtlLogExtracaoAuditoriaDTO;
import com.dashboard.api.dto.etl.EtlSaudeChartsDTO;
import com.dashboard.api.dto.etl.EtlSaudeOverviewDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class EtlSaudeSqlRepository {

    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final ZoneId ZONE_ID_APLICACAO = ZoneId.of("America/Sao_Paulo");
    private static final String STATUS_NORMALIZADO_SQL =
            "COALESCE(NULLIF(LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status])))), N''), N'')";
    private static final String STATUS_SUCESSO_SQL = STATUS_NORMALIZADO_SQL + " = N'success'";
    private static final String STATUS_ERRO_SQL = STATUS_NORMALIZADO_SQL + " <> N'success'";
    private static final String DURACAO_SEGUNDOS_SQL = "COALESCE(TRY_CONVERT(INT, [Duracao (s)]), 0)";
    private static final String TOTAL_REGISTROS_SQL = "COALESCE(TRY_CONVERT(INT, [Total Registros]), 0)";

    private final NamedParameterJdbcOperations jdbcTemplate;
    private final DashboardExportSqlBuilder sqlBuilder;

    public EtlSaudeSqlRepository(
            NamedParameterJdbcOperations jdbcTemplate,
            DashboardExportSqlBuilder sqlBuilder
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlBuilder = sqlBuilder;
    }

    public EtlSaudeOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                ),
                agregado AS (
                    SELECT
                        CAST(NULL AS DATETIME2) AS updated_at,
                        COUNT(1) AS total_execucoes,
                        AVG(CAST(%s AS FLOAT)) AS tempo_medio_execucao_segundos,
                        SUM(CASE WHEN %s THEN 1 ELSE 0 END) AS execucoes_com_erro,
                        SUM(%s) AS volume_processado_total,
                        SUM(CASE WHEN %s THEN 1 ELSE 0 END) AS execucoes_sucesso
                    FROM base_filtrada
                )
                SELECT
                    updated_at,
                    CAST(COALESCE(tempo_medio_execucao_segundos, 0) AS DECIMAL(19,2)) AS tempo_medio_execucao_segundos,
                    COALESCE(execucoes_com_erro, 0) AS execucoes_com_erro,
                    total_execucoes,
                    COALESCE(volume_processado_total, 0) AS volume_processado_total,
                    CAST(COALESCE(CAST(execucoes_sucesso AS FLOAT) * 100.0 / NULLIF(total_execucoes, 0), 0) AS DECIMAL(19,2)) AS taxa_sucesso
                FROM agregado
                """.formatted(
                source.sql(),
                DURACAO_SEGUNDOS_SQL,
                STATUS_ERRO_SQL,
                TOTAL_REGISTROS_SQL,
                STATUS_SUCESSO_SQL
        );

        return jdbcTemplate.queryForObject(sql, copiarParams(source), (rs, rowNum) -> new EtlSaudeOverviewDTO(
                updatedAt(rs.getTimestamp("updated_at")),
                decimalDouble(rs.getBigDecimal("tempo_medio_execucao_segundos")),
                rs.getInt("execucoes_com_erro"),
                rs.getInt("total_execucoes"),
                rs.getInt("volume_processado_total"),
                decimalDouble(rs.getBigDecimal("taxa_sucesso"))
        ));
    }

    public List<EtlExecucaoTrendPointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                ),
                base_metricas AS (
                    SELECT
                        [Data] AS data_execucao,
                        %s AS duracao_segundos,
                        %s AS total_registros,
                        %s AS status_normalizado
                    FROM base_filtrada
                    WHERE [Data] IS NOT NULL
                )
                SELECT
                    data_execucao,
                    COUNT(1) AS execucoes,
                    SUM(CASE WHEN status_normalizado <> N'success' THEN 1 ELSE 0 END) AS erros,
                    SUM(total_registros) AS volume_processado,
                    CAST(COALESCE(AVG(CAST(duracao_segundos AS FLOAT)), 0) AS DECIMAL(19,2)) AS duracao_media
                FROM base_metricas
                GROUP BY data_execucao
                ORDER BY data_execucao
                """.formatted(
                source.sql(),
                DURACAO_SEGUNDOS_SQL,
                TOTAL_REGISTROS_SQL,
                STATUS_NORMALIZADO_SQL
        );

        return jdbcTemplate.query(sql, copiarParams(source), (rs, rowNum) -> new EtlExecucaoTrendPointDTO(
                data(rs.getDate("data_execucao")),
                rs.getInt("execucoes"),
                rs.getInt("erros"),
                rs.getInt("volume_processado"),
                decimalDouble(rs.getBigDecimal("duracao_media"))
        ));
    }

    public List<EtlLogExtracaoAuditoriaDTO> buscarTabela(FiltroConsultaDTO filtro) {
        String sql = """
                SELECT TOP (5000)
                    log.id,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), log.entidade))), N'') AS entidade,
                    log.timestamp_inicio,
                    log.timestamp_fim,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(40), log.status_final))), N'') AS status_final,
                    log.registros_extraidos,
                    log.paginas_processadas,
                    log.noop_count,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), log.mensagem))), N'') AS mensagem
                FROM dbo.log_extracoes log
                WHERE log.timestamp_fim >= :dataInicio
                  AND log.timestamp_fim < :dataFimExclusivo
                ORDER BY log.timestamp_fim DESC, log.id DESC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("dataInicio", filtro.dataInicio())
                .addValue("dataFimExclusivo", filtro.dataFim().plusDays(1));
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new EtlLogExtracaoAuditoriaDTO(
                rs.getLong("id"),
                rs.getString("entidade"),
                localDateTime(rs.getTimestamp("timestamp_inicio")),
                localDateTime(rs.getTimestamp("timestamp_fim")),
                rs.getString("status_final"),
                inteiro(rs, "registros_extraidos"),
                inteiro(rs, "paginas_processadas"),
                inteiro(rs, "noop_count"),
                rs.getString("mensagem")
        ));
    }

    public long totalTabela(FiltroConsultaDTO filtro) {
        String sql = """
                SELECT COUNT(1)
                FROM dbo.log_extracoes log
                WHERE log.timestamp_fim >= :dataInicio
                  AND log.timestamp_fim < :dataFimExclusivo
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("dataInicio", filtro.dataInicio())
                .addValue("dataFimExclusivo", filtro.dataFim().plusDays(1));
        Long total = jdbcTemplate.queryForObject(sql, params, Long.class);
        return total == null ? 0 : total;
    }

    public EtlSaudeChartsDTO buscarGraficos(FiltroConsultaDTO filtro) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        String categoriaSql = "NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Categoria Erro]))), N'')";
        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                ),
                base_metricas AS (
                    SELECT
                        %s AS categoria
                    FROM base_filtrada
                )
                SELECT
                    categoria,
                    COUNT(1) AS total
                FROM base_metricas
                WHERE categoria IS NOT NULL
                GROUP BY categoria
                ORDER BY total DESC, categoria
                """.formatted(source.sql(), categoriaSql);

        List<EtlCategoriaErroDTO> categoriasErro = jdbcTemplate.query(sql, copiarParams(source), (rs, rowNum) ->
                new EtlCategoriaErroDTO(rs.getString("categoria"), rs.getInt("total"))
        );
        return new EtlSaudeChartsDTO(categoriasErro);
    }

    private DashboardExportSqlBuilder.ExportSql source(FiltroConsultaDTO filtro) {
        return sqlBuilder.buildFilteredSource(
                DashboardExportDefinition.ETL_SAUDE,
                filtro,
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );
    }

    private MapSqlParameterSource copiarParams(DashboardExportSqlBuilder.ExportSql source) {
        return new MapSqlParameterSource(source.params().getValues());
    }

    private String updatedAt(Timestamp timestamp) {
        LocalDateTime valor = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now(ZONE_ID_APLICACAO);
        return valor.format(ISO_LOCAL_DATE_TIME);
    }

    private double decimalDouble(BigDecimal valor) {
        return (valor != null ? valor : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String data(Date data) {
        return data != null ? data.toLocalDate().toString() : null;
    }

    private String timestamp(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime().toString() : null;
    }

    private LocalDateTime localDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private Integer inteiro(ResultSet rs, String coluna) throws SQLException {
        int valor = rs.getInt(coluna);
        return rs.wasNull() ? null : valor;
    }
}
