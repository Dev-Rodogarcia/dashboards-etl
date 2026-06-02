package com.dashboard.api.repository;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.model.VisaoFretesEntity;
import com.dashboard.api.service.acesso.EscopoFilialService;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FretesDashboardSqlRepository {

    private static final ZoneOffset OFFSET_BRASILIA = ZoneOffset.ofHours(-3);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EscopoFilialService escopoFilialService;
    private volatile FretesViewColumns fretesViewColumns;

    public FretesDashboardSqlRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            EscopoFilialService escopoFilialService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.escopoFilialService = escopoFilialService;
    }

    public List<VisaoFretesEntity> buscarRegistros(FiltroConsultaDTO filtro) {
        QueryContext ctx = criarContexto(filtro);
        String sql = baseSql(ctx.colunas()) + """
                SELECT *
                FROM fretes
                WHERE data_referencia_periodo >= :dataInicio
                  AND data_referencia_periodo < :dataFim
                """ + ctx.where() + """
                ORDER BY data_referencia_faturamento DESC, numero_minuta DESC, id DESC
                """;

        return jdbcTemplate.query(sql, ctx.params(), this::mapear);
    }

    private QueryContext criarContexto(FiltroConsultaDTO filtro) {
        QueryContext ctx = new QueryContext(
                new StringBuilder(),
                new MapSqlParameterSource(Map.of(
                        "dataInicio", filtro.dataInicio(),
                        "dataFim", filtro.dataFim().plusDays(1)
                )),
                carregarColunasFretes()
        );

        aplicarEscopo(ctx);
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "filiais", "filial_nome", filtro.valores("filiais"));
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "status", "status", filtro.valores("status"));
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "pagadores", "pagador_nome", filtro.valores("pagadores"));
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "ufOrigem", "origem_uf", filtro.valores("ufOrigem"));
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "ufDestino", "destino_uf", filtro.valores("ufDestino"));
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "tiposFrete", "tipo_frete", filtro.valores("tiposFrete"));
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "modais", "modal", filtro.valores("modais"));
        return ctx;
    }

    private void aplicarEscopo(QueryContext ctx) {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        if (escopo.acessoTotal()) {
            return;
        }
        List<String> filiais = normalizar(escopo.filiaisOrdenadas());
        if (filiais.isEmpty()) {
            ctx.whereBuilder().append("\n AND 1 = 0");
            return;
        }
        ctx.params().addValue("escopoFiliais", filiais);
        ctx.whereBuilder().append("\n AND LOWER(filial_nome) IN (:escopoFiliais)");
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
        where.append("\n AND LOWER(").append(campo).append(") IN (:").append(chave).append(")");
    }

    private FretesViewColumns carregarColunasFretes() {
        FretesViewColumns cached = fretesViewColumns;
        if (cached != null) {
            return cached;
        }

        List<String> nomes = jdbcTemplate.queryForList("""
                SELECT c.name
                FROM sys.columns c
                WHERE c.object_id = OBJECT_ID(N'dbo.vw_fretes_powerbi')
                """, new MapSqlParameterSource(), String.class);

        FretesViewColumns carregadas = new FretesViewColumns(nomes);
        fretesViewColumns = carregadas;
        return carregadas;
    }

    private static String baseSql(FretesViewColumns colunas) {
        String dataReferencia = dataHoraSql(colunas, "data_referencia_faturamento", "CT-e Emissão", "Data frete");
        String dataFrete = dataHoraSql(colunas, "Data frete");
        String cteEmissao = dataHoraSql(colunas, "CT-e Emissão");
        String classificacao = textoNullableSql(colunas, "Classificação", "Classificacao");
        String cortesia = boolSql(colunas, "Cortesia Flag");

        return """
                WITH fretes AS (
                    SELECT
                        %s AS id,
                        %s AS data_frete,
                        %s AS numero_minuta,
                        %s AS valor_total,
                        %s AS subtotal,
                        %s AS volumes,
                        %s AS peso_taxado,
                        %s AS pagador_nome,
                        %s AS remetente_nome,
                        %s AS destinatario_nome,
                        %s AS origem_uf,
                        %s AS destino_uf,
                        %s AS destino_cidade,
                        %s AS filial_nome,
                        %s AS filial_emissora,
                        %s AS responsavel_regiao_destino,
                        %s AS classificacao_nome,
                        %s AS previsao_entrega,
                        %s AS status,
                        %s AS cortesia_flag,
                        %s AS tipo_frete,
                        %s AS modal,
                        %s AS numero_cte,
                        %s AS cte_emissao,
                        %s AS data_referencia_faturamento,
                        TRY_CONVERT(date, CONVERT(NVARCHAR(64), %s)) AS data_referencia_periodo,
                        %s AS elegivel_faturamento,
                        %s AS cte_id,
                        %s AS nfse_numero,
                        %s AS valor_icms,
                        %s AS valor_pis,
                        %s AS valor_cofins,
                        %s AS data_extracao
                    FROM dbo.vw_fretes_powerbi
                )
                """.formatted(
                inteiroLongoSql(colunas, "ID"),
                dataFrete,
                inteiroLongoSql(colunas, "Nº Minuta", "N° Minuta"),
                decimalSql(colunas, "Valor Total do Serviço", "Valor Total do Servico"),
                decimalSql(colunas, "Valor Frete"),
                inteiroSql(colunas, "Volumes"),
                decimalSql(colunas, "Kg Taxado", "Peso Taxado"),
                textoNullableSql(colunas, "Pagador"),
                textoNullableSql(colunas, "Remetente"),
                textoNullableSql(colunas, "Destinatario", "Destinatário"),
                textoNullableSql(colunas, "UF Origem"),
                textoNullableSql(colunas, "UF Destino"),
                textoNullableSql(colunas, "Cidade Destino", "Destino"),
                textoNullableSql(colunas, "Filial"),
                textoNullableSql(colunas, "Filial Emissora", "Filial"),
                textoNullableSql(colunas, "Responsável pela Região de Destino", "Filial Emissora", "Filial"),
                classificacao,
                dataSql(colunas, "Previsão de Entrega", "Previsao de Entrega"),
                textoNullableSql(colunas, "Status"),
                cortesia,
                textoNullableSql(colunas, "Tipo Frete"),
                textoNullableSql(colunas, "Modal"),
                inteiroSql(colunas, "Nº CT-e", "N° CT-e"),
                cteEmissao,
                dataReferencia,
                dataReferencia,
                elegivelFaturamentoSql(colunas, cortesia, classificacao),
                inteiroLongoSql(colunas, "CT-e ID"),
                inteiroSql(colunas, "Nº NFS-e", "N° NFS-e"),
                decimalSql(colunas, "Valor ICMS"),
                decimalSql(colunas, "Valor PIS"),
                decimalSql(colunas, "Valor COFINS"),
                dataHoraSql(colunas, "Data de extracao", "Data de extração")
        );
    }

    private VisaoFretesEntity mapear(ResultSet rs, int rowNum) throws SQLException {
        return VisaoFretesEntity.criarParaPainel(
                longo(rs, "id"),
                offset(rs, "data_frete"),
                longo(rs, "numero_minuta"),
                decimal(rs, "valor_total"),
                decimal(rs, "subtotal"),
                inteiro(rs, "volumes"),
                decimal(rs, "peso_taxado"),
                texto(rs, "pagador_nome"),
                texto(rs, "remetente_nome"),
                texto(rs, "destinatario_nome"),
                texto(rs, "origem_uf"),
                texto(rs, "destino_uf"),
                texto(rs, "destino_cidade"),
                texto(rs, "filial_nome"),
                texto(rs, "filial_emissora"),
                texto(rs, "responsavel_regiao_destino"),
                texto(rs, "classificacao_nome"),
                data(rs, "previsao_entrega"),
                texto(rs, "status"),
                booleano(rs, "cortesia_flag"),
                texto(rs, "tipo_frete"),
                texto(rs, "modal"),
                inteiro(rs, "numero_cte"),
                offset(rs, "cte_emissao"),
                offset(rs, "data_referencia_faturamento"),
                booleano(rs, "elegivel_faturamento"),
                longo(rs, "cte_id"),
                inteiro(rs, "nfse_numero"),
                decimal(rs, "valor_icms"),
                decimal(rs, "valor_pis"),
                decimal(rs, "valor_cofins"),
                dataHora(rs, "data_extracao")
        );
    }

    private static String textoNullableSql(FretesViewColumns colunas, String... nomes) {
        List<String> expressoes = List.of(nomes).stream()
                .filter(colunas::existe)
                .map(FretesDashboardSqlRepository::textoColunaSql)
                .toList();
        return coalesceOuNull(expressoes);
    }

    private static String textoColunaSql(String nome) {
        return "NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [" + nome + "]))), '')";
    }

    private static String decimalSql(FretesViewColumns colunas, String... nomes) {
        List<String> expressoes = List.of(nomes).stream()
                .filter(colunas::existe)
                .map(nome -> "TRY_CONVERT(DECIMAL(18, 2), REPLACE(CONVERT(NVARCHAR(100), [" + nome + "]), ',', '.'))")
                .toList();
        return coalesceOuNull(expressoes);
    }

    private static String inteiroSql(FretesViewColumns colunas, String... nomes) {
        List<String> expressoes = List.of(nomes).stream()
                .filter(colunas::existe)
                .map(nome -> "TRY_CONVERT(INT, [" + nome + "])")
                .toList();
        return coalesceOuNull(expressoes);
    }

    private static String inteiroLongoSql(FretesViewColumns colunas, String... nomes) {
        List<String> expressoes = List.of(nomes).stream()
                .filter(colunas::existe)
                .map(nome -> "TRY_CONVERT(BIGINT, [" + nome + "])")
                .toList();
        return coalesceOuNull(expressoes);
    }

    private static String dataSql(FretesViewColumns colunas, String... nomes) {
        List<String> expressoes = List.of(nomes).stream()
                .filter(colunas::existe)
                .map(nome -> "TRY_CONVERT(date, CONVERT(NVARCHAR(64), [" + nome + "]))")
                .toList();
        return coalesceOuNull(expressoes);
    }

    private static String dataHoraSql(FretesViewColumns colunas, String... nomes) {
        List<String> expressoes = List.of(nomes).stream()
                .filter(colunas::existe)
                .map(nome -> "TRY_CONVERT(datetime2, CONVERT(NVARCHAR(64), [" + nome + "]))")
                .toList();
        return coalesceOuNull(expressoes);
    }

    private static String boolSql(FretesViewColumns colunas, String nome) {
        if (!colunas.existe(nome)) {
            return "0";
        }
        return """
                CASE
                    WHEN TRY_CONVERT(bit, [%s]) = 1 THEN 1
                    WHEN UPPER(LTRIM(RTRIM(CONVERT(NVARCHAR(20), [%s])))) IN (N'SIM', N'TRUE', N'1') THEN 1
                    ELSE 0
                END
                """.formatted(nome, nome);
    }

    private static String elegivelFaturamentoSql(
            FretesViewColumns colunas,
            String cortesiaSql,
            String classificacaoSql
    ) {
        if (colunas.existe("is_elegivel_faturamento")) {
            return boolSql(colunas, "is_elegivel_faturamento");
        }

        String classificacaoNormalizada = "UPPER(COALESCE(" + classificacaoSql + ", N''))";
        return """
                CASE
                    WHEN (%s) = 1 THEN 0
                    WHEN %s LIKE N'%%BLOQUEIO%%'
                         AND (%s LIKE N'%%ANULA%%' OR %s LIKE N'%%ISOLAMENTO%%') THEN 0
                    ELSE 1
                END
                """.formatted(cortesiaSql, classificacaoNormalizada, classificacaoNormalizada, classificacaoNormalizada);
    }

    private static String coalesceOuNull(List<String> expressoes) {
        if (expressoes.isEmpty()) {
            return "NULL";
        }
        if (expressoes.size() == 1) {
            return expressoes.get(0);
        }
        return "COALESCE(" + String.join(", ", expressoes) + ")";
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

    private static String texto(ResultSet rs, String coluna) throws SQLException {
        return rs.getString(coluna);
    }

    private static BigDecimal decimal(ResultSet rs, String coluna) throws SQLException {
        return rs.getBigDecimal(coluna);
    }

    private static Integer inteiro(ResultSet rs, String coluna) throws SQLException {
        Object valor = rs.getObject(coluna);
        return valor instanceof Number number ? number.intValue() : null;
    }

    private static Long longo(ResultSet rs, String coluna) throws SQLException {
        Object valor = rs.getObject(coluna);
        return valor instanceof Number number ? number.longValue() : null;
    }

    private static Boolean booleano(ResultSet rs, String coluna) throws SQLException {
        Object valor = rs.getObject(coluna);
        if (valor == null) {
            return null;
        }
        if (valor instanceof Boolean bool) {
            return bool;
        }
        if (valor instanceof Number number) {
            return number.intValue() != 0;
        }
        String texto = valor.toString().trim();
        return "1".equals(texto) || "true".equalsIgnoreCase(texto) || "sim".equalsIgnoreCase(texto);
    }

    private static LocalDate data(ResultSet rs, String coluna) throws SQLException {
        java.sql.Date valor = rs.getDate(coluna);
        return valor != null ? valor.toLocalDate() : null;
    }

    private static LocalDateTime dataHora(ResultSet rs, String coluna) throws SQLException {
        Timestamp valor = rs.getTimestamp(coluna);
        return valor != null ? valor.toLocalDateTime() : null;
    }

    private static OffsetDateTime offset(ResultSet rs, String coluna) throws SQLException {
        LocalDateTime valor = dataHora(rs, coluna);
        return valor != null ? valor.atOffset(OFFSET_BRASILIA) : null;
    }

    private record QueryContext(
            StringBuilder whereBuilder,
            MapSqlParameterSource params,
            FretesViewColumns colunas
    ) {
        String where() {
            return whereBuilder.toString();
        }
    }

    private record FretesViewColumns(List<String> nomes) {
        boolean existe(String nome) {
            return nomes.stream().anyMatch(existente -> existente.equalsIgnoreCase(nome));
        }
    }
}
