package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.cotacoes.CotacoesOverviewDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Repository
class CotacoesDashboardSqlRepository {

    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String STATUS_NORMALIZADO_SQL =
            "LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status Conversão]))))";
    private static final String STATUS_CONVERTIDA_SQL =
            "status_normalizado IN (N'convertida', N'convertido')";
    private static final String STATUS_REPROVADA_SQL =
            "status_normalizado IN (N'reprovada', N'reprovado', N'perdida', N'perdido')";

    private final NamedParameterJdbcOperations jdbcTemplate;
    private final DashboardExportSqlBuilder sqlBuilder;
    private final EscopoFilialService escopoFilialService;

    CotacoesDashboardSqlRepository(
            NamedParameterJdbcOperations jdbcTemplate,
            DashboardExportSqlBuilder sqlBuilder,
            EscopoFilialService escopoFilialService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlBuilder = sqlBuilder;
        this.escopoFilialService = escopoFilialService;
    }

    CotacoesOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        DashboardExportSqlBuilder.ExportSql source = sqlBuilder.buildFilteredSource(
                DashboardExportDefinition.COTACOES,
                filtro,
                escopoFilialService.escopoAtual(),
                Set.of()
        );

        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                ),
                base_metricas AS (
                    SELECT
                        COALESCE(TRY_CONVERT(DECIMAL(19,4), [Valor frete]), 0) AS valor_frete,
                        COALESCE(TRY_CONVERT(DECIMAL(19,4), [Peso taxado]), 0) AS peso_taxado,
                        TRY_CONVERT(datetimeoffset, [Data Cotação]) AS data_cotacao,
                        TRY_CONVERT(datetimeoffset, [CT-e/Data de emissão]) AS cte_emissao,
                        TRY_CONVERT(datetimeoffset, [Nfse/Data de emissão]) AS nfse_emissao,
                        TRY_CONVERT(datetime2, [Data de extracao]) AS data_extracao,
                        %s AS status_normalizado
                    FROM base_filtrada
                ),
                agregado AS (
                    SELECT
                        MAX(data_extracao) AS updated_at,
                        COUNT(1) AS total_cotacoes,
                        SUM(valor_frete) AS valor_potencial,
                        SUM(CASE WHEN %s THEN valor_frete ELSE 0 END) AS valor_convertido,
                        SUM(CASE WHEN %s THEN 1 ELSE 0 END) AS convertidas,
                        SUM(CASE WHEN %s THEN 1 ELSE 0 END) AS reprovadas,
                        SUM(CASE WHEN peso_taxado > 0 THEN peso_taxado ELSE 0 END) AS peso_taxado_valido,
                        SUM(CASE WHEN peso_taxado > 0 THEN valor_frete ELSE 0 END) AS valor_frete_peso,
                        SUM(CASE WHEN %s AND cte_emissao IS NOT NULL THEN 1 ELSE 0 END) AS convertidas_com_cte,
                        SUM(CASE WHEN nfse_emissao IS NOT NULL THEN 1 ELSE 0 END) AS cotacoes_com_nfse,
                        AVG(CASE
                            WHEN cte_emissao IS NOT NULL AND data_cotacao IS NOT NULL
                            THEN CAST(DATEDIFF(HOUR, data_cotacao, cte_emissao) AS FLOAT)
                        END) AS tempo_medio_conversao_horas
                    FROM base_metricas
                )
                SELECT
                    updated_at,
                    total_cotacoes,
                    CAST(COALESCE(valor_potencial, 0) AS DECIMAL(19,2)) AS valor_potencial,
                    CAST(COALESCE(valor_convertido, 0) AS DECIMAL(19,2)) AS valor_convertido,
                    CAST(COALESCE(valor_potencial / NULLIF(total_cotacoes, 0), 0) AS DECIMAL(19,2)) AS frete_medio,
                    CAST(COALESCE(valor_frete_peso / NULLIF(peso_taxado_valido, 0), 0) AS DECIMAL(19,2)) AS frete_kg_medio,
                    CAST(COALESCE(valor_convertido * 100.0 / NULLIF(valor_potencial, 0), 0) AS DECIMAL(19,2)) AS conversao_valor,
                    CAST(COALESCE(CAST(convertidas AS FLOAT) * 100.0 / NULLIF(total_cotacoes, 0), 0) AS DECIMAL(19,2)) AS conversao_quantidade,
                    CAST(COALESCE(CAST(reprovadas AS FLOAT) * 100.0 / NULLIF(total_cotacoes, 0), 0) AS DECIMAL(19,2)) AS reprovacao_percentual,
                    CAST(COALESCE(CAST(convertidas_com_cte AS FLOAT) * 100.0 / NULLIF(total_cotacoes, 0), 0) AS DECIMAL(19,2)) AS taxa_conversao_cte,
                    CAST(COALESCE(CAST(cotacoes_com_nfse AS FLOAT) * 100.0 / NULLIF(total_cotacoes, 0), 0) AS DECIMAL(19,2)) AS taxa_conversao_nfse,
                    CAST(COALESCE(tempo_medio_conversao_horas, 0) AS DECIMAL(19,2)) AS tempo_medio_conversao_horas
                FROM agregado
                """.formatted(
                source.sql(),
                STATUS_NORMALIZADO_SQL,
                STATUS_CONVERTIDA_SQL,
                STATUS_CONVERTIDA_SQL,
                STATUS_REPROVADA_SQL,
                STATUS_CONVERTIDA_SQL
        );

        return jdbcTemplate.queryForObject(sql, copiarParams(source), (rs, rowNum) -> new CotacoesOverviewDTO(
                updatedAt(rs.getTimestamp("updated_at")),
                rs.getInt("total_cotacoes"),
                decimal(rs.getBigDecimal("valor_potencial")),
                decimal(rs.getBigDecimal("valor_convertido")),
                decimal(rs.getBigDecimal("frete_medio")),
                decimal(rs.getBigDecimal("frete_kg_medio")),
                percentual(rs.getBigDecimal("conversao_valor")),
                percentual(rs.getBigDecimal("conversao_quantidade")),
                percentual(rs.getBigDecimal("reprovacao_percentual")),
                percentual(rs.getBigDecimal("taxa_conversao_cte")),
                percentual(rs.getBigDecimal("taxa_conversao_nfse")),
                percentual(rs.getBigDecimal("tempo_medio_conversao_horas"))
        ));
    }

    private MapSqlParameterSource copiarParams(DashboardExportSqlBuilder.ExportSql source) {
        return new MapSqlParameterSource(source.params().getValues());
    }

    private String updatedAt(Timestamp timestamp) {
        LocalDateTime valor = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();
        return valor.format(ISO_LOCAL_DATE_TIME);
    }

    private BigDecimal decimal(BigDecimal valor) {
        return (valor != null ? valor : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private double percentual(BigDecimal valor) {
        return decimal(valor).doubleValue();
    }
}
