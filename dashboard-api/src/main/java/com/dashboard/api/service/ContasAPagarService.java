package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.contaspagar.ContaPagarResumoDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarCentroCustoDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarChartsDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarConciliacaoDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarFornecedorDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarMensalTrendDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarOverviewDTO;
import com.dashboard.api.model.VisaoContasAPagarEntity;
import com.dashboard.api.repository.VisaoContasAPagarRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ContasAPagarService {

    private static final Logger log = LoggerFactory.getLogger(ContasAPagarService.class);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoContasAPagarRepository repository;
    private final EscopoFilialService escopoFilialService;

    public ContasAPagarService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoContasAPagarRepository repository,
            EscopoFilialService escopoFilialService
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.repository = repository;
        this.escopoFilialService = escopoFilialService;
    }

    public ContasAPagarOverviewDTO buscarOverview(LocalDate dataInicio, LocalDate dataFim) {
        return buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public ContasAPagarOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoContasAPagarEntity> contas = buscarRegistros(filtro);
        int total = contas.size();

        if (total == 0) {
            return new ContasAPagarOverviewDTO(
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    0.0, 0.0, 0.0
            );
        }

        BigDecimal valorAPagar = contas.stream()
                .map(VisaoContasAPagarEntity::getValorAPagar)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorPago = contas.stream()
                .map(VisaoContasAPagarEntity::getValorPago)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoAberto = valorAPagar.subtract(valorPago);

        long pagos = contas.stream()
                .filter(c -> "Sim".equalsIgnoreCase(c.getPago()) || "PAGO".equalsIgnoreCase(c.getPago()))
                .count();
        double taxaLiquidacao = percentual(pagos, total);

        double leadTimeLiquidacaoDias = contas.stream()
                .filter(c -> c.getDataLiquidacao() != null && c.getEmissao() != null)
                .mapToLong(c -> ChronoUnit.DAYS.between(c.getEmissao(), c.getDataLiquidacao()))
                .average()
                .orElse(0.0);
        leadTimeLiquidacaoDias = BigDecimal.valueOf(leadTimeLiquidacaoDias).setScale(1, RoundingMode.HALF_UP).doubleValue();

        long conciliados = contas.stream()
                .filter(c -> c.getConciliado() != null && c.getConciliado().toLowerCase().contains("conciliado"))
                .count();
        double pctConciliado = percentual(conciliados, total);

        log.info("Contas a pagar overview calculado: total={}, periodo={} a {}", total, filtro.dataInicio(), filtro.dataFim());

        return new ContasAPagarOverviewDTO(
                ConsultaFiltroUtils.latestUpdate(contas, VisaoContasAPagarEntity::getDataExtracao),
                valorAPagar.setScale(2, RoundingMode.HALF_UP),
                valorPago.setScale(2, RoundingMode.HALF_UP),
                saldoAberto.setScale(2, RoundingMode.HALF_UP),
                taxaLiquidacao,
                leadTimeLiquidacaoDias,
                pctConciliado
        );
    }

    public List<ContasAPagarMensalTrendDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        Map<YearMonth, List<VisaoContasAPagarEntity>> agrupado = buscarRegistros(filtro).stream()
                .filter(c -> c.getEmissao() != null)
                .collect(Collectors.groupingBy(c -> YearMonth.from(c.getEmissao())));

        return agrupado.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ContasAPagarMensalTrendDTO(
                        entry.getKey().format(MONTH_FMT),
                        entry.getValue().stream()
                                .map(VisaoContasAPagarEntity::getValorPago)
                                .map(ConsultaFiltroUtils::zeroSeNulo)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP),
                        entry.getValue().stream()
                                .map(VisaoContasAPagarEntity::getValorAPagar)
                                .map(ConsultaFiltroUtils::zeroSeNulo)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .subtract(entry.getValue().stream()
                                        .map(VisaoContasAPagarEntity::getValorPago)
                                        .map(ConsultaFiltroUtils::zeroSeNulo)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                                .setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    public List<ContaPagarResumoDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 200);

        return repository.findAll(
                        criarSpecification(filtro),
                        PageRequest.of(0, limiteAplicado, Sort.by(Sort.Direction.DESC, "emissao"))
                ).getContent().stream()
                .map(c -> new ContaPagarResumoDTO(
                        c.getSequenceCode(),
                        c.getDocumentoNumero(),
                        c.getEmissao() != null ? c.getEmissao().toString() : null,
                        c.getTipoLancamento(),
                        c.getFilial(),
                        c.getFornecedorNome(),
                        ConsultaFiltroUtils.zeroSeNulo(c.getValor()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(c.getValorPago()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(c.getValorAPagar()).setScale(2, RoundingMode.HALF_UP),
                        c.getClassificacaoContabil(),
                        c.getDescricaoContabil(),
                        c.getCentroCustoNome(),
                        c.getDataLiquidacao() != null ? c.getDataLiquidacao().toString() : null,
                        c.getPago(),
                        c.getConciliado()
                ))
                .toList();
    }

    public ContasAPagarChartsDTO buscarGraficos(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoContasAPagarEntity> contas = buscarRegistros(filtro);

        List<ContasAPagarFornecedorDTO> topFornecedores = contas.stream()
                .collect(Collectors.groupingBy(c -> textoOuPadrao(c.getFornecedorNome(), "Sem fornecedor")))
                .entrySet().stream()
                .map(entry -> new ContasAPagarFornecedorDTO(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(VisaoContasAPagarEntity::getValorAPagar)
                                .map(ConsultaFiltroUtils::zeroSeNulo)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP),
                        entry.getValue().size()
                ))
                .sorted(Comparator.comparing(ContasAPagarFornecedorDTO::valor).reversed()
                        .thenComparing(ContasAPagarFornecedorDTO::fornecedor))
                .limit(10)
                .toList();

        List<ContasAPagarCentroCustoDTO> centroCusto = contas.stream()
                .collect(Collectors.groupingBy(c -> textoOuPadrao(c.getCentroCustoNome(), "Sem centro")))
                .entrySet().stream()
                .map(entry -> new ContasAPagarCentroCustoDTO(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(VisaoContasAPagarEntity::getValorAPagar)
                                .map(ConsultaFiltroUtils::zeroSeNulo)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP)
                ))
                .sorted(Comparator.comparing(ContasAPagarCentroCustoDTO::valor).reversed()
                        .thenComparing(ContasAPagarCentroCustoDTO::centroCusto))
                .toList();

        List<ContasAPagarConciliacaoDTO> conciliacao = contas.stream()
                .collect(Collectors.groupingBy(c -> textoOuPadrao(c.getConciliado(), "Nao informado")))
                .entrySet().stream()
                .map(entry -> new ContasAPagarConciliacaoDTO(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .map(VisaoContasAPagarEntity::getValorAPagar)
                                .map(ConsultaFiltroUtils::zeroSeNulo)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP)
                ))
                .sorted(Comparator.comparing(ContasAPagarConciliacaoDTO::valor).reversed()
                        .thenComparing(ContasAPagarConciliacaoDTO::status))
                .toList();

        return new ContasAPagarChartsDTO(topFornecedores, centroCusto, conciliacao);
    }

    private List<VisaoContasAPagarEntity> buscarRegistros(FiltroConsultaDTO filtro) {
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

    private String textoOuPadrao(String valor, String padrao) {
        return Objects.requireNonNullElse(valor, "").isBlank() ? padrao : valor;
    }

    @NonNull
    private Specification<VisaoContasAPagarEntity> criarSpecification(FiltroConsultaDTO filtro) {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.between("emissao", filtro.dataInicio(), filtro.dataFim()),
                ConsultaSpecificationUtils.escopoFiliais(escopo, "filial"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "filiais", "filial"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "fornecedores", "fornecedorNome"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "classificacoes", "classificacaoContabil"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "centrosCusto", "centroCustoNome"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "pago", "pago"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "conciliado", "conciliado")
        );
    }
}
