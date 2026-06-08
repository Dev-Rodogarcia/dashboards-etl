package com.dashboard.api.repository;

import com.dashboard.api.builder.DashboardExportSqlBuilder;
import com.dashboard.api.definition.DashboardExportDefinition;
import com.dashboard.api.dto.coletas.ColetasAgingBucketDTO;
import com.dashboard.api.dto.coletas.ColetasCidadeOrigemDTO;
import com.dashboard.api.dto.coletas.ColetasOverviewDTO;
import com.dashboard.api.dto.coletas.ColetasRegiaoOrigemDTO;
import com.dashboard.api.dto.coletas.ColetasSlaPorFilialDTO;
import com.dashboard.api.dto.coletas.ColetasStatusDistribuicaoDTO;
import com.dashboard.api.dto.coletas.ColetasTrendPointDTO;
import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.model.coletas.ColetaStatusPendente;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.TemporalJsonUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class ColetasAgregadosSqlRepository {

    private static final String REGIAO_SEM_MAPEAMENTO = "Sem regiao";
    private static final String CIDADE_SEM_MAPEAMENTO = "Sem cidade";
    private static final List<String> ORDEM_AGING = List.of("0-2 dias", "3-5 dias", "6-10 dias", "11+ dias");

    private final NamedParameterJdbcOperations jdbcTemplate;
    private final DashboardExportSqlBuilder sqlBuilder;
    private final EscopoFilialService escopoFilialService;

    public ColetasAgregadosSqlRepository(
            NamedParameterJdbcOperations jdbcTemplate,
            DashboardExportSqlBuilder sqlBuilder,
            EscopoFilialService escopoFilialService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlBuilder = sqlBuilder;
        this.escopoFilialService = escopoFilialService;
    }

    public ColetasOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        String sql = baseDeduplicadaSql(source) + """
                , metricas AS (
                    SELECT
                        MAX([Data de extracao]) AS updated_at,
                        COUNT(1) AS total_coletas,
                        SUM(CASE WHEN status_normalizado IN (N'finalizada', N'coletada') THEN 1 ELSE 0 END) AS finalizadas,
                        SUM(CASE WHEN status_normalizado = N'cancelada' THEN 1 ELSE 0 END) AS canceladas,
                        SUM(CASE
                            WHEN status_normalizado IN (N'finalizada', N'coletada')
                             AND [Finalizacao] IS NOT NULL
                             AND [Agendamento] IS NOT NULL
                             AND [Finalizacao] <= [Agendamento]
                            THEN 1 ELSE 0
                        END) AS finalizadas_dentro_sla,
                        AVG(CASE
                            WHEN status_normalizado IN (N'finalizada', N'coletada')
                             AND [Solicitacao] IS NOT NULL
                             AND [Finalizacao] IS NOT NULL
                            THEN CAST(DATEDIFF(day, [Solicitacao], [Finalizacao]) AS FLOAT)
                        END) AS lead_time_medio_dias,
                        SUM(COALESCE([Peso Taxado], 0)) AS peso_taxado_total,
                        SUM(COALESCE([Valor NF], 0)) AS valor_nf_total
                    FROM base_metricas
                )
                SELECT
                    updated_at,
                    total_coletas,
                    finalizadas,
                    CAST(COALESCE(CAST(finalizadas AS FLOAT) * 100.0 / NULLIF(total_coletas, 0), 0) AS DECIMAL(19,2)) AS taxa_sucesso,
                    CAST(COALESCE(CAST(canceladas AS FLOAT) * 100.0 / NULLIF(total_coletas, 0), 0) AS DECIMAL(19,2)) AS taxa_cancelamento,
                    CAST(COALESCE(CAST(finalizadas_dentro_sla AS FLOAT) * 100.0 / NULLIF(finalizadas, 0), 0) AS DECIMAL(19,2)) AS sla_no_agendamento,
                    CAST(COALESCE(lead_time_medio_dias, 0) AS DECIMAL(19,2)) AS lead_time_medio_dias,
                    CAST(0 AS DECIMAL(19,2)) AS tentativas_medias,
                    CAST(COALESCE(peso_taxado_total, 0) AS DECIMAL(19,2)) AS peso_taxado_total,
                    CAST(COALESCE(valor_nf_total, 0) AS DECIMAL(19,2)) AS valor_nf_total
                FROM metricas
                """;

        return jdbcTemplate.queryForObject(sql, copiarParams(source), (rs, rowNum) -> new ColetasOverviewDTO(
                updatedAt(rs.getTimestamp("updated_at")),
                rs.getInt("total_coletas"),
                rs.getInt("finalizadas"),
                percentual(rs.getBigDecimal("taxa_sucesso")),
                percentual(rs.getBigDecimal("taxa_cancelamento")),
                percentual(rs.getBigDecimal("sla_no_agendamento")),
                percentual(rs.getBigDecimal("lead_time_medio_dias")),
                percentual(rs.getBigDecimal("tentativas_medias")),
                decimal(rs.getBigDecimal("peso_taxado_total")),
                decimal(rs.getBigDecimal("valor_nf_total"))
        ));
    }

    public List<ColetasTrendPointDTO> buscarSerieTemporal(FiltroConsultaDTO filtro) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                ),
                base_deduplicada AS (
                    SELECT *
                    FROM (
                        SELECT
                            base_filtrada.*,
                            ROW_NUMBER() OVER (
                                PARTITION BY [ID]
                                ORDER BY [Data de extracao] DESC, [Solicitacao] DESC, [Numero Manifesto] DESC
                            ) AS [__rn]
                        FROM base_filtrada
                    ) dedup
                    WHERE [__rn] = 1
                ),
                base_metricas AS (
                    SELECT
                        [Solicitacao] AS data_solicitacao,
                        LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status])))) AS status_normalizado
                    FROM base_deduplicada
                    WHERE [Solicitacao] IS NOT NULL
                )
                SELECT
                    data_solicitacao,
                    COUNT(1) AS total_coletas,
                    SUM(CASE WHEN status_normalizado IN (N'finalizada', N'coletada') THEN 1 ELSE 0 END) AS finalizadas,
                    SUM(CASE WHEN status_normalizado = N'cancelada' THEN 1 ELSE 0 END) AS canceladas,
                    SUM(CASE WHEN status_normalizado = N'em tratativa' THEN 1 ELSE 0 END) AS em_tratativa
                FROM base_metricas
                GROUP BY data_solicitacao
                ORDER BY data_solicitacao
                """.formatted(source.sql());

        return jdbcTemplate.query(sql, copiarParams(source), (rs, rowNum) -> new ColetasTrendPointDTO(
                data(rs.getDate("data_solicitacao")),
                rs.getInt("total_coletas"),
                rs.getInt("finalizadas"),
                rs.getInt("canceladas"),
                rs.getInt("em_tratativa")
        ));
    }

    public List<ColetasStatusDistribuicaoDTO> buscarStatusDistribuicao(FiltroConsultaDTO filtro) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        String statusSql = "COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status]))), N''), N'Sem status')";
        String sql = baseDeduplicadaSql(source) + """
                SELECT
                    %s AS status,
                    COUNT(1) AS total
                FROM base_metricas
                GROUP BY %s
                ORDER BY total DESC, status
                """.formatted(statusSql, statusSql);

        return jdbcTemplate.query(sql, copiarParams(source), (rs, rowNum) -> new ColetasStatusDistribuicaoDTO(
                rs.getString("status"),
                rs.getInt("total")
        ));
    }

    public List<ColetasSlaPorFilialDTO> buscarSlaPorFilial(FiltroConsultaDTO filtro) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        String filialSql = "NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), N'')";
        String sql = baseDeduplicadaSql(source) + """
                SELECT TOP (8)
                    %s AS filial,
                    CAST(COALESCE(
                        SUM(CASE
                            WHEN [Finalizacao] IS NOT NULL
                             AND [Agendamento] IS NOT NULL
                             AND [Finalizacao] <= [Agendamento]
                            THEN 1 ELSE 0
                        END) * 100.0 / NULLIF(COUNT(1), 0),
                        0
                    ) AS DECIMAL(19,2)) AS sla_pct,
                    COUNT(1) AS total
                FROM base_metricas
                WHERE status_normalizado IN (N'finalizada', N'coletada')
                  AND %s IS NOT NULL
                GROUP BY %s
                ORDER BY sla_pct DESC, filial
                """.formatted(filialSql, filialSql, filialSql);

        return jdbcTemplate.query(sql, copiarParams(source), (rs, rowNum) -> new ColetasSlaPorFilialDTO(
                rs.getString("filial"),
                percentual(rs.getBigDecimal("sla_pct")),
                rs.getInt("total")
        ));
    }

    public List<ColetasRegiaoOrigemDTO> buscarRegioesOrigem(FiltroConsultaDTO filtro) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        String regiaoSql = regiaoSql();
        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                ),
                base_deduplicada AS (
                    SELECT *
                    FROM (
                        SELECT
                            base_filtrada.*,
                            ROW_NUMBER() OVER (
                                PARTITION BY [ID]
                                ORDER BY [Data de extracao] DESC, [Solicitacao] DESC, [Numero Manifesto] DESC
                            ) AS [__rn]
                        FROM base_filtrada
                    ) dedup
                    WHERE [__rn] = 1
                )
                SELECT
                    %s AS regiao,
                    COUNT(DISTINCT [Coleta]) AS total_coletas,
                    SUM(COALESCE([Peso Taxado], 0)) AS peso_taxado
                FROM base_deduplicada
                GROUP BY %s
                ORDER BY total_coletas DESC, regiao ASC
                """.formatted(source.sql(), regiaoSql, regiaoSql);

        return jdbcTemplate.query(sql, copiarParams(source), (rs, rowNum) -> new ColetasRegiaoOrigemDTO(
                rs.getString("regiao"),
                rs.getInt("total_coletas"),
                zeroSeNulo(rs.getBigDecimal("peso_taxado"))
        ));
    }

    public List<ColetasCidadeOrigemDTO> buscarCidadesOrigem(FiltroConsultaDTO filtro, String regiao) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        MapSqlParameterSource params = copiarParams(source);
        params.addValue("regiaoSelecionada", regiao);

        String cidadeSql = cidadeSql();
        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                ),
                base_deduplicada AS (
                    SELECT *
                    FROM (
                        SELECT
                            base_filtrada.*,
                            ROW_NUMBER() OVER (
                                PARTITION BY [ID]
                                ORDER BY [Data de extracao] DESC, [Solicitacao] DESC, [Numero Manifesto] DESC
                            ) AS [__rn]
                        FROM base_filtrada
                    ) dedup
                    WHERE [__rn] = 1
                )
                SELECT
                    %s AS cidade,
                    COUNT(DISTINCT [Coleta]) AS total_coletas,
                    SUM(COALESCE([Peso Taxado], 0)) AS peso_taxado
                FROM base_deduplicada
                WHERE (
                    (:regiaoSelecionada = N'%s' AND ([Região da Coleta] IS NULL OR LTRIM(RTRIM([Região da Coleta])) = N''))
                    OR [Região da Coleta] = :regiaoSelecionada
                )
                GROUP BY %s
                ORDER BY total_coletas DESC, cidade ASC
                """.formatted(source.sql(), cidadeSql, REGIAO_SEM_MAPEAMENTO, cidadeSql);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new ColetasCidadeOrigemDTO(
                rs.getString("cidade"),
                rs.getInt("total_coletas"),
                zeroSeNulo(rs.getBigDecimal("peso_taxado"))
        ));
    }

    public List<ColetasAgingBucketDTO> buscarAgingAbertas(FiltroConsultaDTO filtro, LocalDate dataReferencia) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        MapSqlParameterSource params = copiarParams(source);
        params.addValue("dataReferencia", dataReferencia);
        params.addValue("statusPendentes", ColetaStatusPendente.normalizados());

        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                ),
                base_deduplicada AS (
                    SELECT *
                    FROM (
                        SELECT
                            base_filtrada.*,
                            ROW_NUMBER() OVER (
                                PARTITION BY [ID]
                                ORDER BY [Data de extracao] DESC, [Solicitacao] DESC, [Numero Manifesto] DESC
                            ) AS [__rn]
                        FROM base_filtrada
                    ) dedup
                    WHERE [__rn] = 1
                ),
                aging AS (
                    SELECT
                        CASE
                            WHEN DATEDIFF(day, [Solicitacao], :dataReferencia) <= 2 THEN N'0-2 dias'
                            WHEN DATEDIFF(day, [Solicitacao], :dataReferencia) <= 5 THEN N'3-5 dias'
                            WHEN DATEDIFF(day, [Solicitacao], :dataReferencia) <= 10 THEN N'6-10 dias'
                            ELSE N'11+ dias'
                        END AS faixa,
                        [Coleta]
                    FROM base_deduplicada
                    WHERE [Solicitacao] IS NOT NULL
                      AND LOWER(LTRIM(RTRIM([Status]))) IN (:statusPendentes)
                )
                SELECT faixa, COUNT(DISTINCT [Coleta]) AS total
                FROM aging
                GROUP BY faixa
                """.formatted(source.sql());

        Map<String, Integer> totais = new LinkedHashMap<>();
        jdbcTemplate.query(sql, params, rs -> {
            totais.put(rs.getString("faixa"), rs.getInt("total"));
        });

        return ORDEM_AGING.stream()
                .map(faixa -> new ColetasAgingBucketDTO(faixa, totais.getOrDefault(faixa, 0)))
                .toList();
    }

    private DashboardExportSqlBuilder.ExportSql source(FiltroConsultaDTO filtro) {
        return sqlBuilder.buildFilteredSource(
                DashboardExportDefinition.COLETAS,
                filtro,
                escopoFilialService.escopoAtual(),
                Set.of()
        );
    }

    private String baseDeduplicadaSql(DashboardExportSqlBuilder.ExportSql source) {
        return """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                ),
                base_deduplicada AS (
                    SELECT *
                    FROM (
                        SELECT
                            base_filtrada.*,
                            ROW_NUMBER() OVER (
                                PARTITION BY [ID]
                                ORDER BY [Data de extracao] DESC, [Solicitacao] DESC, [Numero Manifesto] DESC
                            ) AS [__rn]
                        FROM base_filtrada
                    ) dedup
                    WHERE [__rn] = 1
                ),
                base_metricas AS (
                    SELECT
                        base_deduplicada.*,
                        LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status])))) AS status_normalizado
                    FROM base_deduplicada
                )
                """.formatted(source.sql());
    }

    private MapSqlParameterSource copiarParams(DashboardExportSqlBuilder.ExportSql source) {
        return new MapSqlParameterSource(source.params().getValues());
    }

    private String regiaoSql() {
        return "COALESCE(NULLIF(LTRIM(RTRIM([Região da Coleta])), N''), N'" + REGIAO_SEM_MAPEAMENTO + "')";
    }

    private String cidadeSql() {
        return "COALESCE(NULLIF(LTRIM(RTRIM([Cidade])), N''), N'" + CIDADE_SEM_MAPEAMENTO + "')";
    }

    private BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private BigDecimal decimal(BigDecimal valor) {
        return zeroSeNulo(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private double percentual(BigDecimal valor) {
        return decimal(valor).doubleValue();
    }

    private String updatedAt(Timestamp timestamp) {
        LocalDateTime valor = timestamp != null ? timestamp.toLocalDateTime() : null;
        return TemporalJsonUtils.formatarUtc(valor);
    }

    private String data(Date data) {
        return data != null ? data.toLocalDate().toString() : null;
    }
}
