package com.dashboard.api.repository;

import com.dashboard.api.builder.DashboardExportSqlBuilder;
import com.dashboard.api.definition.DashboardExportDefinition;
import com.dashboard.api.dto.contaspagar.ContasAPagarCentroCustoDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarChartsDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarConciliacaoDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarFornecedorDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarMensalTrendDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarOverviewDTO;
import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.TemporalJsonUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class ContasAPagarSqlRepository {

    private final NamedParameterJdbcOperations jdbcTemplate;
    private final DashboardExportSqlBuilder sqlBuilder;
    private final EscopoFilialService escopoFilialService;

    public ContasAPagarSqlRepository(
            NamedParameterJdbcOperations jdbcTemplate,
            DashboardExportSqlBuilder sqlBuilder,
            EscopoFilialService escopoFilialService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlBuilder = sqlBuilder;
        this.escopoFilialService = escopoFilialService;
    }

    public ContasAPagarOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                ),
                agregado AS (
                    SELECT
                        MAX([Data de extracao]) AS updated_at,
                        COUNT(1) AS total_titulos,
                        SUM(COALESCE([Valor a pagar], 0)) AS valor_a_pagar,
                        SUM(COALESCE([Valor pago], 0)) AS valor_pago,
                        SUM(CASE
                            WHEN LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Pago])))) IN (N'sim', N'pago')
                            THEN 1 ELSE 0
                        END) AS pagos,
                        SUM(CASE
                            WHEN [Conciliado] IS NOT NULL
                             AND LOWER(CONVERT(NVARCHAR(MAX), [Conciliado])) LIKE N'%%conciliado%%'
                            THEN 1 ELSE 0
                        END) AS conciliados,
                        AVG(CASE
                            WHEN [Emissão] IS NOT NULL AND [Baixa/Data liquidação] IS NOT NULL
                            THEN CAST(DATEDIFF(day, [Emissão], [Baixa/Data liquidação]) AS FLOAT)
                        END) AS lead_time_liquidacao_dias
                    FROM base_filtrada
                )
                SELECT
                    updated_at,
                    total_titulos,
                    CAST(COALESCE(valor_a_pagar, 0) AS DECIMAL(19,2)) AS valor_a_pagar,
                    CAST(COALESCE(valor_pago, 0) AS DECIMAL(19,2)) AS valor_pago,
                    CAST(COALESCE(valor_a_pagar, 0) - COALESCE(valor_pago, 0) AS DECIMAL(19,2)) AS saldo_aberto,
                    CAST(COALESCE(CAST(pagos AS FLOAT) * 100.0 / NULLIF(total_titulos, 0), 0) AS DECIMAL(19,2)) AS taxa_liquidacao,
                    CAST(COALESCE(lead_time_liquidacao_dias, 0) AS DECIMAL(19,1)) AS lead_time_liquidacao_dias,
                    CAST(COALESCE(CAST(conciliados AS FLOAT) * 100.0 / NULLIF(total_titulos, 0), 0) AS DECIMAL(19,2)) AS pct_conciliado
                FROM agregado
                """.formatted(source.sql());

        return jdbcTemplate.queryForObject(sql, copiarParams(source), (rs, rowNum) -> new ContasAPagarOverviewDTO(
                updatedAt(rs.getTimestamp("updated_at")),
                decimal(rs.getBigDecimal("valor_a_pagar")),
                decimal(rs.getBigDecimal("valor_pago")),
                decimal(rs.getBigDecimal("saldo_aberto")),
                percentual(rs.getBigDecimal("taxa_liquidacao")),
                umaCasa(rs.getBigDecimal("lead_time_liquidacao_dias")),
                percentual(rs.getBigDecimal("pct_conciliado"))
        ));
    }

    public List<ContasAPagarMensalTrendDTO> buscarSerie(FiltroConsultaDTO filtro) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                )
                SELECT
                    CONVERT(CHAR(7), [Emissão], 23) AS mes,
                    CAST(COALESCE(SUM([Valor pago]), 0) AS DECIMAL(19,2)) AS pago,
                    CAST(COALESCE(SUM(COALESCE([Valor a pagar], 0) - COALESCE([Valor pago], 0)), 0) AS DECIMAL(19,2)) AS aberto
                FROM base_filtrada
                WHERE [Emissão] IS NOT NULL
                GROUP BY CONVERT(CHAR(7), [Emissão], 23)
                ORDER BY mes
                """.formatted(source.sql());

        return jdbcTemplate.query(sql, copiarParams(source), (rs, rowNum) -> new ContasAPagarMensalTrendDTO(
                rs.getString("mes"),
                decimal(rs.getBigDecimal("pago")),
                decimal(rs.getBigDecimal("aberto"))
        ));
    }

    public ContasAPagarChartsDTO buscarGraficos(FiltroConsultaDTO filtro) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        return new ContasAPagarChartsDTO(
              buscarTopFornecedores(source),
              buscarCentroCusto(source),
              buscarConciliacao(source)
        );
    }

    private List<ContasAPagarFornecedorDTO> buscarTopFornecedores(DashboardExportSqlBuilder.ExportSql source) {
        String fornecedorSql = textoComPadrao("[Fornecedor/Nome]", "Sem fornecedor");
        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                )
                SELECT TOP (10)
                    %s AS fornecedor,
                    CAST(COALESCE(SUM([Valor a pagar]), 0) AS DECIMAL(19,2)) AS valor,
                    COUNT(1) AS titulos
                FROM base_filtrada
                GROUP BY %s
                ORDER BY valor DESC, fornecedor
                """.formatted(source.sql(), fornecedorSql, fornecedorSql);

        return jdbcTemplate.query(sql, copiarParams(source), (rs, rowNum) -> new ContasAPagarFornecedorDTO(
                rs.getString("fornecedor"),
                decimal(rs.getBigDecimal("valor")),
                rs.getInt("titulos")
        ));
    }

    private List<ContasAPagarCentroCustoDTO> buscarCentroCusto(DashboardExportSqlBuilder.ExportSql source) {
        String centroCustoSql = textoComPadrao("[Centro de custo/Nome]", "Sem centro");
        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                )
                SELECT
                    %s AS centro_custo,
                    CAST(COALESCE(SUM([Valor a pagar]), 0) AS DECIMAL(19,2)) AS valor
                FROM base_filtrada
                GROUP BY %s
                ORDER BY valor DESC, centro_custo
                """.formatted(source.sql(), centroCustoSql, centroCustoSql);

        return jdbcTemplate.query(sql, copiarParams(source), (rs, rowNum) -> new ContasAPagarCentroCustoDTO(
                rs.getString("centro_custo"),
                decimal(rs.getBigDecimal("valor"))
        ));
    }

    private List<ContasAPagarConciliacaoDTO> buscarConciliacao(DashboardExportSqlBuilder.ExportSql source) {
        String conciliadoSql = textoComPadrao("[Conciliado]", "Nao informado");
        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                )
                SELECT
                    %s AS status,
                    COUNT(1) AS total,
                    CAST(COALESCE(SUM([Valor a pagar]), 0) AS DECIMAL(19,2)) AS valor
                FROM base_filtrada
                GROUP BY %s
                ORDER BY valor DESC, status
                """.formatted(source.sql(), conciliadoSql, conciliadoSql);

        return jdbcTemplate.query(sql, copiarParams(source), (rs, rowNum) -> new ContasAPagarConciliacaoDTO(
                rs.getString("status"),
                rs.getInt("total"),
                decimal(rs.getBigDecimal("valor"))
        ));
    }

    private DashboardExportSqlBuilder.ExportSql source(FiltroConsultaDTO filtro) {
        return sqlBuilder.buildFilteredSource(
                DashboardExportDefinition.CONTAS_A_PAGAR,
                filtro,
                escopoFilialService.escopoAtual(),
                Set.of()
        );
    }

    private MapSqlParameterSource copiarParams(DashboardExportSqlBuilder.ExportSql source) {
        return new MapSqlParameterSource(source.params().getValues());
    }

    private String textoComPadrao(String coluna, String padrao) {
        return "COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), " + coluna + "))), N''), N'" + padrao + "')";
    }

    private String updatedAt(Timestamp timestamp) {
        LocalDateTime valor = timestamp != null ? timestamp.toLocalDateTime() : null;
        return TemporalJsonUtils.formatarIsoComOffset(valor);
    }

    private BigDecimal decimal(BigDecimal valor) {
        return (valor != null ? valor : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private double percentual(BigDecimal valor) {
        return decimal(valor).doubleValue();
    }

    private double umaCasa(BigDecimal valor) {
        return (valor != null ? valor : BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    @SuppressWarnings("unused")
    private String data(Date data) {
        return data != null ? data.toLocalDate().toString() : null;
    }
}
