package com.dashboard.api.repository;

import com.dashboard.api.builder.DashboardExportSqlBuilder;
import com.dashboard.api.definition.DashboardExportDefinition;
import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.etl.EtlCategoriaErroDTO;
import com.dashboard.api.dto.etl.EtlExecucaoResumoDTO;
import com.dashboard.api.dto.etl.EtlExecucaoTrendPointDTO;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class EtlSaudeSqlRepository {

    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
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
                        SYSDATETIME() AS updated_at,
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

    public List<EtlExecucaoResumoDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        String sql = """
                SELECT
                    TRY_CONVERT(BIGINT, base.[Id]) AS id,
                    TRY_CONVERT(datetime2, CONVERT(NVARCHAR(64), base.[Inicio])) AS inicio,
                    TRY_CONVERT(datetime2, CONVERT(NVARCHAR(64), base.[Fim])) AS fim,
                    TRY_CONVERT(INT, base.[Duracao (s)]) AS duracao_segundos,
                    TRY_CONVERT(date, base.[Data]) AS data_execucao,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), base.[Status]))), N'') AS status,
                    TRY_CONVERT(INT, base.[Total Registros]) AS total_registros,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), base.[Categoria Erro]))), N'') AS categoria_erro,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), base.[Mensagem Erro]))), N'') AS mensagem_erro
                %s
                ORDER BY base.[Data] DESC, base.[Inicio] DESC
                OFFSET 0 ROWS FETCH NEXT :limite ROWS ONLY
                """.formatted(source.sql());

        MapSqlParameterSource params = copiarParams(source);
        params.addValue("limite", limite);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new EtlExecucaoResumoDTO(
                rs.getLong("id"),
                timestamp(rs.getTimestamp("inicio")),
                timestamp(rs.getTimestamp("fim")),
                inteiro(rs, "duracao_segundos"),
                data(rs.getDate("data_execucao")),
                rs.getString("status"),
                inteiro(rs, "total_registros"),
                rs.getString("categoria_erro"),
                rs.getString("mensagem_erro")
        ));
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
        LocalDateTime valor = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();
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

    private Integer inteiro(ResultSet rs, String coluna) throws SQLException {
        int valor = rs.getInt(coluna);
        return rs.wasNull() ? null : valor;
    }
}
