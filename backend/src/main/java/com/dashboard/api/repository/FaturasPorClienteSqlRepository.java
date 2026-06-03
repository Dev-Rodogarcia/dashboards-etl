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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
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
                    MAX(data_extracao) AS updated_at,
                    CAST(COALESCE(SUM(CASE WHEN documento_fatura IS NOT NULL THEN valor_operacional ELSE 0 END), 0) AS DECIMAL(19,2)) AS valor_faturado,
                    SUM(CASE WHEN documento_fatura IS NOT NULL THEN 1 ELSE 0 END) AS registros_faturados,
                    SUM(CASE WHEN documento_fatura IS NULL THEN 1 ELSE 0 END) AS aguardando_faturamento,
                    SUM(CASE
                        WHEN documento_fatura IS NOT NULL
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
                , classificacao AS (
                    SELECT
                        valor_operacional,
                        CASE
                            WHEN data_vencimento_fatura IS NULL THEN N'Sem vencimento'
                            WHEN DATEDIFF(day, data_vencimento_fatura, :dataReferencia) < 0 THEN N'A vencer'
                            WHEN DATEDIFF(day, data_vencimento_fatura, :dataReferencia) <= 15 THEN N'1-15 dias'
                            WHEN DATEDIFF(day, data_vencimento_fatura, :dataReferencia) <= 30 THEN N'16-30 dias'
                            WHEN DATEDIFF(day, data_vencimento_fatura, :dataReferencia) <= 60 THEN N'31-60 dias'
                            ELSE N'61+ dias'
                        END AS faixa
                    FROM base_normalizada
                    WHERE documento_fatura IS NOT NULL
                      AND data_baixa_fatura IS NULL
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
                    WHERE documento_fatura IS NOT NULL
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
                                ORDER BY data_extracao DESC, data_emissao_cte DESC, unique_id ASC
                            ) AS [__rn_cliente]
                        FROM base_normalizada
                        WHERE documento_fatura IS NOT NULL
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
                    CASE
                        WHEN documento_fatura IS NOT NULL THEN N'Faturado'
                        ELSE N'Aguardando Faturamento'
                    END AS status_processo,
                    COUNT(1) AS total
                FROM base_normalizada
                GROUP BY CASE
                    WHEN documento_fatura IS NOT NULL THEN N'Faturado'
                    ELSE N'Aguardando Faturamento'
                END
                ORDER BY total DESC, status_processo
                """;

        return jdbcTemplate.query(sql, copiarParams(source), (rs, rowNum) -> new FaturasPorClienteStatusProcessoDTO(
                rs.getString("status_processo"),
                rs.getInt("total")
        ));
    }

    private String baseNormalizadaSql(DashboardExportSqlBuilder.ExportSql source) {
        String documentoOperacionalSql = """
                CASE
                    WHEN deslocamento.deslocada = 1
                    THEN NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Fatura/Emissão]))), N'')
                    ELSE documento_raw.documento
                END
                """;
        String emissaoFaturaSql = dateNullableSql("""
                CASE
                    WHEN deslocamento.deslocada = 1
                    THEN COALESCE(CONVERT(NVARCHAR(64), [Fatura/Valor]), CONVERT(NVARCHAR(64), [CT-e/Data de emissão]))
                    ELSE CONVERT(NVARCHAR(64), [Fatura/Emissão])
                END
                """);
        String valorFitAntSql = decimalNullableSql("""
                CASE
                    WHEN deslocamento.deslocada = 1
                    THEN CONVERT(NVARCHAR(100), [Fatura/Valor Total])
                    ELSE CONVERT(NVARCHAR(100), [Fatura/Valor])
                END
                """);
        String valorFaturaSql = decimalNullableSql("""
                CASE
                    WHEN deslocamento.deslocada = 1
                    THEN CONVERT(NVARCHAR(100), [Fatura/Número])
                    ELSE CONVERT(NVARCHAR(100), [Fatura/Valor Total])
                END
                """);
        String dataEmissaoFaturaSql = dateNullableSql("""
                CASE
                    WHEN deslocamento.deslocada = 1
                    THEN CONVERT(NVARCHAR(64), [Parcelas/Vencimento])
                    ELSE CONVERT(NVARCHAR(64), [Fatura/Emissão Fatura])
                END
                """);
        String dataVencimentoSql = dateNullableSql("""
                CASE
                    WHEN deslocamento.deslocada = 1
                    THEN CONVERT(NVARCHAR(64), [Fatura/Baixa])
                    ELSE CONVERT(NVARCHAR(64), [Parcelas/Vencimento])
                END
                """);
        String dataBaixaSql = dateNullableSql("""
                CASE
                    WHEN deslocamento.deslocada = 1
                    THEN CONVERT(NVARCHAR(64), [Fatura/Data Vencimento Original])
                    ELSE CONVERT(NVARCHAR(64), [Fatura/Baixa])
                END
                """);

        return """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                ),
                base_operacional AS (
                    SELECT
                        id_unico.valor AS unique_id,
                        [CT-e/Data de emissão] AS data_emissao_cte,
                        data_extracao.valor AS data_extracao,
                        documento.documento_fatura,
                        datas.emissao_fatura,
                        datas.data_emissao_fatura,
                        datas.data_vencimento_fatura,
                        datas.data_baixa_fatura,
                        datas_calc.data_base_prazo,
                        datas_calc.data_referencia_mensal,
                        valores.valor_operacional,
                        cliente.cliente_chave,
                        cliente.cliente_nome,
                        cliente.cliente_cnpj,
                        chave.chave_normalizacao
                    FROM base_filtrada
                    CROSS APPLY (
                        SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [ID Único]))), N'') AS valor
                    ) id_unico
                    CROSS APPLY (
                        SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Fatura/N° Documento]))), N'') AS documento
                    ) documento_raw
                    CROSS APPLY (
                        SELECT CASE
                            WHEN LOWER(documento_raw.documento) IN (N'faturado', N'aguardando faturamento')
                            THEN 1 ELSE 0
                        END AS deslocada
                    ) deslocamento
                    CROSS APPLY (
                        SELECT %s AS documento_fatura
                    ) documento
                    CROSS APPLY (
                        SELECT
                            %s AS emissao_fatura,
                            %s AS data_emissao_fatura,
                            %s AS data_vencimento_fatura,
                            %s AS data_baixa_fatura
                    ) datas
                    CROSS APPLY (
                        SELECT
                            COALESCE(datas.emissao_fatura, datas.data_emissao_fatura) AS data_base_prazo,
                            COALESCE(datas.emissao_fatura, datas.data_emissao_fatura, CAST([CT-e/Data de emissão] AS date)) AS data_referencia_mensal
                    ) datas_calc
                    CROSS APPLY (
                        SELECT %s AS valor
                    ) data_extracao
                    CROSS APPLY (
                        SELECT
                            COALESCE(%s, %s, %s, 0) AS valor_operacional
                    ) valores
                    CROSS APPLY (
                        SELECT
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Pagador do frete/Nome]))), N'') AS pagador_nome,
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Cliente/CNPJ]))), N'') AS cliente_cnpj_raw,
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Pagador do frete/Documento]))), N'') AS pagador_documento
                    ) cliente_raw
                    CROSS APPLY (
                        SELECT %s AS pagador_documento_digitos
                    ) cliente_digitos
                    CROSS APPLY (
                        SELECT CASE
                            WHEN cliente_raw.cliente_cnpj_raw IS NOT NULL THEN cliente_raw.cliente_cnpj_raw
                            WHEN LEN(cliente_digitos.pagador_documento_digitos) = 14
                             AND cliente_digitos.pagador_documento_digitos NOT LIKE N'%%[^0-9]%%'
                            THEN cliente_digitos.pagador_documento_digitos
                        END AS cliente_cnpj
                    ) cliente_documento
                    CROSS APPLY (
                        SELECT %s AS cliente_cnpj_digitos
                    ) cliente_documento_digitos
                    CROSS APPLY (
                        SELECT
                            CASE
                                WHEN cliente_documento.cliente_cnpj IS NOT NULL
                                THEN N'cnpj:' + COALESCE(NULLIF(cliente_documento_digitos.cliente_cnpj_digitos, N''), LOWER(cliente_documento.cliente_cnpj))
                                WHEN cliente_raw.pagador_nome IS NOT NULL
                                THEN N'nome:' + LOWER(cliente_raw.pagador_nome)
                            END AS cliente_chave,
                            CASE
                                WHEN cliente_documento.cliente_cnpj IS NOT NULL
                                THEN COALESCE(cliente_raw.pagador_nome, cliente_documento.cliente_cnpj)
                                ELSE cliente_raw.pagador_nome
                            END AS cliente_nome,
                            cliente_documento.cliente_cnpj AS cliente_cnpj
                    ) cliente
                    CROSS APPLY (
                        SELECT CASE
                            WHEN id_unico.valor IS NOT NULL THEN N'linha|' + id_unico.valor
                            WHEN documento.documento_fatura IS NOT NULL THEN N'fatura|' + LOWER(documento.documento_fatura)
                        END AS chave_normalizacao
                    ) chave
                    WHERE chave.chave_normalizacao IS NOT NULL
                ),
                base_normalizada AS (
                    SELECT *
                    FROM (
                        SELECT
                            base_operacional.*,
                            ROW_NUMBER() OVER (
                                PARTITION BY chave_normalizacao
                                ORDER BY data_extracao DESC, data_emissao_cte DESC, unique_id ASC
                            ) AS [__rn]
                        FROM base_operacional
                    ) dedup
                    WHERE [__rn] = 1
                )
                """.formatted(
                source.sql(),
                documentoOperacionalSql,
                emissaoFaturaSql,
                dataEmissaoFaturaSql,
                dataVencimentoSql,
                dataBaixaSql,
                datetime2NullableSql("[Data da Última Atualização]"),
                valorFitAntSql,
                valorFaturaSql,
                decimalNullableSql("[Frete/Valor dos CT-es]"),
                digitsSql("cliente_raw.pagador_documento"),
                digitsSql("cliente_documento.cliente_cnpj")
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

    private static String dateNullableSql(String expressao) {
        String texto = "NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(64), " + expressao + "))), N'')";
        return """
                COALESCE(
                    TRY_CONVERT(date, %1$s),
                    TRY_CONVERT(date, %2$s, 23),
                    TRY_CONVERT(date, %2$s, 103),
                    TRY_CONVERT(date, %2$s, 112),
                    TRY_CONVERT(date, %2$s, 126)
                )
                """.formatted(expressao, texto);
    }

    private static String datetime2NullableSql(String expressao) {
        String texto = "NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(64), " + expressao + "))), N'')";
        return """
                COALESCE(
                    TRY_CONVERT(datetime2, %1$s),
                    TRY_CONVERT(datetime2, %2$s, 126),
                    TRY_CONVERT(datetime2, %2$s, 120),
                    TRY_CONVERT(datetime2, %2$s, 103)
                )
                """.formatted(expressao, texto);
    }

    private static String decimalNullableSql(String expressao) {
        String texto = "REPLACE(REPLACE(CONVERT(NVARCHAR(100), " + expressao + "), NCHAR(160), N''), N' ', N'')";
        return """
                COALESCE(
                    TRY_CONVERT(DECIMAL(19,4), %1$s),
                    TRY_CONVERT(DECIMAL(19,4), %2$s),
                    TRY_CONVERT(DECIMAL(19,4), REPLACE(REPLACE(%2$s, N'.', N''), N',', N'.'))
                )
                """.formatted(expressao, texto);
    }

    private static String digitsSql(String expressao) {
        return """
                REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                    LTRIM(RTRIM(CONVERT(NVARCHAR(100), %s))),
                    N'.', N''), N'/', N''), N'-', N''), N' ', N''), NCHAR(160), N''), N'(', N''), N')', N'')
                """.formatted(expressao);
    }

    private String updatedAt(Timestamp timestamp) {
        LocalDateTime valor = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();
        return valor.format(ISO_LOCAL_DATE_TIME);
    }

    private BigDecimal decimal(BigDecimal valor) {
        return (valor != null ? valor : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private double umaCasa(BigDecimal valor) {
        return (valor != null ? valor : BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
