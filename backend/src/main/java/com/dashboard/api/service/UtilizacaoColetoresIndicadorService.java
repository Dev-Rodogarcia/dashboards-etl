package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresRankingDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresRowDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresSeriePointDTO;
import com.dashboard.api.model.VisaoInventarioEntity;
import com.dashboard.api.model.VisaoManifestosEntity;
import com.dashboard.api.repository.DimFilialRepository;
import com.dashboard.api.repository.VisaoInventarioRepository;
import com.dashboard.api.repository.VisaoManifestosRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class UtilizacaoColetoresIndicadorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UtilizacaoColetoresIndicadorService.class);

    private static final String CLASSIFICACAO_GERAL = "Geral";
    private static final Duration CACHE_PONTOS_TTL = Duration.ofMinutes(2);
    private static final Set<String> TIPOS_ORDEM_CONFERENCIA = Set.of(
            "picking",
            "retorno",
            "recebimento",
            "carregamento",
            "descarregamento"
    );
    private static final String[] FILIAIS_PADRAO = {
            "AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "NHB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "REC - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "RJR - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA"
    };

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoManifestosRepository manifestosRepository;
    private final VisaoInventarioRepository inventarioRepository;
    private final DimFilialRepository dimFilialRepository;
    private final EscopoFilialService escopoFilialService;
    private final PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper;
    private final KpiGoalService kpiGoalService;
    private final ConcurrentMap<IndicadoresGestaoCacheUtils.CacheKey, IndicadoresGestaoCacheUtils.CacheEntry<List<UtilizacaoColetoresPonto>>> pontosCache = new ConcurrentHashMap<>();

    UtilizacaoColetoresIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoManifestosRepository manifestosRepository,
            VisaoInventarioRepository inventarioRepository,
            DimFilialRepository dimFilialRepository
    ) {
        this(
                validadorPeriodo,
                manifestosRepository,
                inventarioRepository,
                dimFilialRepository,
                escopoSemRestricao(),
                PeriodoOffsetDateTimeHelper.padrao(),
                null
        );
    }

    @Autowired
    public UtilizacaoColetoresIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoManifestosRepository manifestosRepository,
            VisaoInventarioRepository inventarioRepository,
            DimFilialRepository dimFilialRepository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper,
            KpiGoalService kpiGoalService
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.manifestosRepository = manifestosRepository;
        this.inventarioRepository = inventarioRepository;
        this.dimFilialRepository = dimFilialRepository;
        this.escopoFilialService = escopoFilialService;
        this.periodoOffsetDateTimeHelper = periodoOffsetDateTimeHelper;
        this.kpiGoalService = kpiGoalService;
    }

    UtilizacaoColetoresIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoManifestosRepository manifestosRepository,
            VisaoInventarioRepository inventarioRepository,
            DimFilialRepository dimFilialRepository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper
    ) {
        this(
                validadorPeriodo,
                manifestosRepository,
                inventarioRepository,
                dimFilialRepository,
                escopoFilialService,
                periodoOffsetDateTimeHelper,
                null
        );
    }

    public UtilizacaoColetoresOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<UtilizacaoColetoresPonto> pontos = buscarPontos(filtro);
        if (pontos.isEmpty()) {
            return new UtilizacaoColetoresOverviewDTO(
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    0,
                    0,
                    0,
                    0,
                    0,
                    0.0
            );
        }

        int manifestosBipados = pontos.stream().mapToInt(UtilizacaoColetoresPonto::manifestosBipados).sum();
        int manifestosEmitidos = pontos.stream().mapToInt(UtilizacaoColetoresPonto::manifestosEmitidos).sum();
        int manifestosDescarregamento = pontos.stream().mapToInt(UtilizacaoColetoresPonto::manifestosDescarregamento).sum();
        int totalManifestos = pontos.stream().mapToInt(UtilizacaoColetoresPonto::totalManifestos).sum();
        int manifestosIncompletos = pontos.stream().mapToInt(UtilizacaoColetoresPonto::manifestosIncompletos).sum();

        return new UtilizacaoColetoresOverviewDTO(
                IndicadoresGestaoMetricasUtils.latestUpdate(pontos, UtilizacaoColetoresPonto::updatedAt),
                manifestosBipados,
                manifestosEmitidos,
                manifestosDescarregamento,
                totalManifestos,
                manifestosIncompletos,
                IndicadoresGestaoMetricasUtils.percentual(manifestosBipados, totalManifestos)
        );
    }

    public List<UtilizacaoColetoresSeriePointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return buscarPontos(filtro).stream()
                .map(ponto -> new UtilizacaoColetoresSeriePointDTO(
                        ponto.data().toString(),
                        ponto.filial(),
                        ponto.classificacao(),
                        ponto.manifestosBipados(),
                        ponto.manifestosEmitidos(),
                        ponto.manifestosDescarregamento(),
                        ponto.totalManifestos(),
                        ponto.manifestosIncompletos(),
                        IndicadoresGestaoMetricasUtils.percentual(ponto.manifestosBipados(), ponto.totalManifestos())
                ))
                .sorted(Comparator.comparing(UtilizacaoColetoresSeriePointDTO::date)
                        .thenComparing(UtilizacaoColetoresSeriePointDTO::filial, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(UtilizacaoColetoresSeriePointDTO::classificacao, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public List<UtilizacaoColetoresRankingDTO> buscarRanking(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        Map<String, AcumuladorRanking> ranking = new LinkedHashMap<>();
        for (UtilizacaoColetoresPonto ponto : buscarPontos(filtro)) {
            String branchName = textoOuPadrao(ponto.filial(), "Filial nao informada");
            ranking.computeIfAbsent(branchName, AcumuladorRanking::new).registrar(ponto);
        }

        Map<String, String> filiaisValidas = carregarFiliaisValidas();
        List<AcumuladorRanking> rankingFiltrado = ranking.values().stream()
                .filter(acumulador -> deveExibirNoRankingColetores(acumulador, filiaisValidas))
                .toList();

        Set<String> filiaisRanking = new LinkedHashSet<>();
        for (AcumuladorRanking acumulador : rankingFiltrado) {
            filiaisRanking.add(acumulador.branchName());
        }

        Map<String, BigDecimal> metas = kpiGoalService != null
                ? kpiGoalService.buscarMetasEfetivasPorIndicador(KpiGoalService.COLLECTOR_USAGE, filiaisRanking)
                : Map.of();

        return rankingFiltrado.stream()
                .map(acumulador -> acumulador.toDto(metas.getOrDefault(acumulador.branchName(), BigDecimal.valueOf(90))))
                .sorted(Comparator.comparingDouble(UtilizacaoColetoresRankingDTO::utilization)
                        .thenComparing(UtilizacaoColetoresRankingDTO::manifestosBipaveis, Comparator.reverseOrder())
                        .thenComparing(UtilizacaoColetoresRankingDTO::branchName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<UtilizacaoColetoresRowDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 500);

        return buscarPontos(filtro).stream()
                .sorted(Comparator.comparing(UtilizacaoColetoresPonto::data, Comparator.reverseOrder())
                        .thenComparing(UtilizacaoColetoresPonto::filial, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(UtilizacaoColetoresPonto::classificacao, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .limit(limiteAplicado)
                .map(ponto -> new UtilizacaoColetoresRowDTO(
                        chavePonto(ponto.data(), ponto.filial(), ponto.classificacao()),
                        IndicadoresGestaoMetricasUtils.formatar(ponto.data()),
                        ponto.filial(),
                        ponto.classificacao(),
                        ponto.manifestosBipados(),
                        ponto.manifestosEmitidos(),
                        ponto.manifestosDescarregamento(),
                        ponto.totalManifestos(),
                        ponto.manifestosIncompletos(),
                        IndicadoresGestaoMetricasUtils.percentual(ponto.manifestosBipados(), ponto.totalManifestos())
                ))
                .toList();
    }

    public List<UtilizacaoColetoresRowDTO> buscarExportacao(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return buscarPontos(filtro).stream()
                .sorted(Comparator.comparing(UtilizacaoColetoresPonto::data, Comparator.reverseOrder())
                        .thenComparing(UtilizacaoColetoresPonto::filial, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(UtilizacaoColetoresPonto::classificacao, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(ponto -> new UtilizacaoColetoresRowDTO(
                        chavePonto(ponto.data(), ponto.filial(), ponto.classificacao()),
                        IndicadoresGestaoMetricasUtils.formatar(ponto.data()),
                        ponto.filial(),
                        ponto.classificacao(),
                        ponto.manifestosBipados(),
                        ponto.manifestosEmitidos(),
                        ponto.manifestosDescarregamento(),
                        ponto.totalManifestos(),
                        ponto.manifestosIncompletos(),
                        IndicadoresGestaoMetricasUtils.percentual(ponto.manifestosBipados(), ponto.totalManifestos())
                ))
                .toList();
    }

    public PaginaDTO<UtilizacaoColetoresRowDTO> buscarTabelaPaginada(FiltroConsultaDTO filtro, int pagina, int tamanhoPagina) {
        return PaginacaoListaUtils.paginar(buscarExportacao(filtro), pagina, tamanhoPagina);
    }

    private List<UtilizacaoColetoresPonto> buscarPontos(FiltroConsultaDTO filtro) {
        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        if (!IndicadoresGestaoCacheUtils.contextoWebAtivo()) {
            return carregarPontos(filtro, escopo, janela);
        }
        IndicadoresGestaoCacheUtils.CacheKey key = IndicadoresGestaoCacheUtils.chave(filtro, escopo);
        Instant agora = Instant.now();
        IndicadoresGestaoCacheUtils.CacheEntry<List<UtilizacaoColetoresPonto>> novaEntry = new IndicadoresGestaoCacheUtils.CacheEntry<>(
                new CompletableFuture<>(),
                agora.plus(CACHE_PONTOS_TTL)
        );

        IndicadoresGestaoCacheUtils.CacheEntry<List<UtilizacaoColetoresPonto>> entry = pontosCache.compute(key, (cacheKey, existente) ->
                existente != null && existente.validaEm(agora) ? existente : novaEntry
        );

        if (entry == novaEntry) {
            try {
                List<UtilizacaoColetoresPonto> pontos = carregarPontos(filtro, escopo, janela);
                novaEntry.future().complete(pontos);
                if (pontos.isEmpty()) {
                    pontosCache.remove(key, novaEntry);
                }
                return pontos;
            } catch (RuntimeException ex) {
                novaEntry.future().completeExceptionally(ex);
                pontosCache.remove(key, novaEntry);
                throw ex;
            }
        }

        try {
            return entry.future().join();
        } catch (CompletionException ex) {
            if (ex.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw ex;
        }
    }

    private List<UtilizacaoColetoresPonto> carregarPontos(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            JanelaOffsetDateTime janela
    ) {
        Map<String, String> filiaisValidas = carregarFiliaisValidas();
        List<VisaoManifestosEntity> manifestos;
        List<VisaoInventarioEntity> ordens;
        try {
            manifestos = Optional
                    .ofNullable(manifestosRepository.findAll(criarManifestosSpecification(janela)))
                    .orElse(List.of());
            ordens = Optional
                    .ofNullable(inventarioRepository.findAll(criarInventarioSpecification(janela)))
                    .orElse(List.of());
        } catch (RuntimeException ex) {
            if (!DatabaseReadFallbackUtils.isRecoverableReadFailure(ex)) {
                throw ex;
            }
            DatabaseReadFallbackUtils.logFallback(LOGGER, "Falha ao consultar dados de utilizacao de coletores", ex);
            return List.of();
        }

        Map<String, AcumuladorPonto> pontos = new LinkedHashMap<>();
        Set<String> emitidosRegistrados = new LinkedHashSet<>();
        Set<String> descarregamentosRegistrados = new LinkedHashSet<>();
        Set<Long> ordensRegistradas = new LinkedHashSet<>();

        for (VisaoManifestosEntity manifesto : manifestos) {
            ManifestoElegivel registro = analisarManifesto(manifesto);
            if (registro == null) {
                continue;
            }

            String filialEmissora = canonicalizarFilial(
                    primeiroTexto(manifesto.getFilialEmissora(), manifesto.getFilial()),
                    filiaisValidas
            );

            if (permiteFilial(escopo, filtro, filialEmissora, filiaisValidas)) {
                String chaveEmitido = registro.chaveManifesto();
                if (emitidosRegistrados.add(chaveEmitido)) {
                    ponto(pontos, registro.data(), filialEmissora)
                            .registrarManifestoEmitido(manifesto.getDataExtracao());
                }
            }

            String filialDescarga = filialDescarregamentoElegivel(
                    manifesto.getLocalDescarregamento(),
                    filiaisValidas,
                    escopo,
                    filtro
            );
            if (filialDescarga != null && descarregamentosRegistrados.add(registro.chaveManifesto())) {
                ponto(pontos, registro.data(), filialDescarga)
                        .registrarManifestoDescarregamento(manifesto.getDataExtracao());
            }
        }

        for (VisaoInventarioEntity ordem : ordens) {
            OrdemConferenciaElegivel registro = analisarOrdemConferencia(ordem, filtro, escopo, filiaisValidas);
            if (registro == null || !ordensRegistradas.add(registro.numeroOrdem())) {
                continue;
            }

            ponto(pontos, registro.data(), registro.filial())
                    .registrarOrdemConferencia(registro.incompleta(), ordem.getDataExtracao());
        }

        return pontos.values().stream()
                .map(AcumuladorPonto::toPonto)
                .filter(ponto -> ponto.totalManifestos() > 0 || ponto.manifestosBipados() > 0)
                .toList();
    }

    private ManifestoElegivel analisarManifesto(VisaoManifestosEntity manifesto) {
        LocalDate data = manifesto.getDataCriacao() != null ? manifesto.getDataCriacao().toLocalDate() : null;
        if (data == null) {
            return null;
        }
        if (classificacaoExcluida(manifesto.getClassificacao())) {
            return null;
        }

        String chaveManifesto = chaveManifesto(manifesto.getNumero(), manifesto.getIdentificadorUnico());
        if (chaveManifesto == null) {
            return null;
        }

        return new ManifestoElegivel(chaveManifesto, data);
    }

    private OrdemConferenciaElegivel analisarOrdemConferencia(
            VisaoInventarioEntity ordem,
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            Map<String, String> filiaisValidas
    ) {
        Long numeroOrdem = ordem.getNumeroOrdem();
        LocalDate data = ordem.getDataHoraInicio() != null ? ordem.getDataHoraInicio().toLocalDate() : null;
        if (numeroOrdem == null || data == null || !tipoOrdemElegivel(ordem.getTipo())) {
            return null;
        }

        String filial = canonicalizarFilial(
                primeiroTexto(
                        ordem.getFilialOrdemConferencia(),
                        ordem.getFilial(),
                        ordem.getFilialEmissoraFrete()
                ),
                filiaisValidas
        );
        if (!permiteFilial(escopo, filtro, filial, filiaisValidas)) {
            return null;
        }

        return new OrdemConferenciaElegivel(
                numeroOrdem,
                data,
                filial,
                ordem.getDataHoraFim() == null
        );
    }

    @NonNull
    private Specification<VisaoManifestosEntity> criarManifestosSpecification(JanelaOffsetDateTime janela) {
        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.greaterThanOrEqualTo("dataCriacao", janela.inicioInclusivo()),
                ConsultaSpecificationUtils.lessThan("dataCriacao", janela.fimExclusivo())
        );
    }

    @NonNull
    private Specification<VisaoInventarioEntity> criarInventarioSpecification(JanelaOffsetDateTime janela) {
        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.greaterThanOrEqualTo("dataHoraInicio", janela.inicioInclusivo()),
                ConsultaSpecificationUtils.lessThan("dataHoraInicio", janela.fimExclusivo())
        );
    }

    private AcumuladorPonto ponto(Map<String, AcumuladorPonto> pontos, LocalDate data, String filial) {
        String filialNormalizada = textoOuPadrao(filial, "Filial nao informada");
        return pontos.computeIfAbsent(
                chavePonto(data, filialNormalizada, CLASSIFICACAO_GERAL),
                chave -> new AcumuladorPonto(data, filialNormalizada, CLASSIFICACAO_GERAL)
        );
    }

    private String chavePonto(LocalDate data, String filial, String classificacao) {
        return IndicadoresGestaoMetricasUtils.chaveSerie(data, filial) + "|" + normalizarTexto(classificacao);
    }

    private Map<String, String> carregarFiliaisValidas() {
        Map<String, String> filiais = new LinkedHashMap<>();
        for (String filial : FILIAIS_PADRAO) {
            registrarAliasesFilial(filiais, filial, filial);
            String codigo = extrairCodigoPrefixo(filial);
            registrarAliasFilial(filiais, "TR RODOGARCIA | " + codigo, filial);
            registrarAliasFilial(filiais, "RODOGARCIA FILIAL " + codigo, filial);
        }
        return filiais;
    }

    private boolean permiteFilial(
            EscopoFilialService.EscopoFilial escopo,
            FiltroConsultaDTO filtro,
            String filial,
            Map<String, String> filiaisValidas
    ) {
        if (!temTexto(filial)) {
            return escopo.acessoTotal() && !filtro.temFiltro("filiais");
        }

        String valor = canonicalizarFilial(filial, filiaisValidas);
        return escopoPermiteFilial(escopo, valor, filiaisValidas)
                && filtroCorrespondeFilial(filtro, valor, filiaisValidas);
    }

    private boolean classificacaoExcluida(String classificacao) {
        String normalizado = normalizarTexto(classificacao);
        return normalizado.startsWith("carga fechada")
                || normalizado.startsWith("acerto de motorista")
                || normalizado.startsWith("frete retorno")
                || normalizado.startsWith("viagem vazia");
    }

    private String filialDescarregamentoElegivel(
            String localDescarregamento,
            Map<String, String> filiaisValidas,
            EscopoFilialService.EscopoFilial escopo,
            FiltroConsultaDTO filtro
    ) {
        for (String filialDescarga : extrairFiliaisDescarregamento(localDescarregamento)) {
            String filialCanonica = canonicalizarFilial(filialDescarga, filiaisValidas);
            if (permiteFilial(escopo, filtro, filialCanonica, filiaisValidas)) {
                return filialCanonica;
            }
        }
        return null;
    }

    private String canonicalizarFilial(String filial, Map<String, String> filiaisValidas) {
        if (!temTexto(filial)) {
            return filial;
        }
        String valor = filial.trim();
        return filiaisValidas.getOrDefault(normalizarTexto(valor), valor);
    }

    private boolean escopoPermiteFilial(
            EscopoFilialService.EscopoFilial escopo,
            String filial,
            Map<String, String> filiaisValidas
    ) {
        if (escopo.acessoTotal()) {
            return true;
        }
        String valorNormalizado = normalizarTexto(canonicalizarFilial(filial, filiaisValidas));
        return escopo.filiaisPermitidas().stream()
                .map(valor -> canonicalizarFilial(valor, filiaisValidas))
                .map(this::normalizarTexto)
                .anyMatch(valorNormalizado::equals);
    }

    private boolean filtroCorrespondeFilial(
            FiltroConsultaDTO filtro,
            String filial,
            Map<String, String> filiaisValidas
    ) {
        if (!filtro.temFiltro("filiais")) {
            return true;
        }
        String valorNormalizado = normalizarTexto(canonicalizarFilial(filial, filiaisValidas));
        return filtro.valores("filiais").stream()
                .map(valor -> canonicalizarFilial(valor, filiaisValidas))
                .map(this::normalizarTexto)
                .anyMatch(valorNormalizado::equals);
    }

    private boolean deveExibirNoRankingColetores(AcumuladorRanking acumulador, Map<String, String> filiaisValidas) {
        return acumulador.manifestosBipados > 0 || filialOperacional(acumulador.branchName(), filiaisValidas);
    }

    private boolean filialOperacional(String filial, Map<String, String> filiaisValidas) {
        if (!temTexto(filial)) {
            return false;
        }
        String filialCanonica = canonicalizarFilial(filial, filiaisValidas);
        return filiaisValidas.containsKey(normalizarTexto(filialCanonica));
    }

    private void registrarAliasesFilial(Map<String, String> lookup, String filial, String canonica) {
        registrarAliasFilial(lookup, filial, canonica);
        registrarAliasFilial(lookup, extrairCodigoPrefixo(filial), canonica);
        registrarAliasFilial(lookup, extrairCodigoSufixoPipe(filial), canonica);
    }

    private void registrarAliasFilial(Map<String, String> lookup, String alias, String canonica) {
        if (!temTexto(alias) || !temTexto(canonica)) {
            return;
        }
        lookup.merge(normalizarTexto(alias), canonica.trim(), UtilizacaoColetoresIndicadorService::preferirNomeCanonico);
    }

    private String extrairCodigoPrefixo(String filial) {
        if (!temTexto(filial)) {
            return null;
        }
        int separador = filial.indexOf(" - ");
        if (separador <= 0) {
            return null;
        }
        return filial.substring(0, separador).trim();
    }

    private String extrairCodigoSufixoPipe(String filial) {
        if (!temTexto(filial)) {
            return null;
        }
        int separador = filial.lastIndexOf('|');
        if (separador < 0 || separador >= filial.length() - 1) {
            return null;
        }
        return filial.substring(separador + 1).trim();
    }

    private static String preferirNomeCanonico(String atual, String candidato) {
        return pontuacaoCanonica(candidato) > pontuacaoCanonica(atual) ? candidato : atual;
    }

    private static int pontuacaoCanonica(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0;
        }
        int score = valor.length();
        if (valor.contains(" - ")) {
            score += 100;
        }
        if (valor.matches("^[A-Z]{2,4}\\s-\\s.+$")) {
            score += 100;
        }
        return score;
    }

    private boolean tipoOrdemElegivel(String tipo) {
        return TIPOS_ORDEM_CONFERENCIA.contains(normalizarTexto(tipo));
    }

    private List<String> extrairFiliaisDescarregamento(String localDescarregamento) {
        if (!temTexto(localDescarregamento)) {
            return List.of();
        }

        List<String> filiais = new ArrayList<>();
        Set<String> registradas = new LinkedHashSet<>();
        for (String parte : localDescarregamento.split("[,;\\n]+")) {
            String valor = textoOuPadrao(parte, "");
            String normalizado = normalizarTexto(valor);
            if (normalizado.isBlank() || normalizado.equals("null")) {
                continue;
            }
            if (registradas.add(normalizado)) {
                filiais.add(valor);
            }
        }
        return filiais;
    }

    private String textoOuPadrao(String valor, String padrao) {
        return temTexto(valor) ? valor.trim() : padrao;
    }

    private String primeiroTexto(String... valores) {
        for (String valor : valores) {
            if (temTexto(valor)) {
                return valor.trim();
            }
        }
        return null;
    }

    private String chaveManifesto(Long numero, String identificadorUnico) {
        String identificador = temTexto(identificadorUnico) ? identificadorUnico.trim() : null;
        if (numero != null) {
            return numero.toString();
        }
        return identificador;
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String normalizarTexto(String valor) {
        String texto = Objects.toString(valor, "").trim().toLowerCase(Locale.ROOT);
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcento.replaceAll("\\s+", " ");
    }

    private static EscopoFilialService escopoSemRestricao() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }

    private record ManifestoElegivel(
            String chaveManifesto,
            LocalDate data
    ) {
    }

    private record OrdemConferenciaElegivel(
            Long numeroOrdem,
            LocalDate data,
            String filial,
            boolean incompleta
    ) {
    }

    private record UtilizacaoColetoresPonto(
            LocalDate data,
            String filial,
            String classificacao,
            int manifestosBipados,
            int manifestosEmitidos,
            int manifestosDescarregamento,
            int manifestosIncompletos,
            LocalDateTime updatedAt
    ) {
        private int totalManifestos() {
            return manifestosEmitidos + manifestosDescarregamento;
        }
    }

    private static final class AcumuladorRanking {
        private final String branchName;
        private int manifestosBipados;
        private int manifestosEmitidos;
        private int manifestosDescarregamento;
        private int manifestosIncompletos;

        private AcumuladorRanking(String branchName) {
            this.branchName = branchName;
        }

        private String branchName() {
            return branchName;
        }

        private void registrar(UtilizacaoColetoresPonto ponto) {
            manifestosBipados += ponto.manifestosBipados();
            manifestosEmitidos += ponto.manifestosEmitidos();
            manifestosDescarregamento += ponto.manifestosDescarregamento();
            manifestosIncompletos += ponto.manifestosIncompletos();
        }

        private UtilizacaoColetoresRankingDTO toDto(BigDecimal goal) {
            int manifestosBipaveis = manifestosEmitidos + manifestosDescarregamento;
            return new UtilizacaoColetoresRankingDTO(
                    branchName,
                    branchName,
                    IndicadoresGestaoMetricasUtils.percentual(manifestosBipados, manifestosBipaveis),
                    goal,
                    manifestosBipados,
                    manifestosBipaveis,
                    manifestosDescarregamento,
                    manifestosIncompletos
            );
        }
    }

    private static final class AcumuladorPonto {
        private final LocalDate data;
        private final String filial;
        private final String classificacao;
        private int manifestosBipados;
        private int manifestosEmitidos;
        private int manifestosDescarregamento;
        private int manifestosIncompletos;
        private LocalDateTime updatedAt;

        private AcumuladorPonto(LocalDate data, String filial, String classificacao) {
            this.data = data;
            this.filial = filial;
            this.classificacao = classificacao;
        }

        private void registrarManifestoEmitido(LocalDateTime dataExtracao) {
            manifestosEmitidos++;
            atualizarDataExtracao(dataExtracao);
        }

        private void registrarManifestoDescarregamento(LocalDateTime dataExtracao) {
            manifestosDescarregamento++;
            atualizarDataExtracao(dataExtracao);
        }

        private void registrarOrdemConferencia(boolean incompleta, LocalDateTime dataExtracao) {
            manifestosBipados++;
            if (incompleta) {
                manifestosIncompletos++;
            }
            atualizarDataExtracao(dataExtracao);
        }

        private void atualizarDataExtracao(LocalDateTime dataExtracao) {
            if (dataExtracao == null) {
                return;
            }
            if (updatedAt == null || dataExtracao.isAfter(updatedAt)) {
                updatedAt = dataExtracao;
            }
        }

        private UtilizacaoColetoresPonto toPonto() {
            return new UtilizacaoColetoresPonto(
                    data,
                    filial,
                    classificacao,
                    manifestosBipados,
                    manifestosEmitidos,
                    manifestosDescarregamento,
                    manifestosIncompletos,
                    updatedAt
            );
        }
    }
}
