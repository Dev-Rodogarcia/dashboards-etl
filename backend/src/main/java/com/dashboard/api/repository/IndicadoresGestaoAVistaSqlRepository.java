package com.dashboard.api.repository;

import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.dto.indicadoresgestao.CubagemMercadoriasRowDTO;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.dto.indicadoresgestao.CubagemMercadoriasSeriePointDTO;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasRowDTO;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasSeriePointDTO;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.dto.indicadoresgestao.PerformanceEntregaRowDTO;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.dto.indicadoresgestao.PerformanceEntregaSeriePointDTO;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresRowDTO;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresSeriePointDTO;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.util.PeriodoOffsetDateTimeHelper;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.math.BigDecimal;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.math.RoundingMode;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.sql.ResultSet;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.sql.SQLException;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.time.LocalDate;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.time.LocalDateTime;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.util.Collection;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.util.List;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.util.Locale;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.util.Set;
import com.dashboard.api.util.JanelaOffsetDateTime;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import com.dashboard.api.util.JanelaOffsetDateTime;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import com.dashboard.api.util.JanelaOffsetDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class IndicadoresGestaoAVistaSqlRepository {

    private static final String STATUS_CANCELADO = "cancelado";
    private static final String PERFORMANCE_EM_ABERTO = "EM ABERTO";
    private static final String CLASSIFICACAO_GERAL = "Geral";
    private static final BigDecimal VALOR_MINIMO_OPERACIONAL = new BigDecimal("0.01");
    private static final List<String> DOCUMENTOS_FILIAIS_OPERACIONAIS = List.of(
            "51863654000180",
            "51863654000260",
            "60960473000162",
            "60960473000243",
            "60960473000596",
            "60960473000677",
            "60960473000758",
            "60960473000839",
            "60960473001134",
            "60960473001304",
            "60960473001568"
    );
    private static final List<String> TIPOS_ORDEM_CONFERENCIA = List.of(
            "picking",
            "retorno",
            "recebimento",
            "carregamento",
            "descarregamento"
    );
    private static final String[] FILIAIS_COLETORES_PADRAO = {
            "AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "NHB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "REC - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "RJR - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA"
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper;

    public IndicadoresGestaoAVistaSqlRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.periodoOffsetDateTimeHelper = periodoOffsetDateTimeHelper;
    }

    public PerformanceEntregaResumo buscarPerformanceEntregaResumo(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        QueryContext ctx = contextoLocalDate(filtro, escopo, false);
        String sql = performanceEntregaCte() + """
                SELECT
                    CONVERT(NVARCHAR(30), MAX(data_extracao), 126) AS updated_at,
                    COUNT_BIG(1) AS total_entregas,
                    COALESCE(SUM(CAST(is_no_prazo AS BIGINT)), 0) AS entregas_no_prazo,
                    COALESCE(SUM(CAST(is_fora_prazo AS BIGINT)), 0) AS entregas_fora_do_prazo
                FROM performance
                """;

        return resumoPerformance(jdbcTemplate.queryForMap(sql, ctx.params()));
    }

    public List<PerformanceEntregaSeriePointDTO> buscarPerformanceEntregaSerie(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        QueryContext ctx = contextoLocalDate(filtro, escopo, false);
        String sql = performanceEntregaCte() + """
                SELECT
                    CONVERT(char(10), previsao_entrega, 23) AS date,
                    filial_performance,
                    COUNT_BIG(1) AS total_entregas,
                    COALESCE(SUM(CAST(is_no_prazo AS BIGINT)), 0) AS entregas_no_prazo,
                    COALESCE(SUM(CAST(is_fora_prazo AS BIGINT)), 0) AS entregas_fora_do_prazo
                FROM performance
                GROUP BY previsao_entrega, filial_performance
                ORDER BY previsao_entrega, filial_performance
                """;

        return jdbcTemplate.query(sql, ctx.params(), (rs, rowNum) -> {
            long total = rs.getLong("total_entregas");
            long noPrazo = rs.getLong("entregas_no_prazo");
            return new PerformanceEntregaSeriePointDTO(
                    rs.getString("date"),
                    rs.getString("filial_performance"),
                    inteiro(total),
                    inteiro(noPrazo),
                    inteiro(rs.getLong("entregas_fora_do_prazo")),
                    percentual(noPrazo, total)
            );
        });
    }

    public List<PerformanceEntregaRowDTO> buscarPerformanceEntregaLinhas(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            int offset,
            int limite
    ) {
        QueryContext ctx = contextoLocalDate(filtro, escopo, false);
        ctx.params()
                .addValue("offsetLinhas", Math.max(0, offset))
                .addValue("limiteLinhas", Math.max(1, limite));
        String sql = performanceEntregaLinhasSql("""
                OFFSET :offsetLinhas ROWS FETCH NEXT :limiteLinhas ROWS ONLY
                """);

        return jdbcTemplate.query(sql, ctx.params(), this::mapearPerformanceEntregaLinha);
    }

    public List<PerformanceEntregaRowDTO> buscarPerformanceEntregaExportacao(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        QueryContext ctx = contextoLocalDate(filtro, escopo, false);
        return jdbcTemplate.query(performanceEntregaLinhasSql(""), ctx.params(), this::mapearPerformanceEntregaLinha);
    }

    public long contarPerformanceEntregaLinhas(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        QueryContext ctx = contextoLocalDate(filtro, escopo, false);
        return count(performanceEntregaCte() + "SELECT COUNT_BIG(1) FROM performance", ctx.params());
    }

    public CubagemResumo buscarCubagemResumo(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            Set<String> pagadorDocsExcluidos
    ) {
        QueryContext ctx = contextoLocalDate(filtro, escopo, false);
        adicionarDocsExcluidos(ctx.params(), pagadorDocsExcluidos);
        String sql = cubagemCte() + """
                SELECT
                    CONVERT(NVARCHAR(30), MAX(data_extracao), 126) AS updated_at,
                    COUNT_BIG(1) AS total_fretes,
                    COALESCE(SUM(CAST(is_cubado AS BIGINT)), 0) AS fretes_cubados,
                    COALESCE(SUM(CAST(is_peso_real_informado AS BIGINT)), 0) AS fretes_com_peso_real
                FROM cubagem
                """;

        return resumoCubagem(jdbcTemplate.queryForMap(sql, ctx.params()));
    }

    public List<CubagemMercadoriasSeriePointDTO> buscarCubagemSerie(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            Set<String> pagadorDocsExcluidos
    ) {
        QueryContext ctx = contextoLocalDate(filtro, escopo, false);
        adicionarDocsExcluidos(ctx.params(), pagadorDocsExcluidos);
        String sql = cubagemCte() + """
                SELECT
                    CONVERT(char(10), data_frete_date, 23) AS date,
                    filial,
                    COUNT_BIG(1) AS total_fretes,
                    COALESCE(SUM(CAST(is_cubado AS BIGINT)), 0) AS fretes_cubados
                FROM cubagem
                GROUP BY data_frete_date, filial
                ORDER BY data_frete_date, filial
                """;

        return jdbcTemplate.query(sql, ctx.params(), (rs, rowNum) -> {
            long total = rs.getLong("total_fretes");
            long cubados = rs.getLong("fretes_cubados");
            return new CubagemMercadoriasSeriePointDTO(
                    rs.getString("date"),
                    rs.getString("filial"),
                    inteiro(total),
                    inteiro(cubados),
                    percentual(cubados, total)
            );
        });
    }

    public List<CubagemMercadoriasRowDTO> buscarCubagemLinhas(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            Set<String> pagadorDocsExcluidos,
            int offset,
            int limite
    ) {
        QueryContext ctx = contextoLocalDate(filtro, escopo, false);
        adicionarDocsExcluidos(ctx.params(), pagadorDocsExcluidos);
        ctx.params()
                .addValue("offsetLinhas", Math.max(0, offset))
                .addValue("limiteLinhas", Math.max(1, limite));
        String sql = cubagemLinhasSql("""
                OFFSET :offsetLinhas ROWS FETCH NEXT :limiteLinhas ROWS ONLY
                """);

        return jdbcTemplate.query(sql, ctx.params(), this::mapearCubagemLinha);
    }

    public List<CubagemMercadoriasRowDTO> buscarCubagemExportacao(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            Set<String> pagadorDocsExcluidos
    ) {
        QueryContext ctx = contextoLocalDate(filtro, escopo, false);
        adicionarDocsExcluidos(ctx.params(), pagadorDocsExcluidos);
        return jdbcTemplate.query(cubagemLinhasSql(""), ctx.params(), this::mapearCubagemLinha);
    }

    public long contarCubagemLinhas(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            Set<String> pagadorDocsExcluidos
    ) {
        QueryContext ctx = contextoLocalDate(filtro, escopo, false);
        adicionarDocsExcluidos(ctx.params(), pagadorDocsExcluidos);
        return count(cubagemCte() + "SELECT COUNT_BIG(1) FROM cubagem", ctx.params());
    }

    public IndenizacaoResumo buscarIndenizacaoResumo(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        QueryContext ctx = contextoIndenizacao(filtro, escopo);
        String sql = indenizacaoCte() + """
                SELECT
                    CONVERT(NVARCHAR(30), COALESCE((
                        SELECT MAX(valor)
                        FROM (VALUES
                            (MAX(s.data_extracao)),
                            ((SELECT MAX(updated_at) FROM faturamento_periodo))
                        ) atualizacoes(valor)
                    ), SYSDATETIME()), 126) AS updated_at,
                    COUNT_BIG(1) AS total_sinistros,
                    COALESCE(SUM(s.valor_a_pagar_cliente), 0) AS valor_indenizado_original,
                    COALESCE(SUM(ABS(s.valor_a_pagar_cliente)), 0) AS valor_indenizado_abs,
                    COALESCE((SELECT SUM(faturamento) FROM faturamento_periodo), 0) AS faturamento_base
                FROM sinistros s
                """;

        return resumoIndenizacao(jdbcTemplate.queryForMap(sql, ctx.params()));
    }

    public List<IndenizacaoMercadoriasSeriePointDTO> buscarIndenizacaoSerie(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        QueryContext ctx = contextoIndenizacao(filtro, escopo);
        String sql = indenizacaoCte() + """
                , sinistros_mensais AS (
                    SELECT
                        DATEFROMPARTS(YEAR(data_abertura), MONTH(data_abertura), 1) AS mes_ref,
                        filial,
                        COUNT_BIG(1) AS total_sinistros,
                        COALESCE(SUM(valor_a_pagar_cliente), 0) AS valor_indenizado_original,
                        COALESCE(SUM(ABS(valor_a_pagar_cliente)), 0) AS valor_indenizado_abs
                    FROM sinistros
                    GROUP BY DATEFROMPARTS(YEAR(data_abertura), MONTH(data_abertura), 1), filial
                )
                SELECT
                    CONVERT(char(10), sm.mes_ref, 23) AS date,
                    sm.filial,
                    sm.total_sinistros,
                    sm.valor_indenizado_original,
                    sm.valor_indenizado_abs,
                    COALESCE(fm.faturamento, 0) AS faturamento_base,
                    COALESCE(fp.faturamento, 0) AS faturamento_periodo_filial
                FROM sinistros_mensais sm
                LEFT JOIN faturamento_mensal fm
                  ON fm.mes_ref = sm.mes_ref
                 AND fm.filial = sm.filial
                LEFT JOIN faturamento_periodo fp
                  ON fp.filial = sm.filial
                ORDER BY sm.mes_ref, sm.filial
                """;

        return jdbcTemplate.query(sql, ctx.params(), (rs, rowNum) -> {
            BigDecimal valorAbs = decimal(rs, "valor_indenizado_abs");
            BigDecimal faturamentoBase = decimal(rs, "faturamento_base");
            return new IndenizacaoMercadoriasSeriePointDTO(
                    rs.getString("date"),
                    rs.getString("filial"),
                    inteiro(rs.getLong("total_sinistros")),
                    escala(decimal(rs, "valor_indenizado_original"), 2),
                    escala(valorAbs, 2),
                    escala(faturamentoBase, 2),
                    escala(decimal(rs, "faturamento_periodo_filial"), 2),
                    percentual(valorAbs, faturamentoBase)
            );
        });
    }

    public List<IndenizacaoMercadoriasRowDTO> buscarIndenizacaoLinhas(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            int offset,
            int limite
    ) {
        QueryContext ctx = contextoIndenizacao(filtro, escopo);
        ctx.params()
                .addValue("offsetLinhas", Math.max(0, offset))
                .addValue("limiteLinhas", Math.max(1, limite));
        String sql = indenizacaoLinhasSql("""
                OFFSET :offsetLinhas ROWS FETCH NEXT :limiteLinhas ROWS ONLY
                """);

        return jdbcTemplate.query(sql, ctx.params(), this::mapearIndenizacaoLinha);
    }

    public List<IndenizacaoMercadoriasRowDTO> buscarIndenizacaoExportacao(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        QueryContext ctx = contextoIndenizacao(filtro, escopo);
        return jdbcTemplate.query(indenizacaoLinhasSql(""), ctx.params(), this::mapearIndenizacaoLinha);
    }

    public long contarIndenizacaoLinhas(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        QueryContext ctx = contextoIndenizacao(filtro, escopo);
        return count(indenizacaoCte() + "SELECT COUNT_BIG(1) FROM sinistros", ctx.params());
    }

    public UtilizacaoColetoresResumo buscarUtilizacaoColetoresResumo(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        QueryContext ctx = contextoColetores(filtro, escopo);
        String sql = coletoresPontosCte() + """
                SELECT
                    CONVERT(NVARCHAR(30), MAX(updated_at), 126) AS updated_at,
                    COALESCE(SUM(manifestos_bipados), 0) AS manifestos_bipados,
                    COALESCE(SUM(manifestos_emitidos), 0) AS manifestos_emitidos,
                    COALESCE(SUM(manifestos_descarregamento), 0) AS manifestos_descarregamento,
                    COALESCE(SUM(manifestos_incompletos), 0) AS manifestos_incompletos
                FROM pontos_agregados
                """;

        return resumoColetores(jdbcTemplate.queryForMap(sql, ctx.params()));
    }

    public List<UtilizacaoColetoresSeriePointDTO> buscarUtilizacaoColetoresSerie(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        QueryContext ctx = contextoColetores(filtro, escopo);
        String sql = coletoresPontosCte() + """
                SELECT
                    CONVERT(char(10), data, 23) AS date,
                    filial,
                    classificacao,
                    manifestos_bipados,
                    manifestos_emitidos,
                    manifestos_descarregamento,
                    manifestos_incompletos
                FROM pontos_agregados
                ORDER BY data, filial, classificacao
                """;

        return jdbcTemplate.query(sql, ctx.params(), (rs, rowNum) -> {
            long bipados = rs.getLong("manifestos_bipados");
            long emitidos = rs.getLong("manifestos_emitidos");
            long descarga = rs.getLong("manifestos_descarregamento");
            long total = emitidos + descarga;
            return new UtilizacaoColetoresSeriePointDTO(
                    rs.getString("date"),
                    rs.getString("filial"),
                    rs.getString("classificacao"),
                    inteiro(bipados),
                    inteiro(emitidos),
                    inteiro(descarga),
                    inteiro(total),
                    inteiro(rs.getLong("manifestos_incompletos")),
                    percentual(bipados, total)
            );
        });
    }

    public List<UtilizacaoColetoresRankingBase> buscarUtilizacaoColetoresRanking(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        QueryContext ctx = contextoColetores(filtro, escopo);
        String sql = coletoresPontosCte() + """
                SELECT
                    filial,
                    COALESCE(SUM(manifestos_bipados), 0) AS manifestos_bipados,
                    COALESCE(SUM(manifestos_emitidos), 0) AS manifestos_emitidos,
                    COALESCE(SUM(manifestos_descarregamento), 0) AS manifestos_descarregamento,
                    COALESCE(SUM(manifestos_incompletos), 0) AS manifestos_incompletos
                FROM pontos_agregados
                GROUP BY filial
                ORDER BY filial
                """;

        return jdbcTemplate.query(sql, ctx.params(), (rs, rowNum) -> new UtilizacaoColetoresRankingBase(
                rs.getString("filial"),
                inteiro(rs.getLong("manifestos_bipados")),
                inteiro(rs.getLong("manifestos_emitidos")),
                inteiro(rs.getLong("manifestos_descarregamento")),
                inteiro(rs.getLong("manifestos_incompletos"))
        ));
    }

    public List<UtilizacaoColetoresRowDTO> buscarUtilizacaoColetoresLinhas(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            int offset,
            int limite
    ) {
        QueryContext ctx = contextoColetores(filtro, escopo);
        ctx.params()
                .addValue("offsetLinhas", Math.max(0, offset))
                .addValue("limiteLinhas", Math.max(1, limite));
        String sql = coletoresLinhasSql("""
                OFFSET :offsetLinhas ROWS FETCH NEXT :limiteLinhas ROWS ONLY
                """);

        return jdbcTemplate.query(sql, ctx.params(), this::mapearColetoresLinha);
    }

    public List<UtilizacaoColetoresRowDTO> buscarUtilizacaoColetoresExportacao(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        QueryContext ctx = contextoColetores(filtro, escopo);
        return jdbcTemplate.query(coletoresLinhasSql(""), ctx.params(), this::mapearColetoresLinha);
    }

    public long contarUtilizacaoColetoresLinhas(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        QueryContext ctx = contextoColetores(filtro, escopo);
        return count(coletoresPontosCte() + "SELECT COUNT_BIG(1) FROM pontos_agregados", ctx.params());
    }

    private String performanceEntregaLinhasSql(String paginacao) {
        return performanceEntregaCte() + """
                SELECT
                    numero_minuta,
                    CONVERT(NVARCHAR(48), data_frete, 127) AS data_frete,
                    filial_performance,
                    filial_emissora,
                    CONVERT(char(10), previsao_entrega, 23) + 'T00:00:00' AS previsao_entrega,
                    CASE
                        WHEN data_finalizacao IS NULL THEN NULL
                        ELSE CONVERT(char(10), data_finalizacao, 23) + 'T00:00:00'
                    END AS data_finalizacao,
                    performance_diferenca_dias,
                    performance_status
                FROM performance
                ORDER BY previsao_entrega DESC, data_frete DESC, data_finalizacao DESC, numero_minuta DESC
                """ + paginacao;
    }

    private String cubagemLinhasSql(String paginacao) {
        return cubagemCte() + """
                SELECT
                    numero_minuta,
                    CONVERT(NVARCHAR(48), data_frete, 127) AS data_frete,
                    filial,
                    pagador,
                    pagador_documento,
                    destino,
                    COALESCE(peso_taxado, 0) AS peso_taxado,
                    COALESCE(peso_real, 0) AS peso_real,
                    COALESCE(peso_cubado, 0) AS peso_cubado,
                    COALESCE(total_m3, 0) AS total_m3,
                    CAST(is_cubado AS INT) AS cubado
                FROM cubagem
                ORDER BY data_frete DESC, numero_minuta DESC
                """ + paginacao;
    }

    private String indenizacaoLinhasSql(String paginacao) {
        return indenizacaoCte() + """
                SELECT
                    s.numero_sinistro,
                    CONVERT(char(10), s.data_abertura, 23) + 'T00:00:00' AS data_abertura,
                    s.filial,
                    s.minuta,
                    s.valor_a_pagar_cliente,
                    ABS(s.valor_a_pagar_cliente) AS valor_a_pagar_cliente_abs,
                    s.causa_raiz,
                    s.solucao,
                    COALESCE(fp.faturamento, 0) AS faturamento_periodo_filial
                FROM sinistros s
                LEFT JOIN faturamento_periodo fp
                  ON fp.filial = s.filial
                ORDER BY s.data_abertura DESC, ABS(s.valor_a_pagar_cliente) DESC, s.numero_sinistro DESC
                """ + paginacao;
    }

    private String coletoresLinhasSql(String paginacao) {
        return coletoresPontosCte() + """
                SELECT
                    CONVERT(char(10), data, 23) + '|' + filial + '|' + LOWER(classificacao) AS chave,
                    CONVERT(char(10), data, 23) AS date,
                    filial,
                    classificacao,
                    manifestos_bipados,
                    manifestos_emitidos,
                    manifestos_descarregamento,
                    manifestos_incompletos
                FROM pontos_agregados
                ORDER BY data DESC, filial, classificacao
                """ + paginacao;
    }

    private static String performanceEntregaCte() {
        return """
                WITH performance AS (
                    SELECT
                        numero_minuta,
                        data_frete,
                        data_referencia AS previsao_entrega,
                        data_finalizacao_performance AS data_finalizacao,
                        filial_performance,
                        filial_emissora,
                        filial_performance_key,
                        performance_diferenca_dias,
                        performance_status,
                        is_no_prazo,
                        is_fora_prazo,
                        data_extracao
                    FROM [ETL_SISTEMA].dbo.fato_gestao_vista_fretes
                    WHERE indicador_codigo = 'PE'
                      AND data_referencia >= :dataInicio
                      AND data_referencia < :dataFimExclusivo
                      AND is_linha_valida_indicador = 1
                      AND excluido_na_origem = 0
                      AND (:escopoFiliaisVazio = 1 OR filial_performance_key IN (:escopoFiliais))
                      AND (:filiaisVazio = 1 OR filial_performance_key IN (:filiais))
                )
                """;
    }

    private static String cubagemCte() {
        return """
                WITH cubagem AS (
                    SELECT
                        numero_minuta,
                        data_frete,
                        data_referencia AS data_frete_date,
                        COALESCE(filial_emissora, N'Filial nao informada') AS filial,
                        filial_emissora_key AS filial_key,
                        COALESCE(pagador_nome, N'Pagador nao informado') AS pagador,
                        COALESCE(pagador_documento_key, pagador_documento) AS pagador_documento,
                        destino,
                        peso_taxado,
                        peso_real,
                        peso_cubado,
                        total_m3,
                        is_cubado,
                        is_peso_real_informado,
                        data_extracao
                    FROM [ETL_SISTEMA].dbo.fato_gestao_vista_fretes
                    WHERE indicador_codigo = 'CB'
                      AND data_referencia >= :dataInicio
                      AND data_referencia < :dataFimExclusivo
                      AND is_linha_valida_indicador = 1
                      AND is_pagador_excluido_cubagem = 0
                      AND excluido_na_origem = 0
                      AND (:escopoFiliaisVazio = 1 OR filial_emissora_key IN (:escopoFiliais))
                      AND (:filiaisVazio = 1 OR filial_emissora_key IN (:filiais))
                )
                """;
    }

    private static String indenizacaoCte() {
        return fretesOperacionaisCte("""
                  AND [Data frete] >= :inicioOffset
                  AND [Data frete] < :fimOffset
                """) + """
                , fretes_faturamento AS (
                    SELECT
                        filial,
                        filial_key,
                        DATEFROMPARTS(YEAR(data_frete_date), MONTH(data_frete_date), 1) AS mes_ref,
                        valor_total,
                        data_extracao
                    FROM fretes_fonte
                    WHERE data_frete_date IS NOT NULL
                      AND filial_key IS NOT NULL
                      AND elegivel_operacional_com_valor = 1
                      AND (:escopoFiliaisVazio = 1 OR filial_key IN (:escopoFiliais))
                      AND (:filiaisVazio = 1 OR filial_key IN (:filiais))
                ),
                faturamento_periodo AS (
                    SELECT
                        filial,
                        SUM(COALESCE(valor_total, 0)) AS faturamento,
                        MAX(data_extracao) AS updated_at
                    FROM fretes_faturamento
                    GROUP BY filial
                ),
                faturamento_mensal AS (
                    SELECT
                        mes_ref,
                        filial,
                        SUM(COALESCE(valor_total, 0)) AS faturamento
                    FROM fretes_faturamento
                    GROUP BY mes_ref, filial
                ),
                sinistros_fonte AS (
                    SELECT
                        TRY_CONVERT(BIGINT, [Nº do Sinistro]) AS numero_sinistro,
                        TRY_CONVERT(date, CONVERT(NVARCHAR(64), [Data abertura])) AS data_abertura,
                        COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Pessoa/Nome fantasia]))), ''), N'Não mapeada') AS filial,
                        LOWER(COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Pessoa/Nome fantasia]))), ''), N'Não mapeada')) AS filial_key,
                        TRY_CONVERT(BIGINT, [Minuta]) AS minuta,
                        COALESCE(TRY_CONVERT(DECIMAL(18, 2), [valor a pagar ao cliente]), 0) AS valor_a_pagar_cliente,
                        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Ocorrência/Descrição]))), '') AS causa_raiz,
                        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Tratativa/Solução]))), '') AS solucao,
                        TRY_CONVERT(datetime2, [Data de extracao]) AS data_extracao
                    FROM dbo.vw_sinistros_powerbi
                    WHERE [Data abertura] >= :dataInicio
                      AND [Data abertura] < :dataFimExclusivo
                ),
                sinistros_deduplicados AS (
                    SELECT *,
                        ROW_NUMBER() OVER (
                            PARTITION BY numero_sinistro
                            ORDER BY data_extracao DESC, numero_sinistro DESC
                        ) AS rn
                    FROM sinistros_fonte
                    WHERE numero_sinistro IS NOT NULL
                      AND data_abertura IS NOT NULL
                ),
                sinistros AS (
                    SELECT *
                    FROM sinistros_deduplicados
                    WHERE rn = 1
                      AND (:escopoFiliaisVazio = 1 OR filial_key IN (:escopoFiliais))
                      AND (:filiaisVazio = 1 OR filial_key IN (:filiais))
                )
                """;
    }

    private static String fretesOperacionaisCte(String whereDataSargavel) {
        return """
                WITH fretes_fonte AS (
                    SELECT
                        TRY_CONVERT(BIGINT, [ID]) AS id,
                        TRY_CONVERT(BIGINT, [Nº Minuta]) AS numero_minuta,
                        TRY_CONVERT(datetimeoffset, CONVERT(NVARCHAR(64), [Data frete])) AS data_frete,
                        TRY_CONVERT(date, CONVERT(NVARCHAR(64), [Data frete])) AS data_frete_date,
                        TRY_CONVERT(date, CONVERT(NVARCHAR(64), [Previsão de Entrega])) AS previsao_entrega,
                        TRY_CONVERT(date, CONVERT(NVARCHAR(64), [Data de Finalização])) AS data_finalizacao,
                        COALESCE(
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Responsável pela Região de Destino]))), ''),
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial Emissora]))), ''),
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), '')
                        ) AS filial_performance,
                        COALESCE(
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial Emissora]))), ''),
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), ''),
                            N'Não informado'
                        ) AS filial,
                        LOWER(COALESCE(
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Responsável pela Região de Destino]))), ''),
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial Emissora]))), ''),
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), '')
                        )) AS filial_performance_key,
                        LOWER(COALESCE(
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial Emissora]))), ''),
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), '')
                        )) AS filial_key,
                        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial Emissora]))), '') AS filial_emissora,
                        LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status]))), '')) AS status_norm,
                        TRY_CONVERT(INT, NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(64), [Performance Diferença de Dias]))), N'')) AS performance_diferenca_dias,
                        UPPER(COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Performance Status]))), ''), N'EM ABERTO')) AS performance_status,
                        TRY_CONVERT(DECIMAL(18, 2), [Valor Total do Serviço]) AS valor_total,
                        TRY_CONVERT(DECIMAL(18, 3), [Kg Taxado]) AS peso_taxado,
                        TRY_CONVERT(DECIMAL(18, 3), [Kg Real]) AS peso_real,
                        TRY_CONVERT(DECIMAL(18, 6), [Kg Cubado]) AS peso_cubado,
                        TRY_CONVERT(DECIMAL(18, 6), [Total M3]) AS total_m3,
                        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Pagador]))), '') AS pagador,
                        %s AS pagador_documento,
                        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Destino]))), '') AS destino,
                        TRY_CONVERT(datetime2, CONVERT(NVARCHAR(64), [Data de extracao])) AS data_extracao,
                        CASE
                            WHEN UPPER(LTRIM(RTRIM(CONVERT(NVARCHAR(20), [Cortesia Flag])))) IN (N'1', N'TRUE', N'SIM') THEN 1
                            ELSE 0
                        END AS cortesia_flag,
                        UPPER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Documento Oficial/Tipo]))), '')) AS documento_oficial_tipo,
                        UPPER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Tipo Frete]))), '')) AS tipo_frete
                    FROM dbo.vw_fretes_powerbi
                    WHERE 1 = 1
                """.formatted(documentoNormalizadoSql("[Pagador Doc]"))
                + whereDataSargavel
                + """
                ),
                fretes_regras AS (
                    SELECT *,
                        CASE
                            WHEN documento_oficial_tipo IN (N'CT-E', N'NFS-E') THEN 1
                            ELSE 0
                        END AS documento_emitido,
                        CASE
                            WHEN cortesia_flag = 1 THEN 0
                            WHEN COALESCE(documento_oficial_tipo, N'') NOT IN (N'CT-E', N'NFS-E')
                             AND pagador_documento IN (:documentosFiliaisOperacionais) THEN 0
                            WHEN COALESCE(documento_oficial_tipo, N'') NOT IN (N'CT-E', N'NFS-E')
                             AND COALESCE(valor_total, 0) <= :valorMinimoOperacional THEN 0
                            WHEN COALESCE(documento_oficial_tipo, N'') NOT IN (N'CT-E', N'NFS-E')
                             AND tipo_frete LIKE N'%SUBSTITUTE%'
                             AND status_norm LIKE N'%pendente%' THEN 0
                            ELSE 1
                        END AS elegivel_operacional,
                        CASE
                            WHEN cortesia_flag = 1 THEN 0
                            WHEN COALESCE(documento_oficial_tipo, N'') NOT IN (N'CT-E', N'NFS-E')
                             AND pagador_documento IN (:documentosFiliaisOperacionais) THEN 0
                            WHEN COALESCE(documento_oficial_tipo, N'') NOT IN (N'CT-E', N'NFS-E')
                             AND COALESCE(valor_total, 0) <= :valorMinimoOperacional THEN 0
                            WHEN COALESCE(documento_oficial_tipo, N'') NOT IN (N'CT-E', N'NFS-E')
                             AND tipo_frete LIKE N'%SUBSTITUTE%'
                             AND status_norm LIKE N'%pendente%' THEN 0
                            WHEN COALESCE(valor_total, 0) <= :valorMinimoOperacional THEN 0
                            ELSE 1
                        END AS elegivel_operacional_com_valor
                    FROM fretes_fonte
                ),
                fretes_deduplicados AS (
                    SELECT *,
                        ROW_NUMBER() OVER (
                            PARTITION BY numero_minuta
                            ORDER BY
                                CASE WHEN data_finalizacao IS NOT NULL THEN 4 ELSE 0 END
                              + CASE WHEN performance_status IS NOT NULL THEN 3 ELSE 0 END
                              + CASE WHEN filial_performance IS NOT NULL THEN 2 ELSE 0 END
                              + CASE WHEN data_extracao IS NOT NULL THEN 1 ELSE 0 END DESC,
                                data_extracao DESC,
                                id DESC
                        ) AS rn
                    FROM fretes_regras
                )
                """;
    }

    private static String coletoresPontosCte() {
        return filiaisAliasCte() + """
                , manifestos_fonte AS (
                    SELECT
                        COALESCE(
                            CONVERT(NVARCHAR(64), TRY_CONVERT(BIGINT, [Número])),
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Identificador Único]))), '')
                        ) AS chave_manifesto,
                        TRY_CONVERT(date, CONVERT(NVARCHAR(64), [Data criação])) AS data,
                        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Local de Descarregamento]))), '') AS local_descarregamento,
                        COALESCE(alias_emitida.filial, filial_raw.valor, N'Filial nao informada') AS filial_emitida,
                        LOWER(COALESCE(alias_emitida.filial, filial_raw.valor, N'Filial nao informada')) COLLATE Latin1_General_CI_AI AS filial_emitida_key,
                        LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Classificação]))), '')) COLLATE Latin1_General_CI_AI AS classificacao_key,
                        TRY_CONVERT(datetime2, [Data de extracao]) AS data_extracao
                    FROM dbo.vw_manifestos_powerbi
                    CROSS APPLY (
                        SELECT COALESCE(
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial Emissora]))), ''),
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), '')
                        ) AS valor
                    ) filial_raw
                    OUTER APPLY (
                        SELECT TOP (1) a.filial
                        FROM filiais_alias a
                        WHERE LOWER(filial_raw.valor) COLLATE Latin1_General_CI_AI = a.alias_key COLLATE Latin1_General_CI_AI
                    ) alias_emitida
                    WHERE [Data criação] >= :inicioOffset
                      AND [Data criação] < :fimOffset
                ),
                manifestos_deduplicados AS (
                    SELECT *,
                        ROW_NUMBER() OVER (
                            PARTITION BY chave_manifesto
                            ORDER BY data_extracao DESC, chave_manifesto
                        ) AS rn
                    FROM manifestos_fonte
                    WHERE chave_manifesto IS NOT NULL
                      AND data IS NOT NULL
                      AND (
                            classificacao_key IS NULL
                         OR classificacao_key NOT LIKE N'carga fechada%'
                        )
                      AND (
                            classificacao_key IS NULL
                         OR classificacao_key NOT LIKE N'acerto de motorista%'
                        )
                      AND (
                            classificacao_key IS NULL
                         OR classificacao_key NOT LIKE N'frete retorno%'
                        )
                      AND (
                            classificacao_key IS NULL
                         OR classificacao_key NOT LIKE N'viagem vazia%'
                        )
                ),
                manifestos AS (
                    SELECT *
                    FROM manifestos_deduplicados
                    WHERE rn = 1
                ),
                manifestos_emitidos AS (
                    SELECT
                        data,
                        filial_emitida AS filial,
                        COUNT_BIG(1) AS manifestos_emitidos,
                        MAX(data_extracao) AS updated_at
                    FROM manifestos
                    WHERE (:escopoFiliaisVazio = 1 OR filial_emitida_key IN (:escopoFiliais))
                      AND (:filiaisVazio = 1 OR filial_emitida_key IN (:filiais))
                    GROUP BY data, filial_emitida
                ),
                descarregamento_partes AS (
                    SELECT
                        m.chave_manifesto,
                        m.data,
                        COALESCE(alias_descarga.filial, parte.valor) AS filial,
                        LOWER(COALESCE(alias_descarga.filial, parte.valor)) COLLATE Latin1_General_CI_AI AS filial_key,
                        m.data_extracao
                    FROM manifestos m
                    CROSS APPLY STRING_SPLIT(
                        REPLACE(REPLACE(REPLACE(COALESCE(m.local_descarregamento, N''), CHAR(13), N';'), CHAR(10), N';'), N',', N';'),
                        N';'
                    ) split
                    CROSS APPLY (
                        SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), split.value))), '') AS valor
                    ) parte
                    OUTER APPLY (
                        SELECT TOP (1) a.filial
                        FROM filiais_alias a
                        WHERE LOWER(parte.valor) COLLATE Latin1_General_CI_AI = a.alias_key COLLATE Latin1_General_CI_AI
                    ) alias_descarga
                    WHERE parte.valor IS NOT NULL
                      AND LOWER(parte.valor) COLLATE Latin1_General_CI_AI <> N'null'
                ),
                descarregamento_elegivel AS (
                    SELECT *,
                        ROW_NUMBER() OVER (
                            PARTITION BY chave_manifesto
                            ORDER BY filial
                        ) AS rn
                    FROM descarregamento_partes
                    WHERE (:escopoFiliaisVazio = 1 OR filial_key IN (:escopoFiliais))
                      AND (:filiaisVazio = 1 OR filial_key IN (:filiais))
                ),
                manifestos_descarregamento AS (
                    SELECT
                        data,
                        filial,
                        COUNT_BIG(1) AS manifestos_descarregamento,
                        MAX(data_extracao) AS updated_at
                    FROM descarregamento_elegivel
                    WHERE rn = 1
                    GROUP BY data, filial
                ),
                ordens_fonte AS (
                    SELECT
                        TRY_CONVERT(BIGINT, [N° Ordem]) AS numero_ordem,
                        TRY_CONVERT(date, CONVERT(NVARCHAR(64), [Data/Hora início])) AS data,
                        CASE WHEN [Data/Hora fim] IS NULL THEN 1 ELSE 0 END AS incompleta,
                        COALESCE(alias_ordem.filial, filial_raw.valor, N'Filial nao informada') AS filial,
                        LOWER(COALESCE(alias_ordem.filial, filial_raw.valor, N'Filial nao informada')) COLLATE Latin1_General_CI_AI AS filial_key,
                        TRY_CONVERT(datetime2, [Data de extracao]) AS data_extracao
                    FROM dbo.vw_inventario_powerbi
                    CROSS APPLY (
                        SELECT COALESCE(
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial da Ordem de Conferência]))), ''),
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), ''),
                            NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial Emissora do Frete]))), '')
                        ) AS valor
                    ) filial_raw
                    OUTER APPLY (
                        SELECT TOP (1) a.filial
                        FROM filiais_alias a
                        WHERE LOWER(filial_raw.valor) COLLATE Latin1_General_CI_AI = a.alias_key COLLATE Latin1_General_CI_AI
                    ) alias_ordem
                    WHERE [Data/Hora início] >= :inicioOffset
                      AND [Data/Hora início] < :fimOffset
                      AND LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Tipo])))) COLLATE Latin1_General_CI_AI IN (:tiposOrdemConferencia)
                ),
                ordens_deduplicadas AS (
                    SELECT *,
                        ROW_NUMBER() OVER (
                            PARTITION BY numero_ordem
                            ORDER BY data_extracao DESC, numero_ordem DESC
                        ) AS rn
                    FROM ordens_fonte
                    WHERE numero_ordem IS NOT NULL
                      AND data IS NOT NULL
                ),
                ordens AS (
                    SELECT *
                    FROM ordens_deduplicadas
                    WHERE rn = 1
                      AND (:escopoFiliaisVazio = 1 OR filial_key IN (:escopoFiliais))
                      AND (:filiaisVazio = 1 OR filial_key IN (:filiais))
                ),
                ordens_agrupadas AS (
                    SELECT
                        data,
                        filial,
                        COUNT_BIG(1) AS manifestos_bipados,
                        SUM(incompleta) AS manifestos_incompletos,
                        MAX(data_extracao) AS updated_at
                    FROM ordens
                    GROUP BY data, filial
                ),
                pontos_union AS (
                    SELECT data, filial, 0 AS manifestos_bipados, manifestos_emitidos, 0 AS manifestos_descarregamento, 0 AS manifestos_incompletos, updated_at
                    FROM manifestos_emitidos
                    UNION ALL
                    SELECT data, filial, 0, 0, manifestos_descarregamento, 0, updated_at
                    FROM manifestos_descarregamento
                    UNION ALL
                    SELECT data, filial, manifestos_bipados, 0, 0, manifestos_incompletos, updated_at
                    FROM ordens_agrupadas
                ),
                pontos_agregados AS (
                    SELECT
                        data,
                        COALESCE(filial, N'Filial nao informada') AS filial,
                        N'Geral' AS classificacao,
                        SUM(manifestos_bipados) AS manifestos_bipados,
                        SUM(manifestos_emitidos) AS manifestos_emitidos,
                        SUM(manifestos_descarregamento) AS manifestos_descarregamento,
                        SUM(manifestos_incompletos) AS manifestos_incompletos,
                        MAX(updated_at) AS updated_at
                    FROM pontos_union
                    GROUP BY data, COALESCE(filial, N'Filial nao informada')
                    HAVING SUM(manifestos_bipados) > 0
                        OR SUM(manifestos_emitidos) > 0
                        OR SUM(manifestos_descarregamento) > 0
                )
                """;
    }

    private static String filiaisAliasCte() {
        return """
                WITH filiais_alias(alias_key, filial) AS (
                    SELECT N'agu', N'AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'agu - rodogarcia transportes rodoviarios ltda', N'AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'tr rodogarcia | agu', N'AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'rodogarcia filial agu', N'AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'cas', N'CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'cas - rodogarcia transportes rodoviarios ltda', N'CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'tr rodogarcia | cas', N'CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'rodogarcia filial cas', N'CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'cpq', N'CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'cpq - rodogarcia transportes rodoviarios ltda', N'CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'tr rodogarcia | cpq', N'CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'rodogarcia filial cpq', N'CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'cwb', N'CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'cwb - rodogarcia transportes rodoviarios ltda', N'CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'tr rodogarcia | cwb', N'CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'rodogarcia filial cwb', N'CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'nhb', N'NHB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'nhb - rodogarcia transportes rodoviarios ltda', N'NHB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'tr rodogarcia | nhb', N'NHB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'rodogarcia filial nhb', N'NHB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'rec', N'REC - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'rec - rodogarcia transportes rodoviarios ltda', N'REC - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'tr rodogarcia | rec', N'REC - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'rodogarcia filial rec', N'REC - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'rjr', N'RJR - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'rjr - rodogarcia transportes rodoviarios ltda', N'RJR - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'tr rodogarcia | rjr', N'RJR - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'rodogarcia filial rjr', N'RJR - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'spo', N'SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'spo - rodogarcia transportes rodoviarios ltda', N'SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'tr rodogarcia | spo', N'SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
                    SELECT N'rodogarcia filial spo', N'SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA'
                )
                """;
    }

    private QueryContext contextoLocalDate(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            boolean coletores
    ) {
        MapSqlParameterSource params = paramsComuns(filtro, escopo, coletores)
                .addValue("dataInicio", filtro.dataInicio())
                .addValue("dataFimExclusivo", filtro.dataFim().plusDays(1));
        return new QueryContext(params);
    }

    private QueryContext contextoOffset(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            boolean coletores
    ) {
        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
        MapSqlParameterSource params = paramsComuns(filtro, escopo, coletores)
                .addValue("inicioOffset", janela.inicioInclusivo())
                .addValue("fimOffset", janela.fimExclusivo());
        return new QueryContext(params);
    }

    private QueryContext contextoIndenizacao(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
        MapSqlParameterSource params = paramsComuns(filtro, escopo, false)
                .addValue("dataInicio", filtro.dataInicio())
                .addValue("dataFimExclusivo", filtro.dataFim().plusDays(1))
                .addValue("inicioOffset", janela.inicioInclusivo())
                .addValue("fimOffset", janela.fimExclusivo());
        return new QueryContext(params);
    }

    private QueryContext contextoColetores(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        QueryContext ctx = contextoOffset(filtro, escopo, true);
        ctx.params().addValue("tiposOrdemConferencia", TIPOS_ORDEM_CONFERENCIA);
        return ctx;
    }

    private MapSqlParameterSource paramsComuns(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            boolean coletores
    ) {
        List<String> escopoFiliais = coletores
                ? normalizarColetores(escopo.filiaisOrdenadas())
                : normalizar(escopo.filiaisOrdenadas());
        List<String> filiaisFiltro = coletores
                ? normalizarColetores(filtro.valores("filiais"))
                : normalizar(filtro.valores("filiais"));

        boolean escopoVazio = escopo.acessoTotal();
        if (escopoFiliais.isEmpty()) {
            escopoFiliais = List.of("__sem_acesso__");
        }
        if (filiaisFiltro.isEmpty()) {
            filiaisFiltro = List.of("__sem_filtro__");
        }

        return new MapSqlParameterSource()
                .addValue("statusCancelado", STATUS_CANCELADO)
                .addValue("performanceEmAberto", PERFORMANCE_EM_ABERTO)
                .addValue("valorMinimoOperacional", VALOR_MINIMO_OPERACIONAL)
                .addValue("documentosFiliaisOperacionais", DOCUMENTOS_FILIAIS_OPERACIONAIS)
                .addValue("escopoFiliais", escopoFiliais)
                .addValue("escopoFiliaisVazio", escopoVazio ? 1 : 0)
                .addValue("filiais", filiaisFiltro)
                .addValue("filiaisVazio", filtro.temFiltro("filiais") ? 0 : 1);
    }

    private static void adicionarDocsExcluidos(MapSqlParameterSource params, Set<String> docs) {
        List<String> normalizados = normalizar(docs);
        params.addValue("docsExcluidos", normalizados.isEmpty() ? List.of("__sem_docs__") : normalizados)
                .addValue("docsExcluidosVazio", normalizados.isEmpty() ? 1 : 0);
    }

    private PerformanceEntregaRowDTO mapearPerformanceEntregaLinha(ResultSet rs, int rowNum) throws SQLException {
        return new PerformanceEntregaRowDTO(
                rs.getLong("numero_minuta"),
                rs.getString("data_frete"),
                rs.getString("filial_performance"),
                rs.getString("filial_emissora"),
                rs.getString("previsao_entrega"),
                rs.getString("data_finalizacao"),
                inteiroOuNulo(rs, "performance_diferenca_dias"),
                rs.getString("performance_status")
        );
    }

    private CubagemMercadoriasRowDTO mapearCubagemLinha(ResultSet rs, int rowNum) throws SQLException {
        return new CubagemMercadoriasRowDTO(
                rs.getLong("numero_minuta"),
                rs.getString("data_frete"),
                rs.getString("filial"),
                rs.getString("pagador"),
                rs.getString("pagador_documento"),
                rs.getString("destino"),
                decimal(rs, "peso_taxado"),
                decimal(rs, "peso_real"),
                decimal(rs, "peso_cubado"),
                decimal(rs, "total_m3"),
                rs.getInt("cubado") == 1
        );
    }

    private IndenizacaoMercadoriasRowDTO mapearIndenizacaoLinha(ResultSet rs, int rowNum) throws SQLException {
        BigDecimal valorAbs = escala(decimal(rs, "valor_a_pagar_cliente_abs"), 2);
        BigDecimal faturamentoFilial = decimal(rs, "faturamento_periodo_filial");
        return new IndenizacaoMercadoriasRowDTO(
                rs.getLong("numero_sinistro"),
                rs.getString("data_abertura"),
                rs.getString("filial"),
                longoOuNulo(rs, "minuta"),
                escala(decimal(rs, "valor_a_pagar_cliente"), 2),
                valorAbs,
                rs.getString("causa_raiz"),
                rs.getString("solucao"),
                percentual(valorAbs, faturamentoFilial)
        );
    }

    private UtilizacaoColetoresRowDTO mapearColetoresLinha(ResultSet rs, int rowNum) throws SQLException {
        long bipados = rs.getLong("manifestos_bipados");
        long emitidos = rs.getLong("manifestos_emitidos");
        long descarga = rs.getLong("manifestos_descarregamento");
        long total = emitidos + descarga;
        return new UtilizacaoColetoresRowDTO(
                rs.getString("chave"),
                rs.getString("date"),
                rs.getString("filial"),
                rs.getString("classificacao"),
                inteiro(bipados),
                inteiro(emitidos),
                inteiro(descarga),
                inteiro(total),
                inteiro(rs.getLong("manifestos_incompletos")),
                percentual(bipados, total)
        );
    }

    private static PerformanceEntregaResumo resumoPerformance(java.util.Map<String, Object> row) {
        return new PerformanceEntregaResumo(
                texto(row, "updated_at"),
                longo(row, "total_entregas"),
                longo(row, "entregas_no_prazo"),
                longo(row, "entregas_fora_do_prazo")
        );
    }

    private static CubagemResumo resumoCubagem(java.util.Map<String, Object> row) {
        return new CubagemResumo(
                texto(row, "updated_at"),
                longo(row, "total_fretes"),
                longo(row, "fretes_cubados"),
                longo(row, "fretes_com_peso_real")
        );
    }

    private static IndenizacaoResumo resumoIndenizacao(java.util.Map<String, Object> row) {
        return new IndenizacaoResumo(
                texto(row, "updated_at"),
                longo(row, "total_sinistros"),
                escala(decimal(row, "valor_indenizado_abs"), 2),
                escala(decimal(row, "valor_indenizado_original"), 2),
                escala(decimal(row, "faturamento_base"), 2)
        );
    }

    private static UtilizacaoColetoresResumo resumoColetores(java.util.Map<String, Object> row) {
        return new UtilizacaoColetoresResumo(
                texto(row, "updated_at"),
                longo(row, "manifestos_bipados"),
                longo(row, "manifestos_emitidos"),
                longo(row, "manifestos_descarregamento"),
                longo(row, "manifestos_incompletos")
        );
    }

    private long count(String sql, MapSqlParameterSource params) {
        Long total = jdbcTemplate.queryForObject(sql, params, Long.class);
        return total != null ? total : 0L;
    }

    private static String documentoNormalizadoSql(String coluna) {
        return "NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), "
                + coluna
                + ")))), N'.', N''), N'/', N''), N'-', N''), N' ', N''), CHAR(9), N''), N'')";
    }

    private static Integer inteiroOuNulo(ResultSet rs, String coluna) throws SQLException {
        int valor = rs.getInt(coluna);
        return rs.wasNull() ? null : valor;
    }

    private static Long longoOuNulo(ResultSet rs, String coluna) throws SQLException {
        long valor = rs.getLong(coluna);
        return rs.wasNull() ? null : valor;
    }

    private static BigDecimal decimal(ResultSet rs, String coluna) throws SQLException {
        BigDecimal valor = rs.getBigDecimal(coluna);
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private static String texto(java.util.Map<String, Object> row, String chave) {
        Object valor = row.get(chave);
        return valor == null ? null : String.valueOf(valor);
    }

    private static long longo(java.util.Map<String, Object> row, String chave) {
        Object valor = row.get(chave);
        return valor instanceof Number number ? number.longValue() : 0L;
    }

    private static BigDecimal decimal(java.util.Map<String, Object> row, String chave) {
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
        return (valor != null ? valor : BigDecimal.ZERO).setScale(escala, RoundingMode.HALF_UP);
    }

    private static int inteiro(long valor) {
        if (valor > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (valor < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) valor;
    }

    private static double percentual(long numerador, long denominador) {
        if (denominador <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf((numerador * 100.0) / denominador)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static double percentual(BigDecimal numerador, BigDecimal denominador) {
        if (numerador == null || denominador == null || denominador.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return numerador.multiply(BigDecimal.valueOf(100))
                .divide(denominador, 3, RoundingMode.HALF_UP)
                .doubleValue();
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

    private static List<String> normalizarColetores(Collection<String> valores) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(IndicadoresGestaoAVistaSqlRepository::canonicalizarFilialColetores)
                .map(valor -> valor.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private static String canonicalizarFilialColetores(String filial) {
        if (filial == null || filial.isBlank()) {
            return filial;
        }
        String valor = filial.trim();
        String normalizado = valor.toLowerCase(Locale.ROOT);
        for (String canonica : FILIAIS_COLETORES_PADRAO) {
            String codigo = canonica.substring(0, canonica.indexOf(" - "));
            String codigoLower = codigo.toLowerCase(Locale.ROOT);
            if (normalizado.equals(codigoLower)
                    || normalizado.equals(canonica.toLowerCase(Locale.ROOT))
                    || normalizado.equals("tr rodogarcia | " + codigoLower)
                    || normalizado.equals("rodogarcia filial " + codigoLower)) {
                return canonica;
            }
        }
        return valor;
    }

    private record QueryContext(MapSqlParameterSource params) {
    }

    public record PerformanceEntregaResumo(
            String updatedAt,
            long totalEntregas,
            long entregasNoPrazo,
            long entregasForaDoPrazo
    ) {
    }

    public record CubagemResumo(
            String updatedAt,
            long totalFretes,
            long fretesCubados,
            long fretesComPesoReal
    ) {
    }

    public record IndenizacaoResumo(
            String updatedAt,
            long totalSinistros,
            BigDecimal valorIndenizadoAbs,
            BigDecimal valorIndenizadoOriginal,
            BigDecimal faturamentoBase
    ) {
    }

    public record UtilizacaoColetoresResumo(
            String updatedAt,
            long manifestosBipados,
            long manifestosEmitidos,
            long manifestosDescarregamento,
            long manifestosIncompletos
    ) {
        public long totalManifestos() {
            return manifestosEmitidos + manifestosDescarregamento;
        }
    }

    public record UtilizacaoColetoresRankingBase(
            String filial,
            int manifestosBipados,
            int manifestosEmitidos,
            int manifestosDescarregamento,
            int manifestosIncompletos
    ) {
        public int totalManifestos() {
            return manifestosEmitidos + manifestosDescarregamento;
        }
    }
}
