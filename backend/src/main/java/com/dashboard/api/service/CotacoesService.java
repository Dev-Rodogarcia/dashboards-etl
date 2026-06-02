package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.cotacoes.CotacoesAgrupamentoDTO;
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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CotacoesService {

    private static final Logger log = LoggerFactory.getLogger(CotacoesService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoCotacoesRepository repository;
    private final CotacoesDashboardSqlRepository dashboardSqlRepository;
    private final EscopoFilialService escopoFilialService;
    private final PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper;

    CotacoesService(ValidadorPeriodoService validadorPeriodo, VisaoCotacoesRepository repository) {
        this(validadorPeriodo, repository, null, escopoSemRestricao(), PeriodoOffsetDateTimeHelper.padrao());
    }

    @Autowired
    public CotacoesService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoCotacoesRepository repository,
            CotacoesDashboardSqlRepository dashboardSqlRepository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.repository = repository;
        this.dashboardSqlRepository = dashboardSqlRepository;
        this.escopoFilialService = escopoFilialService;
        this.periodoOffsetDateTimeHelper = periodoOffsetDateTimeHelper;
    }

    public CotacoesOverviewDTO buscarOverview(LocalDate dataInicio, LocalDate dataFim) {
        return buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public CotacoesOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        if (dashboardSqlRepository != null) {
            CotacoesOverviewDTO overview = dashboardSqlRepository.buscarOverview(filtro);
            log.info("Overview cotacoes calculado via SQL: total={}, periodo={} a {}",
                    overview.totalCotacoes(), filtro.dataInicio(), filtro.dataFim());
            return overview;
        }

        List<VisaoCotacoesEntity> cotacoes = buscarRegistros(filtro);
        int totalCotacoes = cotacoes.size();

        if (totalCotacoes == 0) {
            return new CotacoesOverviewDTO(
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }

        BigDecimal valorPotencial = cotacoes.stream()
                .map(VisaoCotacoesEntity::getValorFrete)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorConvertido = cotacoes.stream()
                .filter(this::isConvertida)
                .map(VisaoCotacoesEntity::getValorFrete)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long convertidas = cotacoes.stream().filter(this::isConvertida).count();
        long reprovadas = cotacoes.stream().filter(this::isReprovada).count();

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
                .filter(c -> isConvertida(c) && c.getCteEmissao() != null)
                .count(), totalCotacoes);

        double taxaConversaoNfse = percentual(cotacoes.stream()
                .filter(c -> c.getNfseEmissao() != null)
                .count(), totalCotacoes);

        double conversaoValor = percentual(valorConvertido, valorPotencial);
        double conversaoQuantidade = percentual(convertidas, totalCotacoes);
        double reprovacaoPercentual = percentual(reprovadas, totalCotacoes);

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
                valorConvertido.setScale(2, RoundingMode.HALF_UP),
                freteMedio,
                freteKgMedio,
                conversaoValor,
                conversaoQuantidade,
                reprovacaoPercentual,
                taxaConversaoCte,
                taxaConversaoNfse,
                BigDecimal.valueOf(tempoMedioConversaoHoras).setScale(2, RoundingMode.HALF_UP).doubleValue()
        );
    }

    public List<CotacoesTrendPointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        if (dashboardSqlRepository != null) {
            return dashboardSqlRepository.buscarSerie(filtro);
        }

        return buscarRegistros(filtro).stream()
                .filter(c -> c.getDataCotacao() != null)
                .collect(Collectors.groupingBy(c -> c.getDataCotacao().toLocalDate()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<VisaoCotacoesEntity> grupo = entry.getValue();
                    int convertidas = (int) grupo.stream().filter(this::isConvertida).count();
                    int reprovadas = (int) grupo.stream().filter(this::isReprovada).count();
                    BigDecimal valorPotencial = somarValorFrete(grupo);
                    BigDecimal valorConvertido = somarValorFrete(grupo.stream().filter(this::isConvertida).toList());
                    return new CotacoesTrendPointDTO(
                            entry.getKey().format(DATE_FMT),
                            grupo.size(),
                            convertidas,
                            reprovadas,
                            valorPotencial,
                            valorConvertido
                    );
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
                    .map(this::toResumo)
                    .toList();
        }

        return repository.findAll(
                        criarSpecification(filtro),
                        PageRequest.of(0, limiteAplicado, Sort.by(Sort.Direction.DESC, "dataCotacao"))
                ).getContent().stream()
                .map(this::toResumo)
                .toList();
    }

    private CotacaoResumoDTO toResumo(VisaoCotacoesEntity c) {
        BigDecimal pesoTaxado = ConsultaFiltroUtils.zeroSeNulo(c.getPesoTaxado()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorNf = ConsultaFiltroUtils.zeroSeNulo(c.getValorNf()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorFrete = ConsultaFiltroUtils.zeroSeNulo(c.getValorFrete()).setScale(2, RoundingMode.HALF_UP);
        return new CotacaoResumoDTO(
                c.getSequenceCode(),
                c.getDataCotacao() != null ? c.getDataCotacao().toString() : null,
                c.getFilial(),
                c.getSolicitante(),
                c.getClientePagador(),
                c.getCliente(),
                c.getTrecho(),
                valorFrete,
                c.getStatusConversao(),
                c.getMotivoPerda(),
                c.getTipoOperacao(),
                inteiroOuNulo(c.getVolume()),
                pesoTaxado,
                dividir(valorFrete, pesoTaxado),
                ConsultaFiltroUtils.zeroSeNulo(c.getMinFreteKg()).setScale(2, RoundingMode.HALF_UP),
                valorNf,
                percentualDecimal(valorFrete, valorNf),
                c.getTabela(),
                textoOuPadrao(c.getOrigem(), cidadeUf(c.getCidadeOrigem(), c.getUfOrigem())),
                textoOuPadrao(c.getDestino(), cidadeUf(c.getCidadeDestino(), c.getUfDestino())),
                c.getCteEmissao() != null ? c.getCteEmissao().toString() : null,
                c.getNfseEmissao() != null ? c.getNfseEmissao().toString() : null
        );
    }

    public CotacoesChartsDTO buscarGraficos(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        if (dashboardSqlRepository != null) {
            return dashboardSqlRepository.buscarGraficos(filtro);
        }

        List<VisaoCotacoesEntity> cotacoes = buscarRegistros(filtro);

        List<CotacoesFunilDTO> funil = cotacoes.stream()
                .collect(Collectors.groupingBy(c -> textoOuPadrao(c.getStatusConversao(), "Sem status")))
                .entrySet().stream()
                .map(entry -> new CotacoesFunilDTO(
                        entry.getKey(),
                        entry.getValue().size(),
                        somarValorFrete(entry.getValue())
                ))
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

        List<CotacoesMotivoPerdaDTO> motivosPerda = agruparPerdas(
                cotacoes,
                VisaoCotacoesEntity::getMotivoPerda,
                "Sem motivo",
                10
        );

        List<CotacoesAgrupamentoDTO> trechosMaisValiosos = agruparCotacoes(
                cotacoes,
                VisaoCotacoesEntity::getTrecho,
                "Sem trecho",
                10
        );
        List<CotacoesAgrupamentoDTO> trechosPorUfOrigem = agruparCotacoes(
                cotacoes,
                VisaoCotacoesEntity::getUfOrigem,
                "Sem UF origem",
                10
        );
        List<CotacoesAgrupamentoDTO> trechosPorUfDestino = agruparCotacoes(
                cotacoes,
                VisaoCotacoesEntity::getUfDestino,
                "Sem UF destino",
                10
        );
        List<CotacoesAgrupamentoDTO> conversaoPorTipoOperacao = agruparCotacoes(
                cotacoes,
                c -> normalizarTipoOperacao(c.getTipoOperacao(), c.getTabela()),
                "Outros",
                10
        );
        List<CotacoesMotivoPerdaDTO> perdasPorCliente = agruparPerdas(
                cotacoes,
                VisaoCotacoesEntity::getClientePagador,
                "Sem cliente",
                10
        );
        List<CotacoesMotivoPerdaDTO> perdasPorTrecho = agruparPerdas(
                cotacoes,
                VisaoCotacoesEntity::getTrecho,
                "Sem trecho",
                10
        );

        return new CotacoesChartsDTO(
                funil,
                corredoresMaisValiosos,
                motivosPerda,
                trechosMaisValiosos,
                trechosPorUfOrigem,
                trechosPorUfDestino,
                conversaoPorTipoOperacao,
                perdasPorCliente,
                perdasPorTrecho
        );
    }

    private List<CotacoesAgrupamentoDTO> agruparCotacoes(
            List<VisaoCotacoesEntity> cotacoes,
            Function<VisaoCotacoesEntity, String> groupBy,
            String padrao,
            int limite
    ) {
        return cotacoes.stream()
                .collect(Collectors.groupingBy(c -> textoOuPadrao(groupBy.apply(c), padrao)))
                .entrySet().stream()
                .map(entry -> {
                    List<VisaoCotacoesEntity> grupo = entry.getValue();
                    BigDecimal valorPotencial = somarValorFrete(grupo);
                    BigDecimal valorConvertido = somarValorFrete(grupo.stream().filter(this::isConvertida).toList());
                    int convertidas = (int) grupo.stream().filter(this::isConvertida).count();
                    int reprovadas = (int) grupo.stream().filter(this::isReprovada).count();
                    return new CotacoesAgrupamentoDTO(
                            entry.getKey(),
                            valorPotencial,
                            valorConvertido,
                            grupo.size(),
                            convertidas,
                            reprovadas
                    );
                })
                .sorted(Comparator.comparing(CotacoesAgrupamentoDTO::valorPotencial).reversed()
                        .thenComparing(CotacoesAgrupamentoDTO::nome))
                .limit(limite)
                .toList();
    }

    private List<CotacoesMotivoPerdaDTO> agruparPerdas(
            List<VisaoCotacoesEntity> cotacoes,
            Function<VisaoCotacoesEntity, String> groupBy,
            String padrao,
            int limite
    ) {
        return cotacoes.stream()
                .filter(this::isReprovada)
                .collect(Collectors.groupingBy(c -> textoOuPadrao(groupBy.apply(c), padrao), Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new CotacoesMotivoPerdaDTO(entry.getKey(), entry.getValue().intValue()))
                .sorted(Comparator.comparing(CotacoesMotivoPerdaDTO::total).reversed()
                        .thenComparing(CotacoesMotivoPerdaDTO::motivo))
                .limit(limite)
                .toList();
    }

    private BigDecimal somarValorFrete(List<VisaoCotacoesEntity> cotacoes) {
        return cotacoes.stream()
                .map(VisaoCotacoesEntity::getValorFrete)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
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

    private double percentual(BigDecimal valor, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        return ConsultaFiltroUtils.zeroSeNulo(valor)
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private BigDecimal dividir(BigDecimal valor, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return ConsultaFiltroUtils.zeroSeNulo(valor)
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentualDecimal(BigDecimal valor, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return ConsultaFiltroUtils.zeroSeNulo(valor)
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
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

    private boolean isConvertida(VisaoCotacoesEntity cotacao) {
        String status = normalizarStatus(cotacao.getStatusConversao());
        return "convertida".equals(status) || "convertido".equals(status);
    }

    private boolean isReprovada(VisaoCotacoesEntity cotacao) {
        String status = normalizarStatus(cotacao.getStatusConversao());
        return "reprovada".equals(status)
                || "reprovado".equals(status)
                || "perdida".equals(status)
                || "perdido".equals(status);
    }

    private String normalizarStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarTipoOperacao(String tipoOperacao, String tabela) {
        String texto = ((tipoOperacao == null ? "" : tipoOperacao) + " " + (tabela == null ? "" : tabela)).toUpperCase(Locale.ROOT);
        if (texto.contains("PTL") || texto.contains("FRAC / DED") || (texto.contains("FRAC") && texto.contains("DED")) || texto.contains("PARCIAL")) {
            return "PTL";
        }
        if (texto.contains("FTL") || texto.contains("FECHAD") || texto.contains("DEDICAD")) {
            return "FTL";
        }
        if (texto.contains("LTL") || texto.contains("FRACIONAD")) {
            return "LTL";
        }
        return textoOuPadrao(tipoOperacao, "Outros");
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String textoOuPadrao(String valor, String padrao) {
        return Objects.requireNonNullElse(valor, "").isBlank() ? padrao : valor;
    }

    private Integer inteiroOuNulo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        BigDecimal decimal = ConsultaFiltroUtils.parseBigDecimalOrNull(valor);
        return decimal != null ? decimal.intValue() : null;
    }

    private String cidadeUf(String cidade, String uf) {
        if (!temTexto(cidade) && !temTexto(uf)) {
            return null;
        }
        if (!temTexto(cidade)) {
            return uf;
        }
        if (!temTexto(uf)) {
            return cidade;
        }
        return cidade + " - " + uf;
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
                ConsultaSpecificationUtils.filtroTexto(filtro, "tabelas", "tabela"),
                ConsultaSpecificationUtils.filtroChaveNormalizada(filtro, "usuarios", "usuarioKey")
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
