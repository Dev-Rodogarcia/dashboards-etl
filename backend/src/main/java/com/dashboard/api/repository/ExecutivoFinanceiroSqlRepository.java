package com.dashboard.api.repository;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.executivo.ExecutivoResumoFinanceiroDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ExecutivoFinanceiroSqlRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EscopoFilialService escopoFilialService;
    private volatile FretesFactColumns fretesFactColumns;

    public ExecutivoFinanceiroSqlRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            EscopoFilialService escopoFilialService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.escopoFilialService = escopoFilialService;
    }

    public List<ExecutivoResumoFinanceiroDTO> buscarResumoFinanceiro(FiltroConsultaDTO filtro) {
        QueryContext ctx = criarContexto(filtro);
        String fretePesoSql = decimalSql(ctx.colunas(), "freight_weight_subtotal", "frete_peso", "Frete Peso");
        String sql = """
                WITH fretes AS (
                    SELECT
                        COALESCE(
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), filial_nome))), ''),
                            N'Filial não informada'
                        ) AS filial,
                        LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), filial_nome))), '')) AS filial_normalizada,
                        COALESCE(receita_bruta, 0) AS total_faturado,
                        %s AS frete_peso,
                        COALESCE(valor_frete, 0) AS frete_valor,
                        CONVERT(INT, is_elegivel_faturamento) AS elegivel_faturamento,
                        data_referencia_faturamento_date AS data_referencia_periodo
                    FROM [ETL_SISTEMA].dbo.fato_fretes_faturamento
                    WHERE excluido_na_origem = 0
                ),
                filtrados AS (
                    SELECT *
                    FROM fretes
                    WHERE data_referencia_periodo >= :dataInicio
                      AND data_referencia_periodo < :dataFimExclusivo
                      %s
                )
                SELECT
                    filial,
                    CAST(COALESCE(SUM(CASE WHEN elegivel_faturamento = 1 THEN total_faturado ELSE 0 END), 0) AS DECIMAL(19, 2)) AS total_faturado,
                    CAST(COALESCE(SUM(CASE WHEN elegivel_faturamento = 1 THEN frete_peso ELSE 0 END), 0) AS DECIMAL(19, 2)) AS frete_peso,
                    CAST(COALESCE(SUM(CASE WHEN elegivel_faturamento = 1 THEN frete_valor ELSE 0 END), 0) AS DECIMAL(19, 2)) AS frete_valor,
                    CAST(COALESCE(
                        SUM(CASE WHEN elegivel_faturamento = 1 THEN total_faturado ELSE 0 END)
                        / NULLIF(SUM(CASE WHEN elegivel_faturamento = 1 THEN 1 ELSE 0 END), 0),
                        0
                    ) AS DECIMAL(19, 2)) AS ticket_medio
                FROM filtrados
                GROUP BY filial
                HAVING COALESCE(SUM(CASE WHEN elegivel_faturamento = 1 THEN total_faturado ELSE 0 END), 0) <> 0
                    OR COALESCE(SUM(CASE WHEN elegivel_faturamento = 1 THEN frete_peso ELSE 0 END), 0) <> 0
                    OR COALESCE(SUM(CASE WHEN elegivel_faturamento = 1 THEN frete_valor ELSE 0 END), 0) <> 0
                ORDER BY total_faturado DESC, filial
                """.formatted(fretePesoSql, ctx.where());

        return jdbcTemplate.query(sql, ctx.params(), this::mapearResumoFinanceiro);
    }

    private QueryContext criarContexto(FiltroConsultaDTO filtro) {
        QueryContext ctx = new QueryContext(
                new StringBuilder(),
                new MapSqlParameterSource(Map.of(
                        "dataInicio", filtro.dataInicio(),
                        "dataFimExclusivo", filtro.dataFim().plusDays(1)
                )),
                carregarColunasFretes()
        );

        aplicarEscopo(ctx);
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "filiais", "filial_normalizada", filtro.valores("filiais"));
        return ctx;
    }

    private void aplicarEscopo(QueryContext ctx) {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        if (escopo.acessoTotal()) {
            return;
        }

        List<String> filiais = normalizar(escopo.filiaisOrdenadas());
        if (filiais.isEmpty()) {
            ctx.whereBuilder().append("\n                      AND 1 = 0");
            return;
        }

        ctx.params().addValue("escopoFiliais", filiais);
        ctx.whereBuilder().append("\n                      AND filial_normalizada IN (:escopoFiliais)");
    }

    private static void adicionarFiltroTexto(
            StringBuilder where,
            MapSqlParameterSource params,
            String chave,
            String campo,
            Collection<String> valores
    ) {
        List<String> normalizados = normalizar(valores);
        if (normalizados.isEmpty()) {
            return;
        }

        params.addValue(chave, normalizados);
        where.append("\n                      AND ").append(campo).append(" IN (:").append(chave).append(")");
    }

    private FretesFactColumns carregarColunasFretes() {
        FretesFactColumns cached = fretesFactColumns;
        if (cached != null) {
            return cached;
        }

        List<String> nomes = jdbcTemplate.queryForList("""
                SELECT c.name
                FROM [ETL_SISTEMA].sys.columns c
                WHERE c.object_id = OBJECT_ID(N'[ETL_SISTEMA].dbo.fato_fretes_faturamento')
                """, new MapSqlParameterSource(), String.class);

        FretesFactColumns carregadas = new FretesFactColumns(nomes);
        fretesFactColumns = carregadas;
        return carregadas;
    }

    private ExecutivoResumoFinanceiroDTO mapearResumoFinanceiro(ResultSet rs, int rowNum) throws SQLException {
        return new ExecutivoResumoFinanceiroDTO(
                rs.getString("filial"),
                decimal(rs, "total_faturado"),
                decimal(rs, "frete_peso"),
                decimal(rs, "frete_valor"),
                decimal(rs, "ticket_medio")
        );
    }

    private static String decimalSql(FretesFactColumns colunas, String... nomes) {
        List<String> expressoes = List.of(nomes).stream()
                .filter(colunas::existe)
                .map(nome -> "TRY_CONVERT(DECIMAL(19, 4), [" + nome + "])")
                .toList();
        if (expressoes.isEmpty()) {
            return "CAST(0 AS DECIMAL(19, 4))";
        }
        if (expressoes.size() == 1) {
            return "COALESCE(" + expressoes.get(0) + ", 0)";
        }
        return "COALESCE(" + String.join(", ", expressoes) + ", 0)";
    }

    private static BigDecimal decimal(ResultSet rs, String coluna) throws SQLException {
        BigDecimal valor = rs.getBigDecimal(coluna);
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private static List<String> normalizar(Collection<String> valores) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(valor -> valor.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private record QueryContext(
            StringBuilder whereBuilder,
            MapSqlParameterSource params,
            FretesFactColumns colunas
    ) {
        String where() {
            return whereBuilder.toString();
        }
    }

    private record FretesFactColumns(List<String> nomes) {
        boolean existe(String nome) {
            return nomes.stream().anyMatch(existente -> existente.equalsIgnoreCase(nome));
        }
    }
}
