package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.dto.performance.PerformanceAgingPointDTO;
import com.dashboard.api.dto.performance.PerformanceDrilldownPointDTO;
import com.dashboard.api.dto.performance.PerformanceEntregaRowDTO;
import com.dashboard.api.dto.performance.PerformanceHistoricoPointDTO;
import com.dashboard.api.dto.performance.PerformanceOverviewDTO;
import com.dashboard.api.dto.performance.PerformanceSerieTemporalPointDTO;
import com.dashboard.api.dto.performance.PerformanceStatusDistribuicaoDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class PerformanceDashboardSqlRepository {

    private static final int TAMANHO_PADRAO = 10;
    private static final int TAMANHO_MAXIMO = 100;
    private static final int DRILLDOWN_LIMITE_PADRAO = 50;
    private static final ZoneId ZONE_ID_BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final BigDecimal MIL = BigDecimal.valueOf(1000);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ValidadorPeriodoService validadorPeriodo;
    private final EscopoFilialService escopoFilialService;
    private volatile PerformanceViewColumns performanceViewColumns;

    public PerformanceDashboardSqlRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            ValidadorPeriodoService validadorPeriodo,
            EscopoFilialService escopoFilialService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.validadorPeriodo = validadorPeriodo;
        this.escopoFilialService = escopoFilialService;
    }

    public PerformanceOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        QueryContext ctx = criarContexto(filtro);
        String sql = ctx.baseCte() + """
                SELECT
                    MAX(data_extracao) AS updated_at,
                    COUNT_BIG(1) AS total_entregas,
                    SUM(CASE WHEN status_norm = N'Finalizada' THEN 1 ELSE 0 END) AS finalizadas,
                    SUM(CASE WHEN status_norm = N'Finalizada' AND performance_diferenca_dias <= 0 THEN 1 ELSE 0 END) AS no_prazo,
                    SUM(CASE WHEN status_norm = N'Finalizada' AND performance_diferenca_dias > 0 THEN 1 ELSE 0 END) AS fora_do_prazo,
                    SUM(CASE WHEN status_norm <> N'Finalizada' AND data_previsao_entrega < :hoje THEN 1 ELSE 0 END) AS em_atraso,
                    COALESCE(SUM(peso_taxado), 0) AS peso_taxado,
                    SUM(CASE WHEN status_norm = N'Finalizada' AND comprovante_anexado = 1 THEN 1 ELSE 0 END) AS finalizadas_com_comprovante,
                    COALESCE(SUM(CASE WHEN status_norm = N'Finalizada' AND comprovante_anexado = 0 THEN valor_nota_fiscal ELSE 0 END), 0) AS valor_nf_sem_comprovante
                FROM entregas
                WHERE 1 = 1
                """ + ctx.where();

        Map<String, Object> row = jdbcTemplate.queryForMap(sql, ctx.params());
        long finalizadas = longo(row, "finalizadas");
        long noPrazo = longo(row, "no_prazo");
        BigDecimal pesoKg = decimal(row, "peso_taxado");

        return new PerformanceOverviewDTO(
                texto(row, "updated_at"),
                longo(row, "total_entregas"),
                finalizadas,
                noPrazo,
                longo(row, "fora_do_prazo"),
                PerformanceMetricasUtils.percentual(noPrazo, finalizadas),
                longo(row, "em_atraso"),
                pesoKg.divide(MIL, 3, RoundingMode.HALF_UP),
                PerformanceMetricasUtils.percentual(longo(row, "finalizadas_com_comprovante"), finalizadas),
                decimal(row, "valor_nf_sem_comprovante").setScale(2, RoundingMode.HALF_UP)
        );
    }

    public List<PerformanceSerieTemporalPointDTO> buscarSerieTemporal(
            FiltroConsultaDTO filtro,
            String nivel,
            Integer ano,
            Integer mes
    ) {
        QueryContext ctx = criarContexto(filtro);
        TemporalQuery temporal = temporalQuery(nivel, ano, mes);
        MapSqlParameterSource params = copiarParams(ctx.params());
        if (temporal.ano() != null) {
            params.addValue("anoTemporal", temporal.ano());
        }
        if (temporal.mes() != null) {
            params.addValue("mesTemporal", temporal.mes());
        }

        String sql = ctx.baseCte() + """
                SELECT
                    CONVERT(char(10), %s, 23) AS date,
                    COUNT_BIG(1) AS total,
                    SUM(CASE WHEN status_norm = N'Finalizada' THEN 1 ELSE 0 END) AS finalizadas,
                    SUM(CASE WHEN status_norm = N'Em Trânsito' THEN 1 ELSE 0 END) AS em_transito,
                    SUM(CASE WHEN status_norm = N'Pendente' THEN 1 ELSE 0 END) AS pendentes,
                    SUM(CASE WHEN status_norm = N'Cancelada' THEN 1 ELSE 0 END) AS canceladas,
                    SUM(CASE WHEN status_norm = N'Em Tratativa' THEN 1 ELSE 0 END) AS em_tratativa
                FROM entregas
                WHERE 1 = 1
                """.formatted(temporal.expressaoData()) + ctx.where() + temporal.where() + """
                GROUP BY %s
                ORDER BY %s
                """.formatted(temporal.expressaoData(), temporal.expressaoData());

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new PerformanceSerieTemporalPointDTO(
                rs.getString("date"),
                rs.getLong("total"),
                rs.getLong("finalizadas"),
                rs.getLong("em_transito"),
                rs.getLong("pendentes"),
                rs.getLong("canceladas"),
                rs.getLong("em_tratativa")
        ));
    }

    public List<PerformanceStatusDistribuicaoDTO> buscarStatus(FiltroConsultaDTO filtro) {
        QueryContext ctx = criarContexto(filtro);
        String sql = ctx.baseCte() + """
                SELECT status_norm AS status, COUNT_BIG(1) AS total
                FROM entregas
                WHERE 1 = 1
                """ + ctx.where() + """
                GROUP BY status_norm
                ORDER BY total DESC, status_norm
                """;

        return jdbcTemplate.query(sql, ctx.params(), (rs, rowNum) -> new PerformanceStatusDistribuicaoDTO(
                rs.getString("status"),
                rs.getLong("total")
        ));
    }

    public List<PerformanceHistoricoPointDTO> buscarHistorico(FiltroConsultaDTO filtro) {
        QueryContext ctx = criarContexto(filtro);
        String sql = ctx.baseCte() + """
                SELECT
                    CONVERT(char(7), data_previsao_entrega, 23) + '-01' AS date,
                    SUM(CASE WHEN status_norm = N'Finalizada' THEN 1 ELSE 0 END) AS finalizadas,
                    SUM(CASE WHEN status_norm = N'Finalizada' AND performance_diferenca_dias <= 0 THEN 1 ELSE 0 END) AS no_prazo
                FROM entregas
                WHERE 1 = 1
                """ + ctx.where() + """
                GROUP BY CONVERT(char(7), data_previsao_entrega, 23)
                ORDER BY date
                """;

        return jdbcTemplate.query(sql, ctx.params(), (rs, rowNum) -> {
            long finalizadas = rs.getLong("finalizadas");
            long noPrazo = rs.getLong("no_prazo");
            return new PerformanceHistoricoPointDTO(
                    rs.getString("date"),
                    PerformanceMetricasUtils.percentual(noPrazo, finalizadas),
                    95.0,
                    finalizadas,
                    noPrazo
            );
        });
    }

    public List<PerformanceDrilldownPointDTO> buscarDrilldown(
            FiltroConsultaDTO filtro,
            String nivel,
            String responsavel,
            String regiaoDestino
    ) {
        QueryContext ctx = criarContexto(filtro);
        String nivelSeguro = normalizarNivel(nivel);
        String campoGrupo = switch (nivelSeguro) {
            case "regiao" -> "regiao_destino";
            case "cidade" -> "cidade_destino";
            default -> "responsavel";
        };
        if ("regiao".equals(nivelSeguro) || "cidade".equals(nivelSeguro)) {
            adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "responsavelDrill", "responsavel", valorOpcional(responsavel));
        }
        if ("cidade".equals(nivelSeguro)) {
            adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "regiaoDrill", "regiao_destino", valorOpcional(regiaoDestino));
        }
        ctx.params().addValue("limiteDrilldown", DRILLDOWN_LIMITE_PADRAO);

        String sql = ctx.baseCte() + """
                SELECT
                    %s AS nome,
                    SUM(CASE WHEN status_norm = N'Finalizada' AND performance_diferenca_dias <= 0 THEN 1 ELSE 0 END) AS no_prazo,
                    SUM(CASE WHEN status_norm = N'Finalizada' AND performance_diferenca_dias > 0 THEN 1 ELSE 0 END) AS fora_do_prazo,
                    SUM(CASE WHEN status_norm <> N'Finalizada' AND data_previsao_entrega < :hoje THEN 1 ELSE 0 END) AS em_atraso,
                    COUNT_BIG(1) AS total
                FROM entregas
                WHERE 1 = 1
                """.formatted(campoGrupo) + ctx.where() + """
                GROUP BY %s
                ORDER BY total DESC, nome
                OFFSET 0 ROWS FETCH NEXT :limiteDrilldown ROWS ONLY
                """.formatted(campoGrupo);

        return jdbcTemplate.query(sql, ctx.params(), (rs, rowNum) -> new PerformanceDrilldownPointDTO(
                rs.getString("nome"),
                nivelSeguro,
                rs.getLong("no_prazo"),
                rs.getLong("fora_do_prazo"),
                rs.getLong("em_atraso"),
                rs.getLong("total")
        ));
    }

    public List<PerformanceAgingPointDTO> buscarAging(FiltroConsultaDTO filtro) {
        QueryContext ctx = criarContexto(filtro);
        String sql = ctx.baseCte() + """
                SELECT bucket, COUNT_BIG(1) AS total
                FROM (
                    SELECT CASE
                        WHEN DATEDIFF(day, data_previsao_entrega, :hoje) <= 2 THEN '0-2 dias'
                        WHEN DATEDIFF(day, data_previsao_entrega, :hoje) <= 5 THEN '3-5 dias'
                        WHEN DATEDIFF(day, data_previsao_entrega, :hoje) <= 10 THEN '6-10 dias'
                        ELSE '11+ dias'
                    END AS bucket
                    FROM entregas
                    WHERE status_norm <> N'Finalizada'
                """ + ctx.where() + """
                ) aging
                GROUP BY bucket
                ORDER BY CASE bucket
                    WHEN '0-2 dias' THEN 1
                    WHEN '3-5 dias' THEN 2
                    WHEN '6-10 dias' THEN 3
                    ELSE 4
                END
                """;

        return jdbcTemplate.query(sql, ctx.params(), (rs, rowNum) -> new PerformanceAgingPointDTO(
                rs.getString("bucket"),
                rs.getLong("total")
        ));
    }

    public PaginaDTO<PerformanceEntregaRowDTO> buscarTabelaPaginada(
            FiltroConsultaDTO filtro,
            int paginaSolicitada,
            int tamanhoSolicitado
    ) {
        QueryContext ctx = criarContexto(filtro);
        int pagina = Math.max(1, paginaSolicitada);
        int tamanho = ConsultaLimiteUtils.limitar(tamanhoSolicitado, TAMANHO_PADRAO, TAMANHO_MAXIMO);
        long offset = (long) (pagina - 1) * tamanho;

        String countSql = ctx.baseCte() + """
                SELECT COUNT_BIG(1)
                FROM entregas
                WHERE 1 = 1
                """ + ctx.where();
        Long total = jdbcTemplate.queryForObject(countSql, ctx.params(), Long.class);
        long totalSeguro = total != null ? total : 0L;
        int totalPaginas = totalSeguro == 0 ? 0 : (int) Math.ceil(totalSeguro / (double) tamanho);

        ctx.params()
                .addValue("offsetTabela", offset)
                .addValue("tamanhoTabela", tamanho);
        String selectSql = ctx.baseCte() + """
                SELECT
                    numero_minuta,
                    status_norm,
                    CONVERT(char(10), data_previsao_entrega, 23) AS data_previsao_entrega,
                    CONVERT(char(10), data_finalizacao, 23) AS data_finalizacao,
                    responsavel_regiao_destino,
                    filial_emissora,
                    regiao_destino,
                    cidade_destino,
                    peso_taxado,
                    valor_nota_fiscal,
                    comprovante_anexado,
                    performance_diferenca_dias
                FROM entregas
                WHERE 1 = 1
                """ + ctx.where() + """
                ORDER BY data_previsao_entrega DESC, numero_minuta DESC
                OFFSET :offsetTabela ROWS FETCH NEXT :tamanhoTabela ROWS ONLY
                """;

        List<PerformanceEntregaRowDTO> conteudo = jdbcTemplate.query(selectSql, ctx.params(), (rs, rowNum) -> {
            Integer diferencaDias = inteiroOuNulo(rs.getObject("performance_diferenca_dias"));
            return new PerformanceEntregaRowDTO(
                    rs.getLong("numero_minuta"),
                    rs.getString("status_norm"),
                    rs.getString("data_previsao_entrega"),
                    rs.getString("data_finalizacao"),
                    rs.getString("responsavel_regiao_destino"),
                    rs.getString("filial_emissora"),
                    rs.getString("regiao_destino"),
                    rs.getString("cidade_destino"),
                    rs.getBigDecimal("peso_taxado"),
                    rs.getBigDecimal("valor_nota_fiscal"),
                    rs.getBoolean("comprovante_anexado"),
                    PerformanceMetricasUtils.performanceStatus(diferencaDias),
                    PerformanceMetricasUtils.performanceStatusDias(diferencaDias)
            );
        });

        return new PaginaDTO<>(conteudo, totalSeguro, totalPaginas, pagina, tamanho);
    }

    private QueryContext criarContexto(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        QueryContext ctx = new QueryContext(new StringBuilder(), new MapSqlParameterSource()
                .addValue("dataInicio", Date.valueOf(filtro.dataInicio()))
                .addValue("dataFim", Date.valueOf(filtro.dataFim().plusDays(1)))
                .addValue("hoje", Date.valueOf(LocalDate.now(ZONE_ID_BRASILIA))),
                carregarColunasPerformance());

        aplicarEscopo(ctx);
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "status", "status_norm", filtro.valores("status"));
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "filiais", "filial_emissora", filtro.valores("filiais"));
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "responsaveis", "responsavel", filtro.valores("responsaveis"));
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "regioesDestino", "regiao_destino", filtro.valores("regioesDestino"));
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "cidadesDestino", "cidade_destino", filtro.valores("cidadesDestino"));
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
        ctx.whereBuilder().append("\n AND LOWER(responsavel) IN (:escopoFiliais)");
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

    private static List<String> valorOpcional(String valor) {
        return valor == null || valor.isBlank() ? List.of() : List.of(valor);
    }

    private static String normalizarNivel(String nivel) {
        if (nivel == null || nivel.isBlank()) {
            return "responsavel";
        }
        String valor = nivel.trim().toLowerCase(Locale.ROOT);
        if ("regiao".equals(valor) || "cidade".equals(valor)) {
            return valor;
        }
        return "responsavel";
    }

    private static TemporalQuery temporalQuery(String nivel, Integer ano, Integer mes) {
        String nivelSeguro = nivel == null || nivel.isBlank() ? "dia" : nivel.trim().toLowerCase(Locale.ROOT);
        StringBuilder where = new StringBuilder();
        Integer anoSeguro = ano != null && ano > 0 ? ano : null;
        Integer mesSeguro = mes != null && mes >= 1 && mes <= 12 ? mes : null;

        if (anoSeguro != null) {
            where.append("\n AND YEAR(data_previsao_entrega) = :anoTemporal");
        }

        return switch (nivelSeguro) {
            case "ano" -> new TemporalQuery("DATEFROMPARTS(YEAR(data_previsao_entrega), 1, 1)", "", null, null);
            case "mes" -> new TemporalQuery(
                    "DATEFROMPARTS(YEAR(data_previsao_entrega), MONTH(data_previsao_entrega), 1)",
                    where.toString(),
                    anoSeguro,
                    null
            );
            default -> {
                if (mesSeguro != null) {
                    where.append("\n AND MONTH(data_previsao_entrega) = :mesTemporal");
                }
                yield new TemporalQuery("data_previsao_entrega", where.toString(), anoSeguro, mesSeguro);
            }
        };
    }

    private PerformanceViewColumns carregarColunasPerformance() {
        PerformanceViewColumns cached = performanceViewColumns;
        if (cached != null) {
            return cached;
        }

        List<String> nomes = jdbcTemplate.queryForList("""
                SELECT c.name
                FROM sys.columns c
                WHERE c.object_id = OBJECT_ID(N'dbo.vw_fretes_powerbi')
                """, new MapSqlParameterSource(), String.class);

        PerformanceViewColumns carregadas = new PerformanceViewColumns(nomes);
        performanceViewColumns = carregadas;
        return carregadas;
    }

    private static String baseCte(PerformanceViewColumns colunas) {
        String responsavelRegiao = textoNullableSql(colunas, "Responsável pela Região de Destino");
        String filialEmissora = textoNullableSql(colunas, "Filial Emissora", "Filial");
        String regiaoDestino = textoComFallbackSql(colunas, "SEM_REGIAO", "Região Destino", "UF Destino");
        String cidadeDestino = textoComFallbackSql(colunas, "SEM_CIDADE", "Cidade Destino", "Destino");
        String comprovanteAnexado = colunas.existe("Comprovante Anexado")
                ? """
                        CASE
                            WHEN UPPER(LTRIM(RTRIM(CONVERT(NVARCHAR(20), [Comprovante Anexado])))) IN (N'SIM', N'TRUE', N'1') THEN 1
                            ELSE 0
                        END
                        """
                : "0";

        return """
                WITH fonte AS (
                    SELECT
                        TRY_CONVERT(BIGINT, [Nº Minuta]) AS numero_minuta,
                        TRY_CONVERT(date, [Previsão de Entrega]) AS data_previsao_entrega,
                        TRY_CONVERT(date, [Data de Finalização]) AS data_finalizacao,
                        %s AS responsavel_regiao_destino,
                        %s AS filial_emissora,
                        COALESCE(%s,
                                 %s,
                                 N'SEM_RESPONSAVEL') AS responsavel,
                        %s AS regiao_destino,
                        %s AS cidade_destino,
                        TRY_CONVERT(DECIMAL(18, 3), [Kg Taxado]) AS peso_taxado,
                        TRY_CONVERT(DECIMAL(18, 2), [Valor NF]) AS valor_nota_fiscal,
                        %s AS comprovante_anexado,
                        CASE
                            WHEN LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status])))) IN (N'finished', N'finalizado', N'finalizada', N'delivered', N'entregue') THEN N'Finalizada'
                            WHEN LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status])))) IN (N'canceled', N'cancelled', N'cancelado', N'cancelada') THEN N'Cancelada'
                            WHEN LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status])))) IN (N'in_transit', N'em trânsito', N'em transito', N'manifested', N'registrado', N'delivering', N'em entrega', N'in_transfer', N'em transferência', N'em transferencia') THEN N'Em Trânsito'
                            WHEN LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status])))) IN (N'occurrence_treatment', N'tratamento de ocorrência', N'tratamento de ocorrencia', N'em tratativa', N'tratativa', N'standby', N'aguardando') THEN N'Em Tratativa'
                            ELSE N'Pendente'
                        END AS status_norm,
                        TRY_CONVERT(datetime2, [Data de extracao]) AS data_extracao,
                        ROW_NUMBER() OVER (
                            PARTITION BY TRY_CONVERT(BIGINT, [Nº Minuta])
                            ORDER BY
                                CASE WHEN [Data de Finalização] IS NOT NULL THEN 0 ELSE 1 END,
                                TRY_CONVERT(datetime2, [Data de extracao]) DESC,
                                TRY_CONVERT(BIGINT, [ID]) DESC
                        ) AS rn
                    FROM dbo.vw_fretes_powerbi
                    WHERE TRY_CONVERT(BIGINT, [Nº Minuta]) IS NOT NULL
                      AND TRY_CONVERT(date, [Previsão de Entrega]) >= :dataInicio
                      AND TRY_CONVERT(date, [Previsão de Entrega]) < :dataFim
                ),
                entregas AS (
                    SELECT
                        numero_minuta,
                        data_previsao_entrega,
                        data_finalizacao,
                        responsavel_regiao_destino,
                        filial_emissora,
                        responsavel,
                        regiao_destino,
                        cidade_destino,
                        COALESCE(peso_taxado, 0) AS peso_taxado,
                        COALESCE(valor_nota_fiscal, 0) AS valor_nota_fiscal,
                        comprovante_anexado,
                        status_norm,
                        data_extracao,
                        CASE
                            WHEN data_finalizacao IS NULL THEN NULL
                            ELSE DATEDIFF(day, data_previsao_entrega, data_finalizacao)
                        END AS performance_diferenca_dias
                    FROM fonte
                    WHERE rn = 1
                )
                """.formatted(
                responsavelRegiao,
                filialEmissora,
                responsavelRegiao,
                filialEmissora,
                regiaoDestino,
                cidadeDestino,
                comprovanteAnexado
        );
    }

    private static String textoNullableSql(PerformanceViewColumns colunas, String... nomes) {
        List<String> expressoes = List.of(nomes).stream()
                .filter(colunas::existe)
                .map(PerformanceDashboardSqlRepository::textoNullableColunaSql)
                .toList();
        if (expressoes.isEmpty()) {
            return "NULL";
        }
        if (expressoes.size() == 1) {
            return expressoes.get(0);
        }
        return "COALESCE(" + String.join(", ", expressoes) + ")";
    }

    private static String textoComFallbackSql(PerformanceViewColumns colunas, String fallback, String... nomes) {
        String texto = textoNullableSql(colunas, nomes);
        return "COALESCE(" + texto + ", N'" + fallback.replace("'", "''") + "')";
    }

    private static String textoNullableColunaSql(String nome) {
        return "NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [" + nome + "]))), '')";
    }

    private static String texto(Map<String, Object> row, String chave) {
        Object valor = row.get(chave);
        return valor == null ? null : String.valueOf(valor);
    }

    private static long longo(Map<String, Object> row, String chave) {
        Object valor = row.get(chave);
        return valor instanceof Number number ? number.longValue() : 0L;
    }

    private static BigDecimal decimal(Map<String, Object> row, String chave) {
        Object valor = row.get(chave);
        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }
        if (valor instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private static Integer inteiroOuNulo(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(valor));
    }

    private static MapSqlParameterSource copiarParams(MapSqlParameterSource params) {
        return new MapSqlParameterSource(params.getValues());
    }

    private record QueryContext(
            StringBuilder whereBuilder,
            MapSqlParameterSource params,
            PerformanceViewColumns colunas
    ) {
        String where() {
            return whereBuilder.toString();
        }

        String baseCte() {
            return PerformanceDashboardSqlRepository.baseCte(colunas);
        }
    }

    private record PerformanceViewColumns(List<String> nomes) {
        boolean existe(String nome) {
            return nomes.contains(nome);
        }
    }

    private record TemporalQuery(String expressaoData, String where, Integer ano, Integer mes) {
    }
}
