package com.dashboard.api.repository;

import com.dashboard.api.builder.DashboardExportSqlBuilder;
import com.dashboard.api.definition.DashboardExportDefinition;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteAgingBucketDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteMensalDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteOverviewDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteStatusProcessoDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteTopClienteDTO;
import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.TemporalJsonUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class FaturasPorClienteSqlRepository {

    private static final List<String> ORDEM_AGING = List.of(
            "A vencer",
            "1-15 dias",
            "16-30 dias",
            "31-60 dias",
            "61+ dias",
            "Sem vencimento"
    );

    private final NamedParameterJdbcOperations jdbcTemplate;
    private final DashboardExportSqlBuilder sqlBuilder;
    private final EscopoFilialService escopoFilialService;

    public FaturasPorClienteSqlRepository(
            NamedParameterJdbcOperations jdbcTemplate,
            DashboardExportSqlBuilder sqlBuilder,
            EscopoFilialService escopoFilialService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlBuilder = sqlBuilder;
        this.escopoFilialService = escopoFilialService;
    }

    public FaturasPorClienteOverviewDTO buscarOverview(FiltroConsultaDTO filtro, LocalDate dataReferencia) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        MapSqlParameterSource params = copiarParams(source);
        params.addValue("dataReferencia", dataReferencia);

        String sql = baseNormalizadaSql(source) + """
                SELECT
                    MAX(snapshot_em) AS updated_at,
                    CAST(COALESCE(SUM(CASE WHEN status_processo = N'Faturado' THEN valor_operacional ELSE 0 END), 0) AS DECIMAL(19,2)) AS valor_faturado,
                    SUM(CASE WHEN status_processo = N'Faturado' THEN 1 ELSE 0 END) AS registros_faturados,
                    SUM(CASE WHEN status_processo = N'Aguardando Faturamento' THEN 1 ELSE 0 END) AS aguardando_faturamento,
                    SUM(CASE
                        WHEN status_processo = N'Faturado'
                         AND data_vencimento_fatura IS NOT NULL
                         AND data_vencimento_fatura < :dataReferencia
                         AND data_baixa_fatura IS NULL
                        THEN 1 ELSE 0
                    END) AS titulos_em_atraso,
                    CAST(COALESCE(AVG(CASE
                        WHEN documento_fatura IS NOT NULL
                         AND data_base_prazo IS NOT NULL
                         AND data_vencimento_fatura IS NOT NULL
                        THEN CAST(DATEDIFF(day, data_base_prazo, data_vencimento_fatura) AS FLOAT)
                    END), 0) AS DECIMAL(19,1)) AS prazo_medio_dias,
                    COUNT(DISTINCT cliente_chave) AS clientes_ativos
                FROM base_normalizada
                """;

        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> new FaturasPorClienteOverviewDTO(
                updatedAt(rs.getTimestamp("updated_at")),
                decimal(rs.getBigDecimal("valor_faturado")),
                rs.getInt("registros_faturados"),
                rs.getInt("aguardando_faturamento"),
                rs.getInt("titulos_em_atraso"),
                umaCasa(rs.getBigDecimal("prazo_medio_dias")),
                rs.getInt("clientes_ativos")
        ));
    }

    public List<FaturasPorClienteMensalDTO> buscarMensal(FiltroConsultaDTO filtro) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        String sql = baseNormalizadaSql(source) + """
                SELECT
                    CONVERT(CHAR(7), data_referencia_mensal, 23) AS mes,
                    CAST(COALESCE(SUM(valor_operacional), 0) AS DECIMAL(19,2)) AS valor_faturado,
                    COUNT(1) AS registros_faturados
                FROM base_normalizada
                WHERE documento_fatura IS NOT NULL
                  AND data_referencia_mensal IS NOT NULL
                GROUP BY CONVERT(CHAR(7), data_referencia_mensal, 23)
                ORDER BY mes
                """;

        return jdbcTemplate.query(sql, copiarParams(source), (rs, rowNum) -> new FaturasPorClienteMensalDTO(
                rs.getString("mes"),
                decimal(rs.getBigDecimal("valor_faturado")),
                rs.getInt("registros_faturados")
        ));
    }

    public List<FaturasPorClienteAgingBucketDTO> buscarAging(FiltroConsultaDTO filtro, LocalDate dataReferencia) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        MapSqlParameterSource params = copiarParams(source);
        params.addValue("dataReferencia", dataReferencia);

        String sql = baseNormalizadaSql(source) + """
                , titulos_aging AS (
                    SELECT
                        valor_operacional,
                        data_vencimento_fatura,
                        status_pagamento
                    FROM base_normalizada
                    WHERE status_processo = N'Faturado'
                      AND status_pagamento <> N'baixado'
                ),
                aging_calculado AS (
                    SELECT
                        valor_operacional,
                        data_vencimento_fatura,
                        status_pagamento,
                        CASE
                            WHEN data_vencimento_fatura IS NULL THEN NULL
                            ELSE DATEDIFF(day, data_vencimento_fatura, :dataReferencia)
                        END AS dias_atraso
                    FROM titulos_aging
                ),
                classificacao AS (
                    SELECT
                        valor_operacional,
                        CASE
                            WHEN status_pagamento = N'sem_vencimento' OR data_vencimento_fatura IS NULL THEN N'Sem vencimento'
                            WHEN status_pagamento = N'a_vencer' THEN N'A vencer'
                            WHEN dias_atraso < 0 THEN N'A vencer'
                            WHEN dias_atraso <= 15 THEN N'1-15 dias'
                            WHEN dias_atraso <= 30 THEN N'16-30 dias'
                            WHEN dias_atraso <= 60 THEN N'31-60 dias'
                            ELSE N'61+ dias'
                        END AS faixa
                    FROM aging_calculado
                )
                SELECT
                    faixa,
                    CAST(COALESCE(SUM(valor_operacional), 0) AS DECIMAL(19,2)) AS valor,
                    COUNT(1) AS titulos
                FROM classificacao
                GROUP BY faixa
                """;

        Map<String, FaturasPorClienteAgingBucketDTO> porFaixa = new LinkedHashMap<>();
        jdbcTemplate.query(sql, params, (org.springframework.jdbc.core.RowCallbackHandler) rs -> porFaixa.put(
                rs.getString("faixa"),
                new FaturasPorClienteAgingBucketDTO(
                        rs.getString("faixa"),
                        decimal(rs.getBigDecimal("valor")),
                        rs.getInt("titulos")
                )
        ));

        return ORDEM_AGING.stream()
                .map(faixa -> porFaixa.getOrDefault(
                        faixa,
                        new FaturasPorClienteAgingBucketDTO(faixa, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0)
                ))
                .toList();
    }

    public List<FaturasPorClienteTopClienteDTO> buscarTopClientes(FiltroConsultaDTO filtro, int limite) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        MapSqlParameterSource params = copiarParams(source);
        params.addValue("limite", limite);

        String sql = baseNormalizadaSql(source) + """
                , totais AS (
                    SELECT
                        cliente_chave,
                        CAST(COALESCE(SUM(valor_operacional), 0) AS DECIMAL(19,2)) AS valor_faturado
                    FROM base_normalizada
                    WHERE status_processo = N'Faturado'
                      AND cliente_chave IS NOT NULL
                    GROUP BY cliente_chave
                ),
                representantes AS (
                    SELECT cliente_chave, cliente_nome, cliente_cnpj
                    FROM (
                        SELECT
                            cliente_chave,
                            cliente_nome,
                            cliente_cnpj,
                            ROW_NUMBER() OVER (
                                PARTITION BY cliente_chave
                                ORDER BY snapshot_em DESC, data_emissao_cte DESC, unique_id ASC
                            ) AS [__rn_cliente]
                        FROM base_normalizada
                        WHERE status_processo = N'Faturado'
                          AND cliente_chave IS NOT NULL
                    ) ranked
                    WHERE [__rn_cliente] = 1
                )
                SELECT
                    representantes.cliente_nome,
                    representantes.cliente_cnpj,
                    totais.valor_faturado
                FROM totais
                JOIN representantes ON representantes.cliente_chave = totais.cliente_chave
                ORDER BY totais.valor_faturado DESC, representantes.cliente_nome
                OFFSET 0 ROWS FETCH NEXT :limite ROWS ONLY
                """;

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new FaturasPorClienteTopClienteDTO(
                rs.getString("cliente_nome"),
                rs.getString("cliente_cnpj"),
                decimal(rs.getBigDecimal("valor_faturado"))
        ));
    }

    public List<FaturasPorClienteStatusProcessoDTO> buscarStatusProcesso(FiltroConsultaDTO filtro) {
        DashboardExportSqlBuilder.ExportSql source = source(filtro);
        String sql = baseNormalizadaSql(source) + """
                SELECT
                    status_processo,
                    COUNT(1) AS total
                FROM base_normalizada
                GROUP BY status_processo
                ORDER BY total DESC, status_processo
                """;

        return jdbcTemplate.query(sql, copiarParams(source), (rs, rowNum) -> new FaturasPorClienteStatusProcessoDTO(
                rs.getString("status_processo"),
                rs.getInt("total")
        ));
    }

    private String baseNormalizadaSql(DashboardExportSqlBuilder.ExportSql source) {
        return """
                WITH base_normalizada AS (
                    SELECT
                        unique_id,
                        chave_normalizacao,
                        documento_fatura,
                        data_emissao_cte,
                        data_emissao_fatura,
                        data_vencimento_fatura,
                        data_baixa_fatura,
                        data_base_prazo,
                        data_referencia_mensal,
                        valor_operacional,
                        cliente_chave,
                        cliente_nome,
                        cliente_cnpj,
                        status_processo,
                        status_pagamento,
                        snapshot_em
                    %s
                )
                """.formatted(
                source.sql()
        );
    }

    private DashboardExportSqlBuilder.ExportSql source(FiltroConsultaDTO filtro) {
        return sqlBuilder.buildFilteredSource(
                DashboardExportDefinition.FATURAS_POR_CLIENTE,
                filtro,
                escopoFilialService.escopoAtual(),
                Set.of()
        );
    }

    private MapSqlParameterSource copiarParams(DashboardExportSqlBuilder.ExportSql source) {
        return new MapSqlParameterSource(source.params().getValues());
    }

    private String updatedAt(Timestamp timestamp) {
        LocalDateTime valor = timestamp != null ? timestamp.toLocalDateTime() : null;
        return TemporalJsonUtils.formatarIsoComOffset(valor);
    }

    private BigDecimal decimal(BigDecimal valor) {
        return (valor != null ? valor : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private double umaCasa(BigDecimal valor) {
        return (valor != null ? valor : BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
