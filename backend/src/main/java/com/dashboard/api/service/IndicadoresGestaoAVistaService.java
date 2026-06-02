package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.dto.indicadoresgestao.HorarioCorteRowDTO;
import com.dashboard.api.dto.indicadoresgestao.HorariosCorteOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.HorariosCorteSeriePointDTO;
import com.dashboard.api.model.VisaoHorariosCorteEntity;
import com.dashboard.api.repository.VisaoHorariosCorteRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class IndicadoresGestaoAVistaService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Duration CACHE_FONTE_TTL = Duration.ofMinutes(2);

    private final ValidadorPeriodoService validadorPeriodo;
    private final HorariosCorteRasterDataSource rasterSqlRepository;
    private final VisaoHorariosCorteRepository repository;
    private final EscopoFilialService escopoFilialService;
    private final HorarioCorteFilialMapperService filialMapperService;
    private final ConcurrentMap<HorarioCorteFonteCacheKey, HorarioCorteFonteCacheEntry> horariosCorteFonteCache = new ConcurrentHashMap<>();

    public IndicadoresGestaoAVistaService(
            ValidadorPeriodoService validadorPeriodo,
            HorariosCorteRasterDataSource rasterSqlRepository,
            VisaoHorariosCorteRepository repository,
            EscopoFilialService escopoFilialService,
            HorarioCorteFilialMapperService filialMapperService
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.rasterSqlRepository = rasterSqlRepository;
        this.repository = repository;
        this.escopoFilialService = escopoFilialService;
        this.filialMapperService = filialMapperService;
    }

    public HorariosCorteOverviewDTO buscarHorariosCorteOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<HorarioCorteRegistroResolvido> rows = buscarHorariosCorte(filtro);
        if (rows.isEmpty()) {
            return new HorariosCorteOverviewDTO(
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    0,
                    0,
                    0.0,
                    null,
                    null
            );
        }

        List<HorarioCorteRegistroResolvido> calculaveis = rows.stream()
                .filter(this::isCalculavelParaKpi)
                .toList();
        int totalProgramado = calculaveis.size();
        int saidasNoHorario = (int) calculaveis.stream()
                .filter(row -> Boolean.TRUE.equals(row.entity().getSaiuNoHorario()))
                .count();
        double pctNoHorario = percentual(saidasNoHorario, totalProgramado);

        HorarioCorteRegistroResolvido ultimaImportacao = rows.stream()
                .filter(row -> row.entity().getImportadoEm() != null)
                .max(Comparator.comparing(row -> row.entity().getImportadoEm()))
                .orElse(null);

        return new HorariosCorteOverviewDTO(
                ConsultaFiltroUtils.latestUpdate(rows, row -> row.entity().getDataExtracao()),
                saidasNoHorario,
                totalProgramado,
                pctNoHorario,
                ultimaImportacao != null ? ultimaImportacao.entity().getImportadoEm().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                ultimaImportacao != null ? ultimaImportacao.entity().getNomeArquivo() : null
        );
    }

    public List<HorariosCorteSeriePointDTO> buscarHorariosCorteSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        Map<String, List<HorarioCorteRegistroResolvido>> agrupado = buscarHorariosCorte(filtro).stream()
                .filter(this::isCalculavelParaKpi)
                .collect(Collectors.groupingBy(row -> chaveSerie(row.entity().getData(), row.filial())));

        return agrupado.entrySet().stream()
                .map(entry -> {
                    List<HorarioCorteRegistroResolvido> grupo = entry.getValue();
                    HorarioCorteRegistroResolvido amostra = grupo.get(0);
                    int totalProgramado = grupo.size();
                    int saidasNoHorario = (int) grupo.stream()
                            .filter(row -> Boolean.TRUE.equals(row.entity().getSaiuNoHorario()))
                            .count();

                    return new HorariosCorteSeriePointDTO(
                            amostra.entity().getData() != null ? amostra.entity().getData().format(DATE_FMT) : null,
                            amostra.filial(),
                            saidasNoHorario,
                            totalProgramado,
                            percentual(saidasNoHorario, totalProgramado)
                    );
                })
                .sorted(Comparator.comparing(HorariosCorteSeriePointDTO::date, Comparator.nullsLast(String::compareTo))
                        .thenComparing(HorariosCorteSeriePointDTO::filial, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public List<HorarioCorteRowDTO> buscarHorariosCorteTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 500);

        return buscarHorariosCorte(filtro).stream()
                .sorted(Comparator.comparing((HorarioCorteRegistroResolvido row) -> row.entity().getData(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(row -> row.entity().getImportadoEm(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HorarioCorteRegistroResolvido::filial, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(row -> row.entity().getLinhaOuOperacao(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .limit(limiteAplicado)
                .map(row -> new HorarioCorteRowDTO(
                        Objects.requireNonNullElse(row.entity().getId(), 0L),
                        formatar(row.entity().getData()),
                        row.filial(),
                        row.entity().getLinhaOuOperacao(),
                        row.entity().getOrigemSm(),
                        row.entity().getDestinoSm(),
                        row.entity().getOrigemDestino(),
                        row.entity().getOrigem(),
                        row.entity().getOrdem(),
                        row.entity().getDestino(),
                        row.entity().getHorarioCorteSm(),
                        row.entity().getPrevisaoChegadaDestino(),
                        row.entity().getTransitTime(),
                        formatar(row.entity().getInicio()),
                        formatar(row.entity().getManifestado()),
                        formatar(row.entity().getSmGerada()),
                        formatar(row.entity().getCorte()),
                        formatar(row.entity().getSaidaEfetiva()),
                        formatar(row.entity().getHorarioCorte()),
                        row.entity().getSaiuNoHorario(),
                        row.entity().getAtrasoMinutos(),
                        row.entity().getObservacao(),
                        row.entity().getNomeArquivo(),
                        formatar(row.entity().getImportadoEm()),
                        row.entity().getImportadoPor()
                ))
                .toList();
    }

    public List<HorarioCorteRowDTO> buscarHorariosCorteExportacao(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return buscarHorariosCorte(filtro).stream()
                .sorted(Comparator.comparing((HorarioCorteRegistroResolvido row) -> row.entity().getData(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(row -> row.entity().getImportadoEm(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HorarioCorteRegistroResolvido::filial, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(row -> row.entity().getLinhaOuOperacao(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(row -> new HorarioCorteRowDTO(
                        Objects.requireNonNullElse(row.entity().getId(), 0L),
                        formatar(row.entity().getData()),
                        row.filial(),
                        row.entity().getLinhaOuOperacao(),
                        row.entity().getOrigemSm(),
                        row.entity().getDestinoSm(),
                        row.entity().getOrigemDestino(),
                        row.entity().getOrigem(),
                        row.entity().getOrdem(),
                        row.entity().getDestino(),
                        row.entity().getHorarioCorteSm(),
                        row.entity().getPrevisaoChegadaDestino(),
                        row.entity().getTransitTime(),
                        formatar(row.entity().getInicio()),
                        formatar(row.entity().getManifestado()),
                        formatar(row.entity().getSmGerada()),
                        formatar(row.entity().getCorte()),
                        formatar(row.entity().getSaidaEfetiva()),
                        formatar(row.entity().getHorarioCorte()),
                        row.entity().getSaiuNoHorario(),
                        row.entity().getAtrasoMinutos(),
                        row.entity().getObservacao(),
                        row.entity().getNomeArquivo(),
                        formatar(row.entity().getImportadoEm()),
                        row.entity().getImportadoPor()
                ))
                .toList();
    }

    public PaginaDTO<HorarioCorteRowDTO> buscarHorariosCorteTabelaPaginada(FiltroConsultaDTO filtro, int pagina, int tamanhoPagina) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        PaginaDTO<VisaoHorariosCorteEntity> paginaFonte = rasterSqlRepository.findPageByDataBetween(
                filtro.dataInicio(),
                filtro.dataFim(),
                pagina,
                tamanhoPagina
        );
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        HorarioCorteFilialMapperService.FilialMappingContext mappingContext = filialMapperService.criarContextoRasterPadrao();

        List<HorarioCorteRowDTO> conteudo = paginaFonte.conteudo().stream()
                .map(row -> new HorarioCorteRegistroResolvido(row, resolverFilial(row, mappingContext)))
                .filter(row -> escopo.permiteAlgumaFilial(row.filial()))
                .filter(row -> filtro.corresponde("filiais", row.filial()))
                .map(row -> new HorarioCorteRowDTO(
                        Objects.requireNonNullElse(row.entity().getId(), 0L),
                        formatar(row.entity().getData()),
                        row.filial(),
                        row.entity().getLinhaOuOperacao(),
                        row.entity().getOrigemSm(),
                        row.entity().getDestinoSm(),
                        row.entity().getOrigemDestino(),
                        row.entity().getOrigem(),
                        row.entity().getOrdem(),
                        row.entity().getDestino(),
                        row.entity().getHorarioCorteSm(),
                        row.entity().getPrevisaoChegadaDestino(),
                        row.entity().getTransitTime(),
                        formatar(row.entity().getInicio()),
                        formatar(row.entity().getManifestado()),
                        formatar(row.entity().getSmGerada()),
                        formatar(row.entity().getCorte()),
                        formatar(row.entity().getSaidaEfetiva()),
                        formatar(row.entity().getHorarioCorte()),
                        row.entity().getSaiuNoHorario(),
                        row.entity().getAtrasoMinutos(),
                        row.entity().getObservacao(),
                        row.entity().getNomeArquivo(),
                        formatar(row.entity().getImportadoEm()),
                        row.entity().getImportadoPor()
                ))
                .toList();

        return new PaginaDTO<>(
                conteudo,
                paginaFonte.totalElementos(),
                paginaFonte.totalPaginas(),
                paginaFonte.paginaAtual(),
                paginaFonte.tamanhoPagina()
        );
    }

    private List<HorarioCorteRegistroResolvido> buscarHorariosCorte(FiltroConsultaDTO filtro) {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        HorarioCorteFilialMapperService.FilialMappingContext mappingContext = filialMapperService.criarContextoRasterPadrao();
        return buscarHorariosCorteNaFonteComCache(filtro).stream()
                .map(row -> new HorarioCorteRegistroResolvido(row, resolverFilial(row, mappingContext)))
                .filter(row -> escopo.permiteAlgumaFilial(row.filial()))
                .filter(row -> filtro.corresponde("filiais", row.filial()))
                .toList();
    }

    private List<VisaoHorariosCorteEntity> buscarHorariosCorteNaFonteComCache(FiltroConsultaDTO filtro) {
        HorarioCorteFonteCacheKey key = new HorarioCorteFonteCacheKey(filtro.dataInicio(), filtro.dataFim());
        Instant agora = Instant.now();
        HorarioCorteFonteCacheEntry novaEntry = new HorarioCorteFonteCacheEntry(
                new CompletableFuture<>(),
                agora.plus(CACHE_FONTE_TTL)
        );

        HorarioCorteFonteCacheEntry entry = horariosCorteFonteCache.compute(key, (cacheKey, existente) ->
                existente != null && existente.validaEm(agora) ? existente : novaEntry
        );

        if (entry == novaEntry) {
            try {
                List<VisaoHorariosCorteEntity> rows = carregarHorariosCorteNaFonte(filtro);
                novaEntry.future().complete(rows);
                return rows;
            } catch (RuntimeException ex) {
                novaEntry.future().completeExceptionally(ex);
                horariosCorteFonteCache.remove(key, novaEntry);
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

    private List<VisaoHorariosCorteEntity> carregarHorariosCorteNaFonte(FiltroConsultaDTO filtro) {
        return rasterSqlRepository.findByDataBetween(filtro.dataInicio(), filtro.dataFim());
    }

    private boolean isCalculavelParaKpi(HorarioCorteRegistroResolvido row) {
        VisaoHorariosCorteEntity entity = row.entity();
        return entity.getData() != null
                && entity.getSaidaEfetiva() != null
                && entity.getHorarioCorte() != null
                && entity.getSaiuNoHorario() != null;
    }

    private String resolverFilial(
            VisaoHorariosCorteEntity row,
            HorarioCorteFilialMapperService.FilialMappingContext mappingContext
    ) {
        String filialAtual = row.getFilial();
        if (filialAtual != null
                && !filialAtual.isBlank()
                && !HorarioCorteFilialMapperService.FILIAL_NAO_MAPEADA.equalsIgnoreCase(filialAtual.trim())) {
            return filialAtual.trim();
        }
        return filialMapperService.mapearFilialCanonica(row.getLinhaOuOperacao(), mappingContext);
    }

    private double percentual(long numerador, long denominador) {
        if (denominador <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf((numerador * 100.0) / denominador)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String chaveSerie(LocalDate data, String filial) {
        return formatar(data) + "|" + (filial == null ? "" : filial);
    }

    private String formatar(LocalDate data) {
        return data != null ? data.format(DATE_FMT) : null;
    }

    private String formatar(LocalTime time) {
        return time != null ? time.format(TIME_FMT) : null;
    }

    private String formatar(LocalDateTime dataHora) {
        return dataHora != null ? dataHora.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    private record HorarioCorteRegistroResolvido(
            VisaoHorariosCorteEntity entity,
            String filial
    ) {
    }

    private record HorarioCorteFonteCacheKey(LocalDate dataInicio, LocalDate dataFim) {
    }

    private record HorarioCorteFonteCacheEntry(
            CompletableFuture<List<VisaoHorariosCorteEntity>> future,
            Instant expiraEm
    ) {
        boolean validaEm(Instant instante) {
            return expiraEm.isAfter(instante);
        }
    }
}
