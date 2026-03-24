package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.manifestos.ManifestoResumoDTO;
import com.dashboard.api.dto.manifestos.ManifestosChartsDTO;
import com.dashboard.api.dto.manifestos.ManifestosComposicaoCustoDTO;
import com.dashboard.api.dto.manifestos.ManifestosCustoPorFilialDTO;
import com.dashboard.api.dto.manifestos.ManifestosOcupacaoScatterDTO;
import com.dashboard.api.dto.manifestos.ManifestosOverviewDTO;
import com.dashboard.api.dto.manifestos.ManifestosRankingMotoristaDTO;
import com.dashboard.api.dto.manifestos.ManifestosTrendPointDTO;
import com.dashboard.api.model.VisaoManifestosEntity;
import com.dashboard.api.repository.VisaoManifestosRepository;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ManifestosService {

    private static final Logger log = LoggerFactory.getLogger(ManifestosService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoManifestosRepository repository;
    private final EscopoFilialService escopoFilialService;

    ManifestosService(ValidadorPeriodoService validadorPeriodo, VisaoManifestosRepository repository) {
        this(validadorPeriodo, repository, escopoSemRestricao());
    }

    @Autowired
    public ManifestosService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoManifestosRepository repository,
            EscopoFilialService escopoFilialService
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.repository = repository;
        this.escopoFilialService = escopoFilialService;
    }

    public ManifestosOverviewDTO buscarOverview(LocalDate dataInicio, LocalDate dataFim) {
        return buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public ManifestosOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoManifestosEntity> manifestos = buscarRegistros(filtro);
        int totalManifestos = manifestos.size();

        if (totalManifestos == 0) {
            return new ManifestosOverviewDTO(
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, 0.0
            );
        }

        int emTransito = (int) manifestos.stream()
                .filter(m -> "em trânsito".equalsIgnoreCase(m.getStatus()) || "em transito".equalsIgnoreCase(m.getStatus()))
                .count();

        int encerrados = (int) manifestos.stream()
                .filter(m -> "encerrado".equalsIgnoreCase(m.getStatus()))
                .count();

        BigDecimal kmTotal = manifestos.stream()
                .map(VisaoManifestosEntity::getKmTotal)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal custoTotal = manifestos.stream()
                .map(VisaoManifestosEntity::getCustoTotal)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal custoPorKm = kmTotal.compareTo(BigDecimal.ZERO) > 0
                ? custoTotal.divide(kmTotal, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<BigDecimal> ocupacoesPeso = manifestos.stream()
                .filter(m -> m.getCapacidadeKg() != null && m.getCapacidadeKg().compareTo(BigDecimal.ZERO) > 0)
                .map(m -> ConsultaFiltroUtils.zeroSeNulo(m.getTotalPesoTaxado())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(m.getCapacidadeKg(), 4, RoundingMode.HALF_UP))
                .toList();

        double ocupacaoPesoMediaPct = ocupacoesPeso.isEmpty() ? 0.0
                : ocupacoesPeso.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(ocupacoesPeso.size()), 2, RoundingMode.HALF_UP)
                .doubleValue();

        List<BigDecimal> ocupacoesCubagem = manifestos.stream()
                .filter(m -> m.getVeiculoPesoCubado() != null && m.getVeiculoPesoCubado().compareTo(BigDecimal.ZERO) > 0)
                .map(m -> ConsultaFiltroUtils.zeroSeNulo(m.getTotalM3())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(m.getVeiculoPesoCubado(), 4, RoundingMode.HALF_UP))
                .toList();

        double ocupacaoCubagemMediaPct = ocupacoesCubagem.isEmpty() ? 0.0
                : ocupacoesCubagem.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(ocupacoesCubagem.size()), 2, RoundingMode.HALF_UP)
                .doubleValue();

        log.info("Overview manifestos calculado: total={}, periodo={} a {}", totalManifestos, filtro.dataInicio(), filtro.dataFim());

        return new ManifestosOverviewDTO(
                ConsultaFiltroUtils.latestUpdate(manifestos, VisaoManifestosEntity::getDataExtracao),
                totalManifestos,
                emTransito,
                encerrados,
                kmTotal.setScale(2, RoundingMode.HALF_UP),
                custoTotal.setScale(2, RoundingMode.HALF_UP),
                custoPorKm,
                ocupacaoPesoMediaPct,
                ocupacaoCubagemMediaPct
        );
    }

    public List<ManifestosTrendPointDTO> buscarSerieTemporal(LocalDate dataInicio, LocalDate dataFim) {
        return buscarSerieTemporal(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public List<ManifestosTrendPointDTO> buscarSerieTemporal(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        Map<LocalDate, List<VisaoManifestosEntity>> agrupado = buscarRegistros(filtro).stream()
                .filter(m -> m.getDataCriacao() != null)
                .collect(Collectors.groupingBy(m -> m.getDataCriacao().toLocalDate()));

        return agrupado.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<VisaoManifestosEntity> grupo = entry.getValue();

                    int encerrado = (int) grupo.stream().filter(m -> "encerrado".equalsIgnoreCase(m.getStatus())).count();
                    int emTransito = (int) grupo.stream().filter(m -> "em trânsito".equalsIgnoreCase(m.getStatus()) || "em transito".equalsIgnoreCase(m.getStatus())).count();
                    int pendente = grupo.size() - encerrado - emTransito;

                    return new ManifestosTrendPointDTO(
                            entry.getKey().format(DATE_FMT),
                            encerrado,
                            emTransito,
                            pendente
                    );
                })
                .toList();
    }

    public List<ManifestoResumoDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 200);
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        if (deveUsarConsultaLegada(filtro, escopo)) {
            return repository.findByDataCriacaoBetween(
                            filtro.dataInicio().atStartOfDay().atOffset(ZoneOffset.UTC),
                            filtro.dataFim().atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
                    ).stream()
                    .sorted(Comparator.comparing(VisaoManifestosEntity::getDataCriacao, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(limiteAplicado)
                    .map(m -> new ManifestoResumoDTO(
                            m.getNumero(),
                            m.getIdentificadorUnico(),
                            m.getStatus(),
                            m.getClassificacao(),
                            m.getFilial(),
                            m.getDataCriacao() != null ? m.getDataCriacao().toString() : null,
                            m.getFechamento() != null ? m.getFechamento().toString() : null,
                            m.getMotorista(),
                            m.getVeiculoPlaca(),
                            m.getTipoVeiculo(),
                            ConsultaFiltroUtils.zeroSeNulo(m.getTotalPesoTaxado()).setScale(2, RoundingMode.HALF_UP),
                            ConsultaFiltroUtils.zeroSeNulo(m.getTotalM3()).setScale(2, RoundingMode.HALF_UP),
                            ConsultaFiltroUtils.zeroSeNulo(m.getCustoTotal()).setScale(2, RoundingMode.HALF_UP),
                            ConsultaFiltroUtils.zeroSeNulo(m.getValorFrete()).setScale(2, RoundingMode.HALF_UP),
                            ConsultaFiltroUtils.zeroSeNulo(m.getCombustivel()).setScale(2, RoundingMode.HALF_UP),
                            ConsultaFiltroUtils.zeroSeNulo(m.getPedagio()).setScale(2, RoundingMode.HALF_UP),
                            ConsultaFiltroUtils.zeroSeNulo(m.getSaldoPagar()).setScale(2, RoundingMode.HALF_UP),
                            ConsultaFiltroUtils.zeroSeNulo(m.getKmTotal()).setScale(2, RoundingMode.HALF_UP),
                            m.getItensTotal()
                    ))
                    .toList();
        }

        return repository.findAll(
                        criarSpecification(filtro),
                        PageRequest.of(0, limiteAplicado, Sort.by(Sort.Direction.DESC, "dataCriacao"))
                ).getContent().stream()
                .map(m -> new ManifestoResumoDTO(
                        m.getNumero(),
                        m.getIdentificadorUnico(),
                        m.getStatus(),
                        m.getClassificacao(),
                        m.getFilial(),
                        m.getDataCriacao() != null ? m.getDataCriacao().toString() : null,
                        m.getFechamento() != null ? m.getFechamento().toString() : null,
                        m.getMotorista(),
                        m.getVeiculoPlaca(),
                        m.getTipoVeiculo(),
                        ConsultaFiltroUtils.zeroSeNulo(m.getTotalPesoTaxado()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(m.getTotalM3()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(m.getCustoTotal()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(m.getValorFrete()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(m.getCombustivel()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(m.getPedagio()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(m.getSaldoPagar()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(m.getKmTotal()).setScale(2, RoundingMode.HALF_UP),
                        m.getItensTotal()
                ))
                .toList();
    }

    public ManifestosChartsDTO buscarGraficos(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoManifestosEntity> manifestos = buscarRegistros(filtro);

        List<ManifestosCustoPorFilialDTO> custoPorFilial = manifestos.stream()
                .collect(Collectors.groupingBy(m -> textoOuPadrao(m.getFilial(), "Sem filial")))
                .entrySet().stream()
                .map(entry -> new ManifestosCustoPorFilialDTO(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(VisaoManifestosEntity::getCustoTotal)
                                .map(ConsultaFiltroUtils::zeroSeNulo)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP)
                ))
                .sorted(Comparator.comparing(ManifestosCustoPorFilialDTO::custoTotal).reversed()
                        .thenComparing(ManifestosCustoPorFilialDTO::filial))
                .toList();

        List<ManifestosRankingMotoristaDTO> rankingMotorista = manifestos.stream()
                .collect(Collectors.groupingBy(m -> textoOuPadrao(m.getMotorista(), "Sem motorista")))
                .entrySet().stream()
                .map(entry -> new ManifestosRankingMotoristaDTO(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .map(VisaoManifestosEntity::getKmTotal)
                                .map(ConsultaFiltroUtils::zeroSeNulo)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP),
                        entry.getValue().stream()
                                .map(VisaoManifestosEntity::getCustoTotal)
                                .map(ConsultaFiltroUtils::zeroSeNulo)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP)
                ))
                .sorted(Comparator.comparing(ManifestosRankingMotoristaDTO::custoTotal).reversed()
                        .thenComparing(ManifestosRankingMotoristaDTO::motorista))
                .limit(10)
                .toList();

        BigDecimal combustivel = manifestos.stream()
                .map(VisaoManifestosEntity::getCombustivel)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pedagio = manifestos.stream()
                .map(VisaoManifestosEntity::getPedagio)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldoPagar = manifestos.stream()
                .map(VisaoManifestosEntity::getSaldoPagar)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outros = manifestos.stream()
                .map(m -> ConsultaFiltroUtils.zeroSeNulo(m.getCustoTotal())
                        .subtract(ConsultaFiltroUtils.zeroSeNulo(m.getCombustivel()))
                        .subtract(ConsultaFiltroUtils.zeroSeNulo(m.getPedagio()))
                        .subtract(ConsultaFiltroUtils.zeroSeNulo(m.getSaldoPagar()))
                        .max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ManifestosComposicaoCustoDTO> composicaoCusto = List.of(
                        new ManifestosComposicaoCustoDTO("Combustivel", combustivel.setScale(2, RoundingMode.HALF_UP)),
                        new ManifestosComposicaoCustoDTO("Pedagio", pedagio.setScale(2, RoundingMode.HALF_UP)),
                        new ManifestosComposicaoCustoDTO("Saldo a Pagar", saldoPagar.setScale(2, RoundingMode.HALF_UP)),
                        new ManifestosComposicaoCustoDTO("Outros", outros.setScale(2, RoundingMode.HALF_UP))
                ).stream()
                .filter(item -> item.valor().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        List<ManifestosOcupacaoScatterDTO> ocupacaoScatter = manifestos.stream()
                .sorted(Comparator.comparing(VisaoManifestosEntity::getDataCriacao, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(80)
                .map(m -> new ManifestosOcupacaoScatterDTO(
                        ConsultaFiltroUtils.zeroSeNulo(m.getTotalPesoTaxado()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(m.getTotalM3()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(m.getCustoTotal()).setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();

        return new ManifestosChartsDTO(custoPorFilial, rankingMotorista, composicaoCusto, ocupacaoScatter);
    }

    private List<VisaoManifestosEntity> buscarRegistros(FiltroConsultaDTO filtro) {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        if (deveUsarConsultaLegada(filtro, escopo)) {
            return repository.findByDataCriacaoBetween(
                    filtro.dataInicio().atStartOfDay().atOffset(ZoneOffset.UTC),
                    filtro.dataFim().atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
            );
        }
        return repository.findAll(criarSpecification(filtro));
    }

    private String textoOuPadrao(String valor, String padrao) {
        return Objects.requireNonNullElse(valor, "").isBlank() ? padrao : valor;
    }

    @NonNull
    private Specification<VisaoManifestosEntity> criarSpecification(FiltroConsultaDTO filtro) {
        OffsetDateTime inicio = filtro.dataInicio().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fim = filtro.dataFim().atTime(23, 59, 59).atOffset(ZoneOffset.UTC);
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.between("dataCriacao", inicio, fim),
                ConsultaSpecificationUtils.escopoFiliais(escopo, "filial"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "filiais", "filial"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "status", "status"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "motoristas", "motorista"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "veiculos", "veiculoPlaca"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "tiposCarga", "tipoCarga"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "tiposContrato", "tipoContrato")
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
