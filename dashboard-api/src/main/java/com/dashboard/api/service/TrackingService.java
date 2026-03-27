package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.tracking.TrackingChartsDTO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);
    private static final Set<String> STATUS_EM_TRANSITO = Set.of("Em entrega", "Em transferência", "Manifestado");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoLocalizacaoCargasRepository repository;
    private final EscopoFilialService escopoFilialService;
    private final PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper;

    TrackingService(ValidadorPeriodoService validadorPeriodo, VisaoLocalizacaoCargasRepository repository) {
        this(validadorPeriodo, repository, escopoSemRestricao(), PeriodoOffsetDateTimeHelper.padrao());
    }

    @Autowired
    public TrackingService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoLocalizacaoCargasRepository repository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.repository = repository;
        this.escopoFilialService = escopoFilialService;
        this.periodoOffsetDateTimeHelper = periodoOffsetDateTimeHelper;
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

        OffsetDateTime agora = OffsetDateTime.now();
        int previsaoVencida = (int) cargas.stream()
                .filter(c -> previsaoVencida(c, agora))
                .count();

        BigDecimal valorFreteEmCarteira = cargas.stream()
                .map(VisaoLocalizacaoCargasEntity::getValorFrete)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pesoTaxadoTotal = cargas.stream()
                .map(c -> ConsultaFiltroUtils.parseBigDecimal(c.getPesoTaxado()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double pctFinalizado = percentual(cargas.stream()
                .filter(c -> "Finalizado".equalsIgnoreCase(c.getStatusCarga()))
                .count(), totalCargas);

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
        OffsetDateTime agora = OffsetDateTime.now();

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
                .filter(c -> previsaoVencida(c, agora))
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

    private double percentual(long valor, int total) {
        if (total == 0) {
            return 0.0;
        }

        return BigDecimal.valueOf(valor)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private boolean previsaoVencida(VisaoLocalizacaoCargasEntity carga, OffsetDateTime agora) {
        return carga.getPrevisaoEntrega() != null
                && carga.getStatusCarga() != null
                && carga.getPrevisaoEntrega().isBefore(agora)
                && !"Finalizado".equalsIgnoreCase(carga.getStatusCarga());
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

    private static EscopoFilialService escopoSemRestricao() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }
}
