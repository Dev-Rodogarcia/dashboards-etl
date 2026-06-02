package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.coletas.ColetasAgingBucketDTO;
import com.dashboard.api.dto.coletas.ColetasCidadeOrigemDTO;
import com.dashboard.api.dto.coletas.ColetasRegiaoOrigemDTO;
import com.dashboard.api.dto.coletas.ColetasTrendPointDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
class ColetasAgregadosSqlRepository {

    private static final String REGIAO_SEM_MAPEAMENTO = "Sem regiao";
    private static final String CIDADE_SEM_MAPEAMENTO = "Sem cidade";
    private static final List<String> ORDEM_AGING = List.of("0-2 dias", "3-5 dias", "6-10 dias", "11+ dias");

    private final NamedParameterJdbcOperations jdbcTemplate;
    private final DashboardExportSqlBuilder sqlBuilder;
    private final EscopoFilialService escopoFilialService;

    ColetasAgregadosSqlRepository(
            NamedParameterJdbcOperations jdbcTemplate,
            DashboardExportSqlBuilder sqlBuilder,
            EscopoFilialService escopoFilialService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlBuilder = sqlBuilder;
        this.escopoFilialService = escopoFilialService;
    }

    List<ColetasTrendPointDTO> buscarSerieTemporal(FiltroConsultaDTO filtro) {
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

    List<ColetasRegiaoOrigemDTO> buscarRegioesOrigem(FiltroConsultaDTO filtro) {
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

    List<ColetasCidadeOrigemDTO> buscarCidadesOrigem(FiltroConsultaDTO filtro, String regiao) {
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

    List<ColetasAgingBucketDTO> buscarAgingAbertas(FiltroConsultaDTO filtro, LocalDate dataReferencia) {
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

    private String data(Date data) {
        return data != null ? data.toLocalDate().toString() : null;
    }
}
