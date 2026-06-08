package com.dashboard.api.repository;

import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.dto.manifestos.ManifestosPerformanceDTO;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.dto.manifestos.ManifestosPerformanceDTO.CustoMotoristaDTO;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.dto.manifestos.ManifestosPerformanceDTO.GaugeMetricDTO;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.dto.manifestos.ManifestosPerformanceDTO.KpisManifestosDTO;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.dto.manifestos.ManifestosPerformanceDTO.StatusSazonalDTO;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.dto.manifestos.ManifestosPerformanceDTO.TipoVeiculoDTO;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.service.ValidadorPeriodoService;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.util.PeriodoOffsetDateTimeHelper;
import com.dashboard.api.util.TemporalJsonUtils;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.math.BigDecimal;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.math.RoundingMode;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.sql.Date;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.time.LocalDate;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.util.Collection;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.util.LinkedHashMap;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.util.List;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.util.Locale;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.util.Map;
import com.dashboard.api.util.JanelaOffsetDateTime;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import com.dashboard.api.util.JanelaOffsetDateTime;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import com.dashboard.api.util.JanelaOffsetDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class ManifestosPerformanceSqlRepository {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final GaugeMetricDTO GAUGE_ZERADO = new GaugeMetricDTO(
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ValidadorPeriodoService validadorPeriodo;
    private final EscopoFilialService escopoFilialService;
    private final PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper;
    private volatile ManifestosViewColumns manifestosViewColumns;

    public ManifestosPerformanceSqlRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            ValidadorPeriodoService validadorPeriodo,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.validadorPeriodo = validadorPeriodo;
        this.escopoFilialService = escopoFilialService;
        this.periodoOffsetDateTimeHelper = periodoOffsetDateTimeHelper;
    }

    public ManifestosPerformanceDTO buscarPerformance(
            FiltroConsultaDTO filtro,
            String nivel,
            Integer ano,
            Integer mes
    ) {
        QueryContext ctx = criarContexto(filtro);
        Map<String, Object> overview = jdbcTemplate.queryForMap(sqlOverview(ctx), ctx.params());

        KpisManifestosDTO kpis = new KpisManifestosDTO(
                longo(overview, "total_manifestos"),
                longo(overview, "em_transito"),
                longo(overview, "pendentes"),
                longo(overview, "encerrados"),
                escala(decimal(overview, "km_total"), 2),
                escala(decimal(overview, "custo_total"), 2),
                escala(decimal(overview, "custo_por_km"), 2),
                escala(decimal(overview, "receita_por_km"), 2)
        );

        return new ManifestosPerformanceDTO(
                TemporalJsonUtils.garantirIsoComOffset(texto(overview, "updated_at")),
                kpis,
                buscarGauge(ctx, "remuneracao"),
              buscarGauge(ctx, "aproveitamento"),
              buscarGauge(ctx, "efetividade"),
              buscarStatusSazonal(ctx, nivel, ano, mes),
              buscarCustosMotorista(ctx),
              buscarTiposVeiculo(ctx)
        );
    }

    private GaugeMetricDTO buscarGauge(QueryContext ctx, String metrica) {
        List<GaugeBucket> buckets = jdbcTemplate.query(sqlGauge(ctx, metrica), ctx.params(), (rs, rowNum) -> new GaugeBucket(
                rs.getString("bucket"),
                escala(rs.getBigDecimal("percentual"), 2)
        ));

        if (buckets.isEmpty()) {
            return GAUGE_ZERADO;
        }

        return new GaugeMetricDTO(
                valorBucket(buckets, "global"),
                valorBucket(buckets, "distribuicao"),
                valorBucket(buckets, "transferencia"),
                valorBucket(buckets, "cargaFechada")
        );
    }

    private List<StatusSazonalDTO> buscarStatusSazonal(QueryContext ctx, String nivel, Integer ano, Integer mes) {
        TemporalQuery temporal = temporalQuery(nivel, ano, mes, ctx.periodoInicio(), ctx.periodoFim());
        MapSqlParameterSource params = copiarParams(ctx.params());
        temporal.parametros().forEach(params::addValue);

        String sql = ctx.baseCte() + """
                SELECT
                    CONVERT(char(10), %s, 23) AS data,
                    SUM(CASE WHEN status_norm = N'Encerrado' THEN 1 ELSE 0 END) AS encerrado,
                    SUM(CASE WHEN status_norm = N'Em Trânsito' THEN 1 ELSE 0 END) AS em_transito,
                    SUM(CASE WHEN status_norm = N'Pendente' THEN 1 ELSE 0 END) AS pendente
                FROM manifestos
                WHERE 1 = 1
                """.formatted(temporal.expressaoData()) + ctx.where() + temporal.where() + """
                GROUP BY %s
                ORDER BY %s
                """.formatted(temporal.expressaoData(), temporal.expressaoData());

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new StatusSazonalDTO(
                rs.getString("data"),
                rs.getLong("encerrado"),
                rs.getLong("em_transito"),
                rs.getLong("pendente")
        ));
    }

    private List<CustoMotoristaDTO> buscarCustosMotorista(QueryContext ctx) {
        String sql = ctx.baseCte() + """
                SELECT
                    tipo_motorista AS tipo,
                    COALESCE(SUM(custo_total), 0) AS custo
                FROM manifestos
                WHERE 1 = 1
                """ + ctx.where() + """
                GROUP BY tipo_motorista
                HAVING COALESCE(SUM(custo_total), 0) > 0
                ORDER BY custo DESC, tipo_motorista
                """;

        return jdbcTemplate.query(sql, ctx.params(), (rs, rowNum) -> new CustoMotoristaDTO(
                rs.getString("tipo"),
                escala(rs.getBigDecimal("custo"), 2)
        ));
    }

    private List<TipoVeiculoDTO> buscarTiposVeiculo(QueryContext ctx) {
        String sql = ctx.baseCte() + """
                SELECT
                    tipo_veiculo AS tipo,
                    COUNT(DISTINCT numero) AS quantidade
                FROM manifestos
                WHERE 1 = 1
                """ + ctx.where() + """
                GROUP BY tipo_veiculo
                ORDER BY quantidade DESC, tipo_veiculo
                """;

        return jdbcTemplate.query(sql, ctx.params(), (rs, rowNum) -> new TipoVeiculoDTO(
                rs.getString("tipo"),
                rs.getLong("quantidade")
        ));
    }

    private String sqlOverview(QueryContext ctx) {
        return ctx.baseCte() + """
                SELECT
                    CONVERT(varchar(19), MAX(data_extracao), 126) AS updated_at,
                    COUNT_BIG(1) AS total_manifestos,
                    SUM(CASE WHEN status_norm = N'Em Trânsito' THEN 1 ELSE 0 END) AS em_transito,
                    SUM(CASE WHEN status_norm = N'Pendente' THEN 1 ELSE 0 END) AS pendentes,
                    SUM(CASE WHEN status_norm = N'Encerrado' THEN 1 ELSE 0 END) AS encerrados,
                    COALESCE(SUM(km_total), 0) AS km_total,
                    COALESCE(SUM(custo_total), 0) AS custo_total,
                    CASE
                        WHEN COALESCE(SUM(km_total), 0) > 0
                        THEN COALESCE(SUM(custo_total), 0) / NULLIF(SUM(km_total), 0)
                        ELSE 0
                    END AS custo_por_km,
                    CASE
                        WHEN COALESCE(SUM(km_total), 0) > 0
                        THEN COALESCE(SUM(receita_total), 0) / NULLIF(SUM(km_total), 0)
                        ELSE 0
                    END AS receita_por_km
                FROM manifestos
                WHERE 1 = 1
                """ + ctx.where();
    }

    private String sqlGauge(QueryContext ctx, String metrica) {
        FormulaGauge formula = formulaGauge(metrica);
        return ctx.baseCte() + """
                SELECT bucket, percentual
                FROM (
                    SELECT
                        N'global' AS bucket,
                        CASE
                            WHEN COALESCE(SUM(%s), 0) > 0
                            THEN COALESCE(SUM(%s), 0) * 100.0 / NULLIF(SUM(%s), 0)
                            ELSE 0
                        END AS percentual
                    FROM manifestos
                    WHERE 1 = 1
                    %s
                    UNION ALL
                    SELECT
                        classificacao_bucket AS bucket,
                        CASE
                            WHEN COALESCE(SUM(%s), 0) > 0
                            THEN COALESCE(SUM(%s), 0) * 100.0 / NULLIF(SUM(%s), 0)
                            ELSE 0
                        END AS percentual
                    FROM manifestos
                    WHERE 1 = 1
                    %s
                    GROUP BY classificacao_bucket
                ) gauge
                """.formatted(
                formula.denominador(), formula.numerador(), formula.denominador(), ctx.where(),
                formula.denominador(), formula.numerador(), formula.denominador(), ctx.where()
        );
    }

    private QueryContext criarContexto(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
        QueryContext ctx = new QueryContext(new StringBuilder(), new MapSqlParameterSource()
                .addValue("inicioOffset", janela.inicioInclusivo())
                .addValue("fimOffset", janela.fimExclusivo()),
                carregarColunasManifestos(),
                filtro.dataInicio(),
                filtro.dataFim());

        aplicarEscopo(ctx);
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "filiais", "filial", filtro.valores("filiais"));
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "status", "status_norm", filtro.valores("status"));
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "motoristas", "motorista", filtro.valores("motoristas"));
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "veiculos", "veiculo_placa", filtro.valores("veiculos"));
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "tiposCarga", "tipo_carga", filtro.valores("tiposCarga"));
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "tiposContrato", "tipo_contrato", filtro.valores("tiposContrato"));
        adicionarFiltroTexto(ctx.whereBuilder(), ctx.params(), "tipoMotorista", "tipo_motorista", filtro.valores("tipoMotorista"));
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
        ctx.whereBuilder().append("\n AND LOWER(filial) IN (:escopoFiliais)");
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

    private static TemporalQuery temporalQuery(
            String nivel,
            Integer ano,
            Integer mes,
            LocalDate periodoInicio,
            LocalDate periodoFim
    ) {
        String nivelSeguro = nivel == null || nivel.isBlank() ? "dia" : nivel.trim().toLowerCase(Locale.ROOT);
        StringBuilder where = new StringBuilder();
        Map<String, Object> parametros = new LinkedHashMap<>();
        Integer anoSeguro = ano != null && ano > 0 ? ano : null;
        Integer mesSeguro = mes != null && mes >= 1 && mes <= 12 ? mes : null;

        if ("ano".equals(nivelSeguro)) {
            return new TemporalQuery("DATEFROMPARTS(YEAR(data_criacao_date), 1, 1)", "", Map.of());
        }

        if ("mes".equals(nivelSeguro)) {
            if (anoSeguro != null) {
                adicionarIntervaloTemporal(
                        where,
                        parametros,
                        "data_criacao_date",
                        "inicioTemporal",
                        "fimTemporal",
                        LocalDate.of(anoSeguro, 1, 1),
                        LocalDate.of(anoSeguro + 1, 1, 1)
                );
            }
            return new TemporalQuery(
                    "DATEFROMPARTS(YEAR(data_criacao_date), MONTH(data_criacao_date), 1)",
                    where.toString(),
                    parametros
            );
        }

        if (anoSeguro != null && mesSeguro != null) {
            LocalDate inicio = LocalDate.of(anoSeguro, mesSeguro, 1);
            adicionarIntervaloTemporal(
                    where,
                    parametros,
                    "data_criacao_date",
                    "inicioTemporal",
                    "fimTemporal",
                    inicio,
                    inicio.plusMonths(1)
            );
        } else if (anoSeguro != null) {
            adicionarIntervaloTemporal(
                    where,
                    parametros,
                    "data_criacao_date",
                    "inicioTemporal",
                    "fimTemporal",
                    LocalDate.of(anoSeguro, 1, 1),
                    LocalDate.of(anoSeguro + 1, 1, 1)
            );
        } else if (mesSeguro != null) {
            adicionarIntervalosMensaisPorAno(where, parametros, "data_criacao_date", mesSeguro, periodoInicio, periodoFim);
        }

        return new TemporalQuery("data_criacao_date", where.toString(), parametros);
    }

    private static void adicionarIntervaloTemporal(
            StringBuilder where,
            Map<String, Object> parametros,
            String coluna,
            String inicioParam,
            String fimParam,
            LocalDate inicio,
            LocalDate fim
    ) {
        parametros.put(inicioParam, Date.valueOf(inicio));
        parametros.put(fimParam, Date.valueOf(fim));
        where.append("\n AND ")
                .append(coluna)
                .append(" >= :")
                .append(inicioParam)
                .append(" AND ")
                .append(coluna)
                .append(" < :")
                .append(fimParam);
    }

    private static void adicionarIntervalosMensaisPorAno(
            StringBuilder where,
            Map<String, Object> parametros,
            String coluna,
            int mes,
            LocalDate periodoInicio,
            LocalDate periodoFim
    ) {
        List<String> predicados = new java.util.ArrayList<>();
        int indice = 0;
        for (int ano = periodoInicio.getYear(); ano <= periodoFim.getYear(); ano++) {
            LocalDate inicio = LocalDate.of(ano, mes, 1);
            LocalDate fim = inicio.plusMonths(1);
            String inicioParam = "inicioTemporal" + indice;
            String fimParam = "fimTemporal" + indice;
            parametros.put(inicioParam, Date.valueOf(inicio));
            parametros.put(fimParam, Date.valueOf(fim));
            predicados.add("(" + coluna + " >= :" + inicioParam + " AND " + coluna + " < :" + fimParam + ")");
            indice++;
        }
        if (!predicados.isEmpty()) {
            where.append("\n AND (").append(String.join(" OR ", predicados)).append(")");
        }
    }

    private static FormulaGauge formulaGauge(String metrica) {
        return switch (metrica) {
            case "aproveitamento" -> new FormulaGauge("peso_taxado", "capacidade_veiculo");
            case "efetividade" -> new FormulaGauge("servicos_finalizados", "servicos_total");
            default -> new FormulaGauge("custo_total", "receita_total");
        };
    }

    private ManifestosViewColumns carregarColunasManifestos() {
        ManifestosViewColumns cached = manifestosViewColumns;
        if (cached != null) {
            return cached;
        }

        ManifestosViewColumns carregadas = carregarColunasViewManifestos();
        if (carregadas.contratoObrigatorioValido()) {
            manifestosViewColumns = carregadas;
        }
        return carregadas;
    }

    private ManifestosViewColumns carregarColunasViewManifestos() {
        List<String> nomes = jdbcTemplate.queryForList("""
                SELECT c.name
                FROM sys.columns c
                WHERE c.object_id = OBJECT_ID(N'dbo.vw_manifestos_powerbi')
                """, new MapSqlParameterSource(), String.class);
        return new ManifestosViewColumns(nomes);
    }

    private static String baseCte(ManifestosViewColumns colunas) {
        String tipoMotorista = tipoMotoristaSql(colunas);

        return """
                WITH manifestos AS (
                    SELECT
                        TRY_CONVERT(BIGINT, [Número]) AS numero,
                        COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), ''), N'Sem filial') AS filial,
                        COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Motorista]))), ''), N'Sem motorista') AS motorista,
                        COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Veículo/Placa]))), ''), N'Sem veículo') AS veiculo_placa,
                        COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Tipo Veículo]))), ''), N'Sem tipo') AS tipo_veiculo,
                        %s AS tipo_motorista,
                        COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Tipo de carga]))), ''), N'Sem tipo') AS tipo_carga,
                        COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Tipo de contrato]))), ''), N'Sem tipo') AS tipo_contrato,
                        CASE
                            WHEN LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status])))) COLLATE Latin1_General_CI_AI IN (N'encerrado', N'closed') THEN N'Encerrado'
                            WHEN LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status])))) COLLATE Latin1_General_CI_AI IN (N'em trânsito', N'em transito', N'in_transit') THEN N'Em Trânsito'
                            WHEN LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status])))) COLLATE Latin1_General_CI_AI IN (N'pendente', N'pending') THEN N'Pendente'
                            ELSE N'Pendente'
                        END AS status_norm,
                        CASE
                            WHEN LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Classificação])))) COLLATE Latin1_General_CI_AI LIKE N'%%distribu%%' THEN N'distribuicao'
                            WHEN LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Classificação])))) COLLATE Latin1_General_CI_AI LIKE N'%%transfer%%' THEN N'transferencia'
                            WHEN LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Classificação])))) COLLATE Latin1_General_CI_AI LIKE N'%%carga%%fechada%%'
                              OR LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Tipo de carga])))) COLLATE Latin1_General_CI_AI LIKE N'%%carga%%fechada%%'
                            THEN N'cargaFechada'
                            ELSE N'distribuicao'
                        END AS classificacao_bucket,
                        [Data criação] AS data_criacao,
                        CAST([Data criação] AS date) AS data_criacao_date,
                        COALESCE(TRY_CONVERT(DECIMAL(18, 2), [KM Total]), 0) AS km_total,
                        COALESCE(TRY_CONVERT(DECIMAL(18, 2), [Custo total]), 0) AS custo_total,
                        COALESCE(TRY_CONVERT(DECIMAL(18, 2), [Fretes/Total]), 0) AS receita_total,
                        COALESCE(TRY_CONVERT(DECIMAL(18, 3), [Total peso taxado]), 0) AS peso_taxado,
                        COALESCE(TRY_CONVERT(DECIMAL(18, 3), [Capacidade Lotação Kg]), 0) AS capacidade_veiculo,
                        COALESCE(TRY_CONVERT(DECIMAL(18, 2), [Itens/Finalizados]), 0) AS servicos_finalizados,
                        COALESCE(TRY_CONVERT(DECIMAL(18, 2), [Itens/Total]), 0) AS servicos_total,
                        TRY_CONVERT(datetime2, [Data de extracao]) AS data_extracao
                    FROM dbo.vw_manifestos_powerbi
                    WHERE [Data criação] >= :inicioOffset
                      AND [Data criação] < :fimOffset
                )
                """.formatted(tipoMotorista);
    }

    private static String tipoMotoristaSql(ManifestosViewColumns colunas) {
        if (colunas.existe("Tipo Motorista")) {
            return "COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Tipo Motorista]))), ''), N'Terceiro / Autônomo')";
        }

        String proprietario = textoNullableSql(colunas, "Proprietário/Nome", "Proprietário");
        String contrato = textoNullableSql(colunas, "Tipo de contrato", "Tipo contrato");
        String proprietarioNormalizado = "UPPER(LTRIM(RTRIM(COALESCE(CONVERT(NVARCHAR(255), " + proprietario + "), N'')))) COLLATE Latin1_General_CI_AI";
        String contratoNormalizado = "LOWER(LTRIM(RTRIM(COALESCE(CONVERT(NVARCHAR(255), " + contrato + "), N'')))) COLLATE Latin1_General_CI_AI";

        return """
                CASE
                    WHEN %s = N'RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' THEN N'Frota Própria'
                    WHEN %s LIKE N'%%agreg%%'
                      OR %s LIKE N'%%exclusiv%%'
                      OR %s LIKE N'%%frota%%agreg%%'
                    THEN N'Agregado'
                    ELSE N'Terceiro / Autônomo'
                END
                """.formatted(
                proprietarioNormalizado,
                contratoNormalizado,
                contratoNormalizado,
                contratoNormalizado
        );
    }

    private static String textoNullableSql(ManifestosViewColumns colunas, String... nomes) {
        List<String> expressoes = List.of(nomes).stream()
                .filter(colunas::existe)
                .map(ManifestosPerformanceSqlRepository::textoNullableColunaSql)
                .toList();
        if (expressoes.isEmpty()) {
            return "NULL";
        }
        if (expressoes.size() == 1) {
            return expressoes.get(0);
        }
        return "COALESCE(" + String.join(", ", expressoes) + ")";
    }

    private static String textoNullableColunaSql(String nome) {
        return "NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [" + nome + "]))), '')";
    }

    private static MapSqlParameterSource copiarParams(MapSqlParameterSource params) {
        return new MapSqlParameterSource(params.getValues());
    }

    private static BigDecimal valorBucket(List<GaugeBucket> buckets, String bucket) {
        return buckets.stream()
                .filter(item -> bucket.equals(item.bucket()))
                .map(GaugeBucket::valor)
                .findFirst()
                .orElse(BigDecimal.ZERO);
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

    private static BigDecimal escala(BigDecimal valor, int escala) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(escala, RoundingMode.HALF_UP);
    }

    private record QueryContext(
            StringBuilder whereBuilder,
            MapSqlParameterSource params,
            ManifestosViewColumns colunas,
            LocalDate periodoInicio,
            LocalDate periodoFim
    ) {
        String where() {
            return whereBuilder.toString();
        }

        String baseCte() {
            return ManifestosPerformanceSqlRepository.baseCte(colunas);
        }
    }

    private record FormulaGauge(String numerador, String denominador) {
    }

    private record GaugeBucket(String bucket, BigDecimal valor) {
    }

    private record TemporalQuery(String expressaoData, String where, Map<String, Object> parametros) {
    }

    private record ManifestosViewColumns(List<String> nomes) {
        boolean existe(String nome) {
            return nomes.contains(nome);
        }

        boolean contratoObrigatorioValido() {
            return existe("Número")
                    && existe("Data criação")
                    && existe("Status")
                    && existe("Classificação")
                    && existe("Filial")
                    && existe("Motorista")
                    && existe("Veículo/Placa")
                    && existe("Tipo Veículo")
                    && existe("Tipo Motorista")
                    && existe("Proprietário/Documento")
                    && existe("KM Total")
                    && existe("Custo total")
                    && existe("Fretes/Total")
                    && existe("Total peso taxado")
                    && existe("Capacidade Lotação Kg")
                    && existe("Itens/Finalizados")
                    && existe("Itens/Total")
                    && existe("Data de extracao");
        }
    }
}
