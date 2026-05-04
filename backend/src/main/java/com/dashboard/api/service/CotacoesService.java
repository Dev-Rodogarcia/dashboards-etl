package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.cotacoes.CotacoesChartsDTO;
import com.dashboard.api.dto.cotacoes.CotacoesCorredorValiosoDTO;
import com.dashboard.api.dto.cotacoes.CotacoesFunilDTO;
import com.dashboard.api.dto.cotacoes.CotacoesMotivoPerdaDTO;
import com.dashboard.api.dto.cotacoes.CotacaoResumoDTO;
import com.dashboard.api.dto.cotacoes.CotacoesOverviewDTO;
import com.dashboard.api.dto.cotacoes.CotacoesTrendPointDTO;
import com.dashboard.api.model.VisaoCotacoesEntity;
import com.dashboard.api.repository.VisaoCotacoesRepository;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CotacoesService {

    private static final Logger log = LoggerFactory.getLogger(CotacoesService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoCotacoesRepository repository;
    private final EscopoFilialService escopoFilialService;
    private final PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper;

    CotacoesService(ValidadorPeriodoService validadorPeriodo, VisaoCotacoesRepository repository) {
        this(validadorPeriodo, repository, escopoSemRestricao(), PeriodoOffsetDateTimeHelper.padrao());
    }

    @Autowired
    public CotacoesService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoCotacoesRepository repository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.repository = repository;
        this.escopoFilialService = escopoFilialService;
        this.periodoOffsetDateTimeHelper = periodoOffsetDateTimeHelper;
    }

    public CotacoesOverviewDTO buscarOverview(LocalDate dataInicio, LocalDate dataFim) {
        return buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public CotacoesOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoCotacoesEntity> cotacoes = buscarRegistros(filtro);
        int totalCotacoes = cotacoes.size();

        if (totalCotacoes == 0) {
            return new CotacoesOverviewDTO(
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    0.0, 0.0, 0.0, 0.0
            );
        }

        BigDecimal valorPotencial = cotacoes.stream()
                .map(VisaoCotacoesEntity::getValorFrete)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal freteMedio = valorPotencial.divide(BigDecimal.valueOf(totalCotacoes), 2, RoundingMode.HALF_UP);

        BigDecimal somaPesoTaxado = cotacoes.stream()
                .map(VisaoCotacoesEntity::getPesoTaxado)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .filter(valor -> valor.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal somaValorFretePeso = cotacoes.stream()
                .filter(c -> ConsultaFiltroUtils.zeroSeNulo(c.getPesoTaxado()).compareTo(BigDecimal.ZERO) > 0)
                .map(VisaoCotacoesEntity::getValorFrete)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal freteKgMedio = somaPesoTaxado.compareTo(BigDecimal.ZERO) > 0
                ? somaValorFretePeso.divide(somaPesoTaxado, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        double taxaConversaoCte = percentual(cotacoes.stream()
                .filter(c -> "Convertida".equalsIgnoreCase(c.getStatusConversao()) && c.getCteEmissao() != null)
                .count(), totalCotacoes);

        double taxaConversaoNfse = percentual(cotacoes.stream()
                .filter(c -> c.getNfseEmissao() != null)
                .count(), totalCotacoes);

        double taxaReprovacao = percentual(cotacoes.stream()
                .filter(c -> "Reprovada".equalsIgnoreCase(c.getStatusConversao()))
                .count(), totalCotacoes);

        double tempoMedioConversaoHoras = cotacoes.stream()
                .filter(c -> c.getCteEmissao() != null && c.getDataCotacao() != null)
                .mapToLong(c -> Duration.between(c.getDataCotacao(), c.getCteEmissao()).toHours())
                .average()
                .orElse(0.0);

        log.info("Overview cotacoes calculado: total={}, periodo={} a {}", totalCotacoes, filtro.dataInicio(), filtro.dataFim());

        return new CotacoesOverviewDTO(
                ConsultaFiltroUtils.latestUpdate(cotacoes, VisaoCotacoesEntity::getDataExtracao),
                totalCotacoes,
                valorPotencial.setScale(2, RoundingMode.HALF_UP),
                freteMedio,
                freteKgMedio,
                taxaConversaoCte,
                taxaConversaoNfse,
                taxaReprovacao,
                BigDecimal.valueOf(tempoMedioConversaoHoras).setScale(2, RoundingMode.HALF_UP).doubleValue()
        );
    }

    public List<CotacoesTrendPointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return buscarRegistros(filtro).stream()
                .filter(c -> c.getDataCotacao() != null)
                .collect(Collectors.groupingBy(c -> c.getDataCotacao().toLocalDate()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<VisaoCotacoesEntity> grupo = entry.getValue();
                    int convertidas = (int) grupo.stream().filter(c -> "Convertida".equalsIgnoreCase(c.getStatusConversao())).count();
                    int reprovadas = (int) grupo.stream().filter(c -> "Reprovada".equalsIgnoreCase(c.getStatusConversao())).count();
                    return new CotacoesTrendPointDTO(entry.getKey().format(DATE_FMT), grupo.size(), convertidas, reprovadas);
                })
                .toList();
    }

    public List<CotacaoResumoDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 200);
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        if (deveUsarConsultaLegada(filtro, escopo)) {
            JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
            return repository.findByDataCotacaoGreaterThanEqualAndDataCotacaoLessThan(
                            janela.inicioInclusivo(),
                            janela.fimExclusivo()
                    ).stream()
                    .sorted(Comparator.comparing(VisaoCotacoesEntity::getDataCotacao, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(limiteAplicado)
                    .map(c -> new CotacaoResumoDTO(
                            c.getSequenceCode(),
                            c.getDataCotacao() != null ? c.getDataCotacao().toString() : null,
                            c.getFilial(),
                            c.getSolicitante(),
                            c.getClientePagador(),
                            c.getCliente(),
                            c.getTrecho(),
                            ConsultaFiltroUtils.zeroSeNulo(c.getPesoTaxado()).setScale(2, RoundingMode.HALF_UP),
                            ConsultaFiltroUtils.zeroSeNulo(c.getValorNf()).setScale(2, RoundingMode.HALF_UP),
                            ConsultaFiltroUtils.zeroSeNulo(c.getValorFrete()).setScale(2, RoundingMode.HALF_UP),
                            c.getTabela(),
                            c.getStatusConversao(),
                            c.getMotivoPerda(),
                            c.getCteEmissao() != null ? c.getCteEmissao().toString() : null,
                            c.getNfseEmissao() != null ? c.getNfseEmissao().toString() : null
                    ))
                    .toList();
        }

        return repository.findAll(
                        criarSpecification(filtro),
                        PageRequest.of(0, limiteAplicado, Sort.by(Sort.Direction.DESC, "dataCotacao"))
                ).getContent().stream()
                .map(c -> new CotacaoResumoDTO(
                        c.getSequenceCode(),
                        c.getDataCotacao() != null ? c.getDataCotacao().toString() : null,
                        c.getFilial(),
                        c.getSolicitante(),
                        c.getClientePagador(),
                        c.getCliente(),
                        c.getTrecho(),
                        ConsultaFiltroUtils.zeroSeNulo(c.getPesoTaxado()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(c.getValorNf()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(c.getValorFrete()).setScale(2, RoundingMode.HALF_UP),
                        c.getTabela(),
                        c.getStatusConversao(),
                        c.getMotivoPerda(),
                        c.getCteEmissao() != null ? c.getCteEmissao().toString() : null,
                        c.getNfseEmissao() != null ? c.getNfseEmissao().toString() : null
                ))
                .toList();
    }

    public CotacoesChartsDTO buscarGraficos(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoCotacoesEntity> cotacoes = buscarRegistros(filtro);

        List<CotacoesFunilDTO> funil = cotacoes.stream()
                .collect(Collectors.groupingBy(c -> textoOuPadrao(c.getStatusConversao(), "Sem status"), Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new CotacoesFunilDTO(entry.getKey(), entry.getValue().intValue()))
                .sorted(Comparator.comparing(CotacoesFunilDTO::total).reversed()
                        .thenComparing(CotacoesFunilDTO::etapa))
                .toList();

        List<CotacoesCorredorValiosoDTO> corredoresMaisValiosos = cotacoes.stream()
                .collect(Collectors.groupingBy(c -> textoOuPadrao(c.getTrecho(), "Sem trecho")))
                .entrySet().stream()
                .map(entry -> new CotacoesCorredorValiosoDTO(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(VisaoCotacoesEntity::getValorFrete)
                                .map(ConsultaFiltroUtils::zeroSeNulo)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP),
                        entry.getValue().size()
                ))
                .sorted(Comparator.comparing(CotacoesCorredorValiosoDTO::valorFrete).reversed()
                        .thenComparing(CotacoesCorredorValiosoDTO::trecho))
                .limit(10)
                .toList();

        List<CotacoesMotivoPerdaDTO> motivosPerda = cotacoes.stream()
                .map(VisaoCotacoesEntity::getMotivoPerda)
                .filter(this::temTexto)
                .collect(Collectors.groupingBy(motivo -> motivo, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new CotacoesMotivoPerdaDTO(entry.getKey(), entry.getValue().intValue()))
                .sorted(Comparator.comparing(CotacoesMotivoPerdaDTO::total).reversed()
                        .thenComparing(CotacoesMotivoPerdaDTO::motivo))
                .toList();

        return new CotacoesChartsDTO(funil, corredoresMaisValiosos, motivosPerda);
    }

    private List<VisaoCotacoesEntity> buscarRegistros(FiltroConsultaDTO filtro) {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        if (deveUsarConsultaLegada(filtro, escopo)) {
            JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
            return repository.findByDataCotacaoGreaterThanEqualAndDataCotacaoLessThan(
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

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String textoOuPadrao(String valor, String padrao) {
        return Objects.requireNonNullElse(valor, "").isBlank() ? padrao : valor;
    }

    @NonNull
    private Specification<VisaoCotacoesEntity> criarSpecification(FiltroConsultaDTO filtro) {
        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.greaterThanOrEqualTo("dataCotacao", janela.inicioInclusivo()),
                ConsultaSpecificationUtils.lessThan("dataCotacao", janela.fimExclusivo()),
                ConsultaSpecificationUtils.escopoFiliais(escopo, "filial"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "filiais", "filial"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "clientes", "clientePagador"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "ufOrigem", "ufOrigem"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "ufDestino", "ufDestino"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "statusConversao", "statusConversao"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "tabelas", "tabela")
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
