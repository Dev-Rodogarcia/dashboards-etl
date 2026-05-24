package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.tracking.TrackingDashboardDTO;
import com.dashboard.api.dto.tracking.TrackingChartsDTO;
import com.dashboard.api.dto.tracking.TrackingMatrizRegiaoDTO;
import com.dashboard.api.dto.tracking.TrackingOverviewDTO;
import com.dashboard.api.dto.tracking.TrackingPrevisaoVencidaFilialDTO;
import com.dashboard.api.dto.tracking.TrackingResumoDTO;
import com.dashboard.api.dto.tracking.TrackingStatusDistribuicaoDTO;
import com.dashboard.api.dto.tracking.TrackingTimelinePointDTO;
import com.dashboard.api.dto.tracking.TrackingValorPorRegiaoDTO;
import com.dashboard.api.model.VisaoLocalizacaoCargasEntity;
import com.dashboard.api.repository.VisaoLocalizacaoCargasRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);
    private static final Set<String> STATUS_EM_TRANSITO = Set.of("Em entrega", "Em transferência", "Manifestado");
    private static final Set<String> STATUS_FINAIS = Set.of("finalizado", "finished", "entregue", "delivered");
    private static final Set<String> STATUS_CANCELADOS = Set.of("cancelado", "canceled", "cancelled");
    private static final Set<String> STATUS_TERMINAIS = Set.of("finalizado", "finished", "entregue", "delivered", "cancelado", "canceled", "cancelled");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoLocalizacaoCargasRepository repository;
    private final EscopoFilialService escopoFilialService;
    private final PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DashboardExportSqlBuilder sqlBuilder;
    private volatile TrackingViewColumns trackingViewColumns;

    TrackingService(ValidadorPeriodoService validadorPeriodo, VisaoLocalizacaoCargasRepository repository) {
        this(validadorPeriodo, repository, escopoSemRestricao(), PeriodoOffsetDateTimeHelper.padrao(), null, null);
    }

    @Autowired
    public TrackingService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoLocalizacaoCargasRepository repository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper,
            NamedParameterJdbcTemplate jdbcTemplate,
            DashboardExportSqlBuilder sqlBuilder
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.repository = repository;
        this.escopoFilialService = escopoFilialService;
        this.periodoOffsetDateTimeHelper = periodoOffsetDateTimeHelper;
        this.jdbcTemplate = jdbcTemplate;
        this.sqlBuilder = sqlBuilder;
    }

    public TrackingOverviewDTO buscarOverview(LocalDate dataInicio, LocalDate dataFim) {
        return buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public TrackingOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoLocalizacaoCargasEntity> cargas = buscarRegistros(filtro);
        int totalCargas = cargas.size();

        if (totalCargas == 0) {
            return new TrackingOverviewDTO(
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0.0
            );
        }

        int emTransito = (int) cargas.stream()
                .filter(c -> c.getStatusCarga() != null && STATUS_EM_TRANSITO.contains(c.getStatusCarga()))
                .count();

        LocalDate hoje = periodoOffsetDateTimeHelper.hoje();
        int previsaoVencida = (int) cargas.stream()
                .filter(c -> previsaoVencida(c, hoje))
                .count();

        BigDecimal valorFreteEmCarteira = cargas.stream()
                .map(VisaoLocalizacaoCargasEntity::getValorFrete)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pesoTaxadoTotal = cargas.stream()
                .map(c -> ConsultaFiltroUtils.parseBigDecimal(c.getPesoTaxado()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalElegivelFinalizacao = cargas.stream()
                .filter(c -> !statusCancelado(c.getStatusCarga()))
                .count();
        double pctFinalizado = percentual(cargas.stream()
                .filter(c -> statusFinalizado(c.getStatusCarga()))
                .count(), totalElegivelFinalizacao);

        log.info("Tracking overview calculado: total={}, periodo={} a {}", totalCargas, filtro.dataInicio(), filtro.dataFim());

        return new TrackingOverviewDTO(
                ConsultaFiltroUtils.latestUpdate(cargas, VisaoLocalizacaoCargasEntity::getDataExtracao),
                totalCargas,
                emTransito,
                previsaoVencida,
                valorFreteEmCarteira.setScale(2, RoundingMode.HALF_UP),
                pesoTaxadoTotal.setScale(2, RoundingMode.HALF_UP),
                pctFinalizado
        );
    }

    public TrackingDashboardDTO buscarDashboard(FiltroConsultaDTO filtro) {
        if (jdbcTemplate == null || sqlBuilder == null) {
            throw new IllegalStateException("Tracking dashboard analitico exige JdbcTemplate e SQL builder.");
        }
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        FiltroConsultaDTO filtroObrigatorio = aplicarFilialAtualObrigatoria(filtro, escopo);
        try {
            DashboardExportSqlBuilder.ExportSql source = sqlBuilder.buildFilteredSource(
                    DashboardExportDefinition.TRACKING,
                    filtroObrigatorio,
                    escopo,
                    Set.of()
            );
            TrackingViewColumns colunas = carregarColunasTracking();

            TrackingOverviewDTO overview = buscarOverviewAgregado(source, colunas);
            List<TrackingMatrizRegiaoDTO> matriz = buscarMatrizRegiaoDestino(source, colunas);
            TrackingChartsDTO graficos = new TrackingChartsDTO(
                    buscarStatusDistribuicaoAgregado(source),
                    List.of(),
                    buscarValorRegiaoDestinoTop10(source, colunas)
            );
            return new TrackingDashboardDTO(overview, matriz, graficos);
        } catch (DataAccessException ex) {
            log.warn(
                    "Falha no dashboard agregado de localização de cargas; usando fallback legado. periodo={} a {}, filtros={}, causa={}",
                    filtroObrigatorio.dataInicio(),
                    filtroObrigatorio.dataFim(),
                    filtroObrigatorio.filtros().keySet(),
                    ex.getMostSpecificCause().getMessage()
            );
            return buscarDashboardLegado(filtroObrigatorio);
        }
    }

    public FiltroConsultaDTO normalizarFiltroComFilialAtualObrigatoria(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return aplicarFilialAtualObrigatoria(filtro, escopoFilialService.escopoAtual());
    }

    public List<TrackingTimelinePointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return buscarRegistros(filtro).stream()
                .filter(c -> c.getDataFrete() != null)
                .collect(Collectors.groupingBy(c -> c.getDataFrete().toLocalDate()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<VisaoLocalizacaoCargasEntity> grupo = entry.getValue();
                    return new TrackingTimelinePointDTO(
                            entry.getKey().format(DATE_FMT),
                            (int) grupo.stream().filter(c -> "Pendente".equalsIgnoreCase(c.getStatusCarga())).count(),
                            (int) grupo.stream().filter(c -> "Em entrega".equalsIgnoreCase(c.getStatusCarga())).count(),
                            (int) grupo.stream().filter(c -> "Em transferência".equalsIgnoreCase(c.getStatusCarga())).count(),
                            (int) grupo.stream().filter(c -> "Finalizado".equalsIgnoreCase(c.getStatusCarga())).count()
                    );
                })
                .toList();
    }

    public List<TrackingResumoDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 200);
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        if (deveUsarConsultaLegada(filtro, escopo)) {
            JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
            return repository.findByDataFreteGreaterThanEqualAndDataFreteLessThan(
                            janela.inicioInclusivo(),
                            janela.fimExclusivo()
                    ).stream()
                    .sorted(Comparator.comparing(VisaoLocalizacaoCargasEntity::getDataFrete, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(limiteAplicado)
                    .map(c -> new TrackingResumoDTO(
                            c.getSequenceNumber(),
                            c.getDataFrete() != null ? c.getDataFrete().toString() : null,
                            c.getTipo(),
                            c.getVolumes(),
                            ConsultaFiltroUtils.parseBigDecimal(c.getPesoTaxado()).setScale(2, RoundingMode.HALF_UP),
                            ConsultaFiltroUtils.parseBigDecimal(c.getValorNf()).setScale(2, RoundingMode.HALF_UP),
                            ConsultaFiltroUtils.zeroSeNulo(c.getValorFrete()).setScale(2, RoundingMode.HALF_UP),
                            c.getFilialEmissora(),
                            c.getFilialOrigem(),
                            c.getFilialAtual(),
                            c.getFilialDestino(),
                            c.getRegiaoOrigem(),
                            c.getRegiaoDestino(),
                            c.getClassificacao(),
                            c.getStatusCarga(),
                            c.getPrevisaoEntrega() != null ? c.getPrevisaoEntrega().toString() : null
                    ))
                    .toList();
        }

        return repository.findAll(
                        criarSpecification(filtro),
                        PageRequest.of(0, limiteAplicado, Sort.by(Sort.Direction.DESC, "dataFrete"))
                ).getContent().stream()
                .limit(limiteAplicado)
                .map(c -> new TrackingResumoDTO(
                        c.getSequenceNumber(),
                        c.getDataFrete() != null ? c.getDataFrete().toString() : null,
                        c.getTipo(),
                        c.getVolumes(),
                        ConsultaFiltroUtils.parseBigDecimal(c.getPesoTaxado()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.parseBigDecimal(c.getValorNf()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(c.getValorFrete()).setScale(2, RoundingMode.HALF_UP),
                        c.getFilialEmissora(),
                        c.getFilialOrigem(),
                        c.getFilialAtual(),
                        c.getFilialDestino(),
                        c.getRegiaoOrigem(),
                        c.getRegiaoDestino(),
                        c.getClassificacao(),
                        c.getStatusCarga(),
                        c.getPrevisaoEntrega() != null ? c.getPrevisaoEntrega().toString() : null
                ))
                .toList();
    }

    public TrackingChartsDTO buscarGraficos(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoLocalizacaoCargasEntity> cargas = buscarRegistros(filtro);
        LocalDate hoje = periodoOffsetDateTimeHelper.hoje();

        List<TrackingStatusDistribuicaoDTO> statusDistribuicao = cargas.stream()
                .collect(Collectors.groupingBy(c -> textoOuPadrao(c.getStatusCarga(), "Sem status")))
                .entrySet().stream()
                .map(entry -> new TrackingStatusDistribuicaoDTO(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .map(VisaoLocalizacaoCargasEntity::getValorFrete)
                                .map(ConsultaFiltroUtils::zeroSeNulo)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP)
                ))
                .sorted(Comparator.comparing(TrackingStatusDistribuicaoDTO::total).reversed()
                        .thenComparing(TrackingStatusDistribuicaoDTO::status))
                .toList();

        List<TrackingPrevisaoVencidaFilialDTO> previsaoVencidaPorFilialAtual = cargas.stream()
                .filter(c -> previsaoVencida(c, hoje))
                .collect(Collectors.groupingBy(c -> textoOuPadrao(c.getFilialAtual(), "Sem filial")))
                .entrySet().stream()
                .map(entry -> new TrackingPrevisaoVencidaFilialDTO(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().size()
                ))
                .sorted(Comparator.comparing(TrackingPrevisaoVencidaFilialDTO::vencidas).reversed()
                        .thenComparing(TrackingPrevisaoVencidaFilialDTO::filialAtual))
                .toList();

        List<TrackingValorPorRegiaoDTO> valorPorRegiaoDestino = cargas.stream()
                .collect(Collectors.groupingBy(c -> textoOuPadrao(c.getRegiaoDestino(), "Sem regiao")))
                .entrySet().stream()
                .map(entry -> new TrackingValorPorRegiaoDTO(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(VisaoLocalizacaoCargasEntity::getValorFrete)
                                .map(ConsultaFiltroUtils::zeroSeNulo)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP),
                        entry.getValue().size()
                ))
                .sorted(Comparator.comparing(TrackingValorPorRegiaoDTO::valorFrete).reversed()
                        .thenComparing(TrackingValorPorRegiaoDTO::regiaoDestino))
                .toList();

        return new TrackingChartsDTO(statusDistribuicao, previsaoVencidaPorFilialAtual, valorPorRegiaoDestino);
    }

    private List<VisaoLocalizacaoCargasEntity> buscarRegistros(FiltroConsultaDTO filtro) {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        if (deveUsarConsultaLegada(filtro, escopo)) {
            JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
            return repository.findByDataFreteGreaterThanEqualAndDataFreteLessThan(
                    janela.inicioInclusivo(),
                    janela.fimExclusivo()
            );
        }
        return repository.findAll(criarSpecification(filtro));
    }

    private TrackingOverviewDTO buscarOverviewAgregado(
            DashboardExportSqlBuilder.ExportSql source,
            TrackingViewColumns colunas
    ) {
        MapSqlParameterSource params = copiarParams(source);
        params.addValue("hoje", periodoOffsetDateTimeHelper.hoje());
        String statusNormalizadoSql = statusNormalizadoSql(colunas);
        String valorFreteSql = valorFreteSql();
        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                )
                SELECT
                    MAX(%s) AS updated_at,
                    COUNT(1) AS total_cargas,
                    SUM(CASE WHEN %s IN ('delivering', 'in_transfer', 'manifested', 'em entrega', 'em transferência', 'em transferencia', 'manifestado') THEN 1 ELSE 0 END) AS em_transito,
                    SUM(CASE WHEN %s THEN 1 ELSE 0 END) AS previsao_vencida,
                    SUM(%s) AS valor_frete,
                    SUM(%s) AS peso_taxado,
                    CAST(
                        100.0 * SUM(CASE WHEN %s IN ('finished', 'delivered', 'finalizado', 'entregue') THEN 1 ELSE 0 END)
                        / NULLIF(SUM(CASE WHEN %s NOT IN ('canceled', 'cancelled', 'cancelado') THEN 1 ELSE 0 END), 0)
                        AS DECIMAL(9, 2)
                    ) AS pct_finalizado
                FROM base_filtrada
                """.formatted(
                source.sql(),
                dataExtracaoSql(),
                statusNormalizadoSql,
                previsaoVencidaSql(statusNormalizadoSql),
                valorFreteSql,
                pesoSql(colunas),
                statusNormalizadoSql,
                statusNormalizadoSql
        );

        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> new TrackingOverviewDTO(
                rs.getTimestamp("updated_at") != null
                        ? rs.getTimestamp("updated_at").toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                rs.getInt("total_cargas"),
                rs.getInt("em_transito"),
                rs.getInt("previsao_vencida"),
                decimal(rs.getBigDecimal("valor_frete")),
                decimal(rs.getBigDecimal("peso_taxado")),
                decimal(rs.getBigDecimal("pct_finalizado")).doubleValue()
        ));
    }

    private List<TrackingMatrizRegiaoDTO> buscarMatrizRegiaoDestino(
            DashboardExportSqlBuilder.ExportSql source,
            TrackingViewColumns colunas
    ) {
        MapSqlParameterSource params = copiarParams(source);
        params.addValue("hoje", periodoOffsetDateTimeHelper.hoje());
        String siglaRegiaoSql = siglaRegiaoDestinoSql(colunas);
        String responsavelRegiaoSql = responsavelRegiaoDestinoSql();
        String statusNormalizadoSql = statusNormalizadoSql(colunas);
        String valorFreteSql = valorFreteSql();
        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                )
                SELECT
                    %s AS sigla,
                    %s AS responsavel,
                    SUM(%s) AS peso_taxado,
                    SUM(%s) AS valor_frete,
                    SUM(%s) AS valor_nota,
                    SUM(%s) AS volumes,
                    SUM(CASE WHEN %s THEN 1 ELSE 0 END) AS fora_prazo
                FROM base_filtrada
                GROUP BY
                    %s,
                    %s
                ORDER BY peso_taxado DESC, sigla ASC
                """.formatted(
                source.sql(),
                siglaRegiaoSql,
                responsavelRegiaoSql,
                pesoSql(colunas),
                valorFreteSql,
                valorNfSql(colunas),
                volumesSql(),
                previsaoVencidaSql(statusNormalizadoSql),
                siglaRegiaoSql,
                responsavelRegiaoSql
        );

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new TrackingMatrizRegiaoDTO(
                rs.getString("sigla"),
                rs.getString("responsavel"),
                decimal(rs.getBigDecimal("peso_taxado")),
                decimal(rs.getBigDecimal("valor_frete")),
                decimal(rs.getBigDecimal("valor_nota")),
                rs.getInt("volumes"),
                rs.getInt("fora_prazo")
        ));
    }

    private List<TrackingStatusDistribuicaoDTO> buscarStatusDistribuicaoAgregado(DashboardExportSqlBuilder.ExportSql source) {
        String valorFreteSql = valorFreteSql();
        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                )
                SELECT
                    COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status Carga]))), ''), 'Sem status') AS status,
                    COUNT(1) AS total,
                    SUM(%s) AS valor_frete
                FROM base_filtrada
                GROUP BY COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status Carga]))), ''), 'Sem status')
                ORDER BY total DESC, status ASC
                """.formatted(source.sql(), valorFreteSql);

        return jdbcTemplate.query(sql, copiarParams(source), (rs, rowNum) -> new TrackingStatusDistribuicaoDTO(
                rs.getString("status"),
                rs.getInt("total"),
                decimal(rs.getBigDecimal("valor_frete"))
        ));
    }

    private List<TrackingValorPorRegiaoDTO> buscarValorRegiaoDestinoTop10(
            DashboardExportSqlBuilder.ExportSql source,
            TrackingViewColumns colunas
    ) {
        String siglaRegiaoSql = siglaRegiaoDestinoSql(colunas);
        String valorFreteSql = valorFreteSql();
        String sql = """
                WITH base_filtrada AS (
                    SELECT *
                    %s
                ),
                regioes AS (
                    SELECT
                        %s AS regiao,
                        SUM(%s) AS valor_frete,
                        COUNT(1) AS cargas,
                        ROW_NUMBER() OVER (ORDER BY SUM(%s) DESC) AS rn
                    FROM base_filtrada
                    GROUP BY %s
                )
                SELECT
                    CASE WHEN rn <= 10 THEN regiao ELSE 'Outros' END AS regiao_destino,
                    SUM(valor_frete) AS valor_frete,
                    SUM(cargas) AS cargas,
                    MIN(CASE WHEN rn <= 10 THEN rn ELSE 999 END) AS ordem
                FROM regioes
                GROUP BY CASE WHEN rn <= 10 THEN regiao ELSE 'Outros' END
                ORDER BY ordem ASC, valor_frete DESC
                """.formatted(source.sql(), siglaRegiaoSql, valorFreteSql, valorFreteSql, siglaRegiaoSql);

        return jdbcTemplate.query(sql, copiarParams(source), (rs, rowNum) -> new TrackingValorPorRegiaoDTO(
                rs.getString("regiao_destino"),
                decimal(rs.getBigDecimal("valor_frete")),
                rs.getInt("cargas")
        ));
    }

    private TrackingDashboardDTO buscarDashboardLegado(FiltroConsultaDTO filtro) {
        DashboardExportSqlBuilder.ExportSql select = sqlBuilder.buildSelect(
                DashboardExportDefinition.TRACKING,
                filtro,
                escopoFilialService.escopoAtual(),
                Set.of()
        );
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(select.sql(), select.params());
        TrackingOverviewDTO overview = buscarOverviewLegado(rows);
        List<TrackingMatrizRegiaoDTO> matriz = buscarMatrizRegiaoDestinoLegada(rows);
        TrackingChartsDTO graficosLegados = buscarGraficosLegado(rows);
        TrackingChartsDTO graficos = new TrackingChartsDTO(
                graficosLegados.statusDistribuicao(),
                List.of(),
                valorPorRegiaoDestinoTop10Legado(graficosLegados.valorPorRegiaoDestino())
        );
        return new TrackingDashboardDTO(overview, matriz, graficos);
    }

    private TrackingOverviewDTO buscarOverviewLegado(List<Map<String, Object>> rows) {
        int totalCargas = rows.size();
        if (totalCargas == 0) {
            return new TrackingOverviewDTO(
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0.0
            );
        }

        LocalDate hoje = periodoOffsetDateTimeHelper.hoje();
        int emTransito = (int) rows.stream()
                .filter(row -> statusEmTransito(textoRaw(row, "Status Carga")))
                .count();
        int previsaoVencida = (int) rows.stream()
                .filter(row -> previsaoVencidaRaw(row, hoje))
                .count();
        BigDecimal valorFrete = rows.stream()
                .map(row -> decimalRaw(row, "Valor Frete"))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal pesoTaxado = rows.stream()
                .map(row -> decimalRaw(row, "Peso Taxado Decimal", "Peso Taxado"))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        long elegiveis = rows.stream()
                .filter(row -> !statusCancelado(textoRaw(row, "Status Carga")))
                .count();
        double pctFinalizado = percentual(rows.stream()
                .filter(row -> statusFinalizado(textoRaw(row, "Status Carga")))
                .count(), elegiveis);

        return new TrackingOverviewDTO(
                latestUpdateRaw(rows),
                totalCargas,
                emTransito,
                previsaoVencida,
                valorFrete,
                pesoTaxado,
                pctFinalizado
        );
    }

    private List<TrackingMatrizRegiaoDTO> buscarMatrizRegiaoDestinoLegada(List<Map<String, Object>> rows) {
        LocalDate hoje = periodoOffsetDateTimeHelper.hoje();
        return rows.stream()
                .collect(Collectors.groupingBy(
                        row -> new MatrizRegiaoKey(siglaRegiaoDestinoLegada(row), responsavelRegiaoDestinoLegada(row)),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> {
                    List<Map<String, Object>> grupo = entry.getValue();
                    return new TrackingMatrizRegiaoDTO(
                            entry.getKey().sigla(),
                            entry.getKey().responsavel(),
                            grupo.stream()
                                    .map(row -> decimalRaw(row, "Peso Taxado Decimal", "Peso Taxado"))
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .setScale(2, RoundingMode.HALF_UP),
                            grupo.stream()
                                    .map(row -> decimalRaw(row, "Valor Frete"))
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .setScale(2, RoundingMode.HALF_UP),
                            grupo.stream()
                                    .map(row -> decimalRaw(row, "Valor NF Decimal", "Valor NF"))
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .setScale(2, RoundingMode.HALF_UP),
                            grupo.stream()
                                    .mapToInt(row -> inteiroRaw(row, "Volumes"))
                                    .sum(),
                            (int) grupo.stream().filter(row -> previsaoVencidaRaw(row, hoje)).count()
                    );
                })
                .sorted(Comparator.comparing(TrackingMatrizRegiaoDTO::pesoTaxado).reversed()
                        .thenComparing(TrackingMatrizRegiaoDTO::siglaRegiaoDestino))
                .toList();
    }

    private TrackingChartsDTO buscarGraficosLegado(List<Map<String, Object>> rows) {
        List<TrackingStatusDistribuicaoDTO> statusDistribuicao = rows.stream()
                .collect(Collectors.groupingBy(row -> textoOuPadrao(textoRaw(row, "Status Carga"), "Sem status")))
                .entrySet().stream()
                .map(entry -> new TrackingStatusDistribuicaoDTO(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .map(row -> decimalRaw(row, "Valor Frete"))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP)
                ))
                .sorted(Comparator.comparing(TrackingStatusDistribuicaoDTO::total).reversed()
                        .thenComparing(TrackingStatusDistribuicaoDTO::status))
                .toList();

        List<TrackingValorPorRegiaoDTO> valorPorRegiaoDestino = rows.stream()
                .collect(Collectors.groupingBy(row -> siglaRegiaoDestinoLegada(row)))
                .entrySet().stream()
                .map(entry -> new TrackingValorPorRegiaoDTO(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(row -> decimalRaw(row, "Valor Frete"))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP),
                        entry.getValue().size()
                ))
                .sorted(Comparator.comparing(TrackingValorPorRegiaoDTO::valorFrete).reversed()
                        .thenComparing(TrackingValorPorRegiaoDTO::regiaoDestino))
                .toList();

        return new TrackingChartsDTO(statusDistribuicao, List.of(), valorPorRegiaoDestino);
    }

    private List<TrackingValorPorRegiaoDTO> valorPorRegiaoDestinoTop10Legado(List<TrackingValorPorRegiaoDTO> valores) {
        if (valores.size() <= 10) {
            return valores;
        }

        List<TrackingValorPorRegiaoDTO> resultado = new ArrayList<>(valores.subList(0, 10));
        List<TrackingValorPorRegiaoDTO> demais = valores.subList(10, valores.size());
        BigDecimal valorOutros = demais.stream()
                .map(TrackingValorPorRegiaoDTO::valorFrete)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        int cargasOutros = demais.stream().mapToInt(TrackingValorPorRegiaoDTO::cargas).sum();
        if (valorOutros.compareTo(BigDecimal.ZERO) > 0 || cargasOutros > 0) {
            resultado.add(new TrackingValorPorRegiaoDTO("Outros", valorOutros, cargasOutros));
        }
        return resultado;
    }

    private String siglaRegiaoDestinoLegada(Map<String, Object> row) {
        return textoOuPadrao(
                textoRaw(row, "Sigla Responsável Região Destino", "Sigla Responsavel Regiao Destino"),
                textoOuPadrao(textoRaw(row, "Responsável pela Região de Destino", "Responsavel pela Regiao de Destino"),
                        textoOuPadrao(textoRaw(row, "Região Destino", "Regiao Destino"), "SEM_MAP"))
        );
    }

    private String responsavelRegiaoDestinoLegada(Map<String, Object> row) {
        return textoOuPadrao(textoRaw(row, "Responsável pela Região de Destino", "Responsavel pela Regiao de Destino"), "Sem responsável");
    }

    private boolean statusEmTransito(String status) {
        return status != null && STATUS_EM_TRANSITO.stream().anyMatch(item -> item.equalsIgnoreCase(status.trim()));
    }

    private boolean previsaoVencidaRaw(Map<String, Object> row, LocalDate hoje) {
        LocalDate previsao = localDateRaw(row, "Previsão Entrega/Previsão de entrega", "Previsao Entrega/Previsao de entrega");
        return previsao != null
                && previsao.isBefore(hoje)
                && !statusTerminal(textoRaw(row, "Status Carga"));
    }

    private String latestUpdateRaw(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> localDateTimeRaw(row, "Data de extracao"))
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(data -> data.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .orElseGet(() -> java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    private Object valorRaw(Map<String, Object> row, String... colunas) {
        for (String coluna : colunas) {
            Object valor = row.get(coluna);
            if (valor != null) {
                return valor;
            }
        }
        return null;
    }

    private String textoRaw(Map<String, Object> row, String... colunas) {
        Object valor = valorRaw(row, colunas);
        return valor != null ? valor.toString() : null;
    }

    private BigDecimal decimalRaw(Map<String, Object> row, String... colunas) {
        Object valor = valorRaw(row, colunas);
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }
        if (valor instanceof Number numero) {
            return BigDecimal.valueOf(numero.doubleValue());
        }
        return ConsultaFiltroUtils.parseBigDecimal(valor.toString());
    }

    private int inteiroRaw(Map<String, Object> row, String... colunas) {
        Object valor = valorRaw(row, colunas);
        if (valor == null) {
            return 0;
        }
        if (valor instanceof Number numero) {
            return numero.intValue();
        }
        return ConsultaFiltroUtils.parseBigDecimal(valor.toString()).intValue();
    }

    private LocalDate localDateRaw(Map<String, Object> row, String... colunas) {
        LocalDateTime dataHora = localDateTimeRaw(row, colunas);
        if (dataHora != null) {
            return dataHora.toLocalDate();
        }
        Object valor = valorRaw(row, colunas);
        if (valor instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        String texto = valor != null ? valor.toString().trim() : "";
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDateTime localDateTimeRaw(Map<String, Object> row, String... colunas) {
        Object valor = valorRaw(row, colunas);
        if (valor == null) {
            return null;
        }
        if (valor instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (valor instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toLocalDateTime();
        }
        if (valor instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (valor instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().atStartOfDay();
        }
        if (valor instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.of(PeriodoOffsetDateTimeHelper.DEFAULT_ZONE_ID));
        }

        String texto = valor.toString().trim();
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(texto).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(texto);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(texto).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private FiltroConsultaDTO aplicarFilialAtualObrigatoria(FiltroConsultaDTO filtro, EscopoFilialService.EscopoFilial escopo) {
        List<String> filiaisAtuais = filtro.valores("filialAtual");
        if (filiaisAtuais.size() == 1) {
            return filtro;
        }
        if (filiaisAtuais.size() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione apenas uma Filial Atual para o painel de Localização de Cargas.");
        }

        List<String> filiaisPermitidas = escopo.filiaisOrdenadas();
        if (!escopo.acessoTotal() && filiaisPermitidas.size() == 1) {
            Map<String, List<String>> filtros = new LinkedHashMap<>(filtro.filtros());
            filtros.put("filialAtual", List.of(filiaisPermitidas.get(0)));
            return new FiltroConsultaDTO(filtro.dataInicio(), filtro.dataFim(), filtros);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filial Atual é obrigatória para carregar o painel de Localização de Cargas.");
    }

    private String previsaoVencidaSql(String statusNormalizadoSql) {
        return """
                (
                    [Previsão Entrega/Previsão de entrega] IS NOT NULL
                    AND TRY_CONVERT(date, [Previsão Entrega/Previsão de entrega]) < :hoje
                    AND %s NOT IN ('finished', 'delivered', 'canceled', 'cancelled', 'finalizado', 'entregue', 'cancelado')
                )
                """.formatted(statusNormalizadoSql);
    }

    private TrackingViewColumns carregarColunasTracking() {
        TrackingViewColumns cached = trackingViewColumns;
        if (cached != null) {
            return cached;
        }

        List<String> nomes = jdbcTemplate.queryForList("""
                SELECT c.name
                FROM sys.columns c
                WHERE c.object_id = OBJECT_ID(N'dbo.vw_localizacao_cargas_powerbi')
                """, new MapSqlParameterSource(), String.class);

        Set<String> colunas = nomes.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        TrackingViewColumns carregadas = new TrackingViewColumns(
                colunas.contains("Status Normalizado"),
                colunas.contains("Peso Taxado Decimal"),
                colunas.contains("Valor NF Decimal"),
                colunas.contains("Sigla Responsável Região Destino")
        );

        if (!carregadas.contratoGovernadoCompleto()) {
            log.warn(
                    "View de localização de cargas sem contrato governado completo; usando fallback compatível. colunas={}",
                    carregadas
            );
        }

        trackingViewColumns = carregadas;
        return carregadas;
    }

    private String statusNormalizadoSql(TrackingViewColumns colunas) {
        if (colunas.statusNormalizado()) {
            return """
                    COALESCE(
                        NULLIF(LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status Normalizado])))), ''),
                        NULLIF(LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status Carga])))), ''),
                        'sem_status'
                    )
                    """;
        }

        return """
                COALESCE(
                    NULLIF(LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status Carga])))), ''),
                    'sem_status'
                )
                """;
    }

    private String pesoSql(TrackingViewColumns colunas) {
        return """
                COALESCE(
                    %s
                    TRY_CONVERT(DECIMAL(18, 3), [Peso Taxado]),
                    TRY_CONVERT(DECIMAL(18, 3), REPLACE(CONVERT(NVARCHAR(50), [Peso Taxado]), ',', '.')),
                    TRY_CONVERT(DECIMAL(18, 3), REPLACE(REPLACE(CONVERT(NVARCHAR(50), [Peso Taxado]), '.', ''), ',', '.')),
                    0
                )
                """.formatted(colunas.pesoTaxadoDecimal()
                ? "TRY_CONVERT(DECIMAL(18, 3), [Peso Taxado Decimal]),\n        "
                : "");
    }

    private String valorNfSql(TrackingViewColumns colunas) {
        return """
                COALESCE(
                    %s
                    TRY_CONVERT(DECIMAL(18, 2), [Valor NF]),
                    TRY_CONVERT(DECIMAL(18, 2), REPLACE(CONVERT(NVARCHAR(50), [Valor NF]), ',', '.')),
                    TRY_CONVERT(DECIMAL(18, 2), REPLACE(REPLACE(CONVERT(NVARCHAR(50), [Valor NF]), '.', ''), ',', '.')),
                    0
                )
                """.formatted(colunas.valorNfDecimal()
                ? "TRY_CONVERT(DECIMAL(18, 2), [Valor NF Decimal]),\n        "
                : "");
    }

    private String dataExtracaoSql() {
        return """
                COALESCE(
                    TRY_CONVERT(DATETIME2, [Data de extracao]),
                    TRY_CONVERT(DATETIME2, CONVERT(NVARCHAR(50), [Data de extracao]), 126),
                    TRY_CONVERT(DATETIME2, CONVERT(NVARCHAR(50), [Data de extracao]), 120),
                    TRY_CONVERT(DATETIME2, CONVERT(NVARCHAR(50), [Data de extracao]), 103)
                )
                """;
    }

    private String valorFreteSql() {
        return """
                COALESCE(
                    TRY_CONVERT(DECIMAL(18, 2), [Valor Frete]),
                    TRY_CONVERT(DECIMAL(18, 2), REPLACE(CONVERT(NVARCHAR(50), [Valor Frete]), ',', '.')),
                    TRY_CONVERT(DECIMAL(18, 2), REPLACE(REPLACE(CONVERT(NVARCHAR(50), [Valor Frete]), '.', ''), ',', '.')),
                    0
                )
                """;
    }

    private String volumesSql() {
        return """
                COALESCE(
                    TRY_CONVERT(INT, [Volumes]),
                    TRY_CONVERT(INT, TRY_CONVERT(DECIMAL(18, 3), [Volumes])),
                    TRY_CONVERT(INT, TRY_CONVERT(DECIMAL(18, 3), REPLACE(CONVERT(NVARCHAR(50), [Volumes]), ',', '.'))),
                    0
                )
                """;
    }

    private String siglaRegiaoDestinoSql(TrackingViewColumns colunas) {
        if (colunas.siglaResponsavelRegiaoDestino()) {
            return """
                    COALESCE(
                        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Sigla Responsável Região Destino]))), ''),
                        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Responsável pela Região de Destino]))), ''),
                        'SEM_MAP'
                    )
                    """;
        }

        return """
                COALESCE(
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Responsável pela Região de Destino]))), ''),
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Região Destino]))), ''),
                    'SEM_MAP'
                )
                """;
    }

    private String responsavelRegiaoDestinoSql() {
        return """
                COALESCE(
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Responsável pela Região de Destino]))), ''),
                    'Sem responsável'
                )
                """;
    }

    private MapSqlParameterSource copiarParams(DashboardExportSqlBuilder.ExportSql source) {
        return new MapSqlParameterSource(source.params().getValues());
    }

    private BigDecimal decimal(BigDecimal valor) {
        return ConsultaFiltroUtils.zeroSeNulo(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private double percentual(long valor, long total) {
        if (total == 0) {
            return 0.0;
        }

        return BigDecimal.valueOf(valor)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private boolean previsaoVencida(VisaoLocalizacaoCargasEntity carga, LocalDate hoje) {
        return carga.getPrevisaoEntrega() != null
                && carga.getPrevisaoEntrega().toLocalDate().isBefore(hoje)
                && !statusTerminal(carga.getStatusCarga());
    }

    private boolean statusTerminal(String status) {
        return status != null && STATUS_TERMINAIS.contains(status.trim().toLowerCase(java.util.Locale.ROOT));
    }

    private boolean statusFinalizado(String status) {
        return status != null && STATUS_FINAIS.contains(status.trim().toLowerCase(java.util.Locale.ROOT));
    }

    private boolean statusCancelado(String status) {
        return status != null && STATUS_CANCELADOS.contains(status.trim().toLowerCase(java.util.Locale.ROOT));
    }

    private String textoOuPadrao(String valor, String padrao) {
        return Objects.requireNonNullElse(valor, "").isBlank() ? padrao : valor;
    }

    @NonNull
    private Specification<VisaoLocalizacaoCargasEntity> criarSpecification(FiltroConsultaDTO filtro) {
        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.greaterThanOrEqualTo("dataFrete", janela.inicioInclusivo()),
                ConsultaSpecificationUtils.lessThan("dataFrete", janela.fimExclusivo()),
                ConsultaSpecificationUtils.escopoFiliais(escopo, "filialEmissora", "filialOrigem", "filialAtual", "filialDestino"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "filialEmissora", "filialEmissora"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "filialAtual", "filialAtual"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "filialDestino", "filialDestino"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "regiaoOrigem", "regiaoOrigem"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "regiaoDestino", "regiaoDestino"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "statusCarga", "statusCarga")
        );
    }

    private boolean deveUsarConsultaLegada(FiltroConsultaDTO filtro, EscopoFilialService.EscopoFilial escopo) {
        return escopo.acessoTotal() && filtro.filtros().isEmpty();
    }

    private record MatrizRegiaoKey(String sigla, String responsavel) {
    }

    private record TrackingViewColumns(
            boolean statusNormalizado,
            boolean pesoTaxadoDecimal,
            boolean valorNfDecimal,
            boolean siglaResponsavelRegiaoDestino
    ) {
        private boolean contratoGovernadoCompleto() {
            return statusNormalizado
                    && pesoTaxadoDecimal
                    && valorNfDecimal
                    && siglaResponsavelRegiaoDestino;
        }
    }

    private static EscopoFilialService escopoSemRestricao() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }
}
