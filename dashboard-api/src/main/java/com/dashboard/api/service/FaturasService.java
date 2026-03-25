package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.faturas.FaturaReconciliacaoDTO;
import com.dashboard.api.dto.faturas.FaturaResumoDTO;
import com.dashboard.api.dto.faturas.FaturasAgingBucketDTO;
import com.dashboard.api.dto.faturas.FaturasClienteTopDTO;
import com.dashboard.api.dto.faturas.FaturasMensalTrendDTO;
import com.dashboard.api.dto.faturas.FaturasOverviewDTO;
import com.dashboard.api.dto.faturas.FaturasStatusProcessoDTO;
import com.dashboard.api.model.VisaoFaturasClienteEntity;
import com.dashboard.api.model.VisaoFaturasGraphqlEntity;
import com.dashboard.api.repository.VisaoFaturasClienteRepository;
import com.dashboard.api.repository.VisaoFaturasGraphqlRepository;
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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FaturasService {

    private static final Logger log = LoggerFactory.getLogger(FaturasService.class);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoFaturasGraphqlRepository graphqlRepository;
    private final VisaoFaturasClienteRepository clienteRepository;
    private final EscopoFilialService escopoFilialService;

    FaturasService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFaturasGraphqlRepository graphqlRepository,
            VisaoFaturasClienteRepository clienteRepository
    ) {
        this(validadorPeriodo, graphqlRepository, clienteRepository, escopoSemRestricao());
    }

    @Autowired
    public FaturasService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFaturasGraphqlRepository graphqlRepository,
            VisaoFaturasClienteRepository clienteRepository,
            EscopoFilialService escopoFilialService
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.graphqlRepository = graphqlRepository;
        this.clienteRepository = clienteRepository;
        this.escopoFilialService = escopoFilialService;
    }

    public FaturasOverviewDTO buscarOverview(LocalDate dataInicio, LocalDate dataFim) {
        return buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public FaturasOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoFaturasGraphqlEntity> titulos = buscarTitulos(filtro);
        List<VisaoFaturasClienteEntity> operacionais = buscarOperacionais(filtro);

        if (titulos.isEmpty() && operacionais.isEmpty()) {
            return new FaturasOverviewDTO(
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    0.0, 0.0, 0, 0, false
            );
        }

        if (titulos.isEmpty()) {
            return new FaturasOverviewDTO(
                    latestUpdate(titulos, operacionais),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    0.0, 0.0, 0, 0, false
            );
        }

        BigDecimal valorFaturado = titulos.stream()
                .map(VisaoFaturasGraphqlEntity::getValor)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorRecebido = titulos.stream()
                .map(VisaoFaturasGraphqlEntity::getValorPago)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoAberto = titulos.stream()
                .map(VisaoFaturasGraphqlEntity::getValorAPagar)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pagos = titulos.stream()
                .filter(this::isPago)
                .count();

        double taxaAdimplencia = titulos.isEmpty() ? 0.0 : BigDecimal.valueOf(pagos)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(titulos.size()), 2, RoundingMode.HALF_UP)
                .doubleValue();

        double dsoMedioDias = titulos.stream()
                .filter(t -> t.getEmissao() != null && t.getVencimento() != null)
                .mapToLong(t -> ChronoUnit.DAYS.between(t.getEmissao(), t.getVencimento()))
                .average()
                .orElse(0.0);
        dsoMedioDias = BigDecimal.valueOf(dsoMedioDias)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        int titulosEmAtraso = (int) titulos.stream()
                .filter(t -> t.getVencimento() != null
                        && t.getVencimento().isBefore(LocalDate.now())
                        && !isPago(t))
                .count();

        int clientesAtivos = (int) operacionais.stream()
                .map(VisaoFaturasClienteEntity::getPagadorNome)
                .filter(nome -> nome != null && !nome.isBlank())
                .distinct()
                .count();

        log.info("Faturas overview calculado: titulos={}, clientes={}, periodo={} a {}",
                titulos.size(), clientesAtivos, filtro.dataInicio(), filtro.dataFim());

        return new FaturasOverviewDTO(
                latestUpdate(titulos, operacionais),
                valorFaturado.setScale(2, RoundingMode.HALF_UP),
                valorRecebido.setScale(2, RoundingMode.HALF_UP),
                saldoAberto.setScale(2, RoundingMode.HALF_UP),
                taxaAdimplencia,
                dsoMedioDias,
                titulosEmAtraso,
                clientesAtivos,
                true
        );
    }

    public List<FaturasMensalTrendDTO> buscarMensal(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoFaturasGraphqlEntity> titulos = buscarTitulos(filtro);
        if (titulos.isEmpty()) {
            return List.of();
        }

        Map<YearMonth, List<VisaoFaturasGraphqlEntity>> agrupado = titulos.stream()
                .filter(t -> t.getEmissao() != null)
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getEmissao())));

        return agrupado.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new FaturasMensalTrendDTO(
                        entry.getKey().format(MONTH_FMT),
                        somarTitulos(entry.getValue(), VisaoFaturasGraphqlEntity::getValor),
                        somarTitulos(entry.getValue(), VisaoFaturasGraphqlEntity::getValorPago),
                        somarTitulos(entry.getValue(), VisaoFaturasGraphqlEntity::getValorAPagar)
                ))
                .toList();
    }

    public List<FaturasAgingBucketDTO> buscarAging(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoFaturasGraphqlEntity> titulos = buscarTitulos(filtro);
        if (titulos.isEmpty()) {
            return List.of();
        }

        Map<String, List<VisaoFaturasGraphqlEntity>> buckets = titulos.stream()
                .filter(t -> !isPago(t))
                .collect(Collectors.groupingBy(this::agingBucket));

        List<String> ordem = List.of("A vencer", "1-15 dias", "16-30 dias", "31-60 dias", "61+ dias", "Sem vencimento");

        return ordem.stream()
                .map(bucket -> {
                    List<VisaoFaturasGraphqlEntity> grupo = buckets.getOrDefault(bucket, List.of());
                    return new FaturasAgingBucketDTO(
                            bucket,
                            somarTitulos(grupo, VisaoFaturasGraphqlEntity::getValorAPagar),
                            grupo.size()
                    );
                })
                .toList();
    }

    public List<FaturasClienteTopDTO> buscarTopClientes(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 10, 50);

        List<VisaoFaturasGraphqlEntity> titulos = buscarTitulos(filtro);
        if (titulos.isEmpty()) {
            return List.of();
        }

        Map<String, VisaoFaturasGraphqlEntity> titulosPorDocumento = titulos.stream()
                .filter(t -> t.getDocumento() != null && !t.getDocumento().isBlank())
                .collect(Collectors.toMap(
                        VisaoFaturasGraphqlEntity::getDocumento,
                        t -> t,
                        (a, b) -> a
                ));

        return buscarOperacionais(filtro).stream()
                .filter(row -> row.getPagadorNome() != null && !row.getPagadorNome().isBlank())
                .collect(Collectors.groupingBy(VisaoFaturasClienteEntity::getPagadorNome))
                .entrySet().stream()
                .map(entry -> {
                    BigDecimal faturado = entry.getValue().stream()
                            .map(this::valorOperacional)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    Set<String> documentos = entry.getValue().stream()
                            .map(VisaoFaturasClienteEntity::getDocumentoFatura)
                            .filter(doc -> doc != null && !doc.isBlank())
                            .collect(Collectors.toSet());

                    BigDecimal saldoAberto = documentos.stream()
                            .map(titulosPorDocumento::get)
                            .filter(java.util.Objects::nonNull)
                            .map(VisaoFaturasGraphqlEntity::getValorAPagar)
                            .map(ConsultaFiltroUtils::zeroSeNulo)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new FaturasClienteTopDTO(
                            entry.getKey(),
                            faturado.setScale(2, RoundingMode.HALF_UP),
                            saldoAberto.setScale(2, RoundingMode.HALF_UP)
                    );
                })
                .sorted(Comparator.comparing(FaturasClienteTopDTO::faturado).reversed())
                .limit(limiteAplicado)
                .toList();
    }

    public List<FaturasStatusProcessoDTO> buscarStatusProcesso(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        if (buscarTitulos(filtro).isEmpty()) {
            return List.of();
        }

        Map<String, Long> agrupado = buscarOperacionais(filtro).stream()
                .collect(Collectors.groupingBy(this::statusProcesso, Collectors.counting()));

        return agrupado.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new FaturasStatusProcessoDTO(entry.getKey(), entry.getValue().intValue()))
                .toList();
    }

    public List<FaturaReconciliacaoDTO> buscarReconciliacao(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 50, 200);

        List<VisaoFaturasGraphqlEntity> titulos = buscarTitulos(filtro);
        if (titulos.isEmpty()) {
            return List.of();
        }

        Map<String, VisaoFaturasGraphqlEntity> titulosPorDocumento = titulos.stream()
                .filter(t -> t.getDocumento() != null && !t.getDocumento().isBlank())
                .collect(Collectors.toMap(
                        VisaoFaturasGraphqlEntity::getDocumento,
                        t -> t,
                        (a, b) -> a
                ));

        return buscarOperacionaisPaginados(filtro, limiteAplicado).stream()
                .map(row -> {
                    VisaoFaturasGraphqlEntity titulo = titulosPorDocumento.get(row.getDocumentoFatura());
                    BigDecimal valorOperacional = valorOperacional(row);
                    BigDecimal valorFinanceiro = titulo != null ? ConsultaFiltroUtils.zeroSeNulo(titulo.getValor()) : null;

                    return new FaturaReconciliacaoDTO(
                            row.getUniqueId(),
                            row.getDocumentoFatura() != null ? row.getDocumentoFatura() : row.getUniqueId(),
                            row.getEmissaoFatura() != null ? row.getEmissaoFatura().toString() : null,
                            row.getPagadorNome(),
                            valorOperacional,
                            valorFinanceiro,
                            statusReconciliacao(valorOperacional, valorFinanceiro)
                    );
                })
                .toList();
    }

    public List<FaturaResumoDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 200);

        List<VisaoFaturasGraphqlEntity> titulos = buscarTitulos(filtro);
        if (titulos.isEmpty()) {
            return List.of();
        }

        Map<String, VisaoFaturasGraphqlEntity> titulosPorDocumento = titulos.stream()
                .filter(t -> t.getDocumento() != null && !t.getDocumento().isBlank())
                .collect(Collectors.toMap(
                        VisaoFaturasGraphqlEntity::getDocumento,
                        t -> t,
                        (a, b) -> a
                ));

        return buscarOperacionaisPaginados(filtro, limiteAplicado).stream()
                .map(row -> {
                    VisaoFaturasGraphqlEntity titulo = titulosPorDocumento.get(row.getDocumentoFatura());

                    return new FaturaResumoDTO(
                            row.getUniqueId(),
                            row.getDocumentoFatura() != null ? row.getDocumentoFatura() : row.getUniqueId(),
                            row.getEmissaoFatura() != null ? row.getEmissaoFatura().toString() : null,
                            titulo != null && titulo.getVencimento() != null ? titulo.getVencimento().toString() : null,
                            row.getFilial(),
                            row.getPagadorNome(),
                            valorOperacional(row),
                            titulo != null ? ConsultaFiltroUtils.zeroSeNulo(titulo.getValor()) : BigDecimal.ZERO,
                            titulo != null ? ConsultaFiltroUtils.zeroSeNulo(titulo.getValorPago()) : BigDecimal.ZERO,
                            titulo != null ? ConsultaFiltroUtils.zeroSeNulo(titulo.getValorAPagar()) : BigDecimal.ZERO,
                            statusProcesso(row),
                            titulo != null ? titulo.getStatus() : "Sem titulo"
                    );
                })
                .toList();
    }

    private List<VisaoFaturasGraphqlEntity> buscarTitulos(FiltroConsultaDTO filtro) {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        if (deveUsarConsultaLegada(filtro, escopo)) {
            return graphqlRepository.findByEmissaoBetween(filtro.dataInicio(), filtro.dataFim());
        }
        return graphqlRepository.findAll(criarSpecificationTitulos(filtro));
    }

    private List<VisaoFaturasClienteEntity> buscarOperacionais(FiltroConsultaDTO filtro) {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        if (deveUsarConsultaLegada(filtro, escopo)) {
            return clienteRepository.findByDataEmissaoCteBetween(
                    filtro.dataInicio().atStartOfDay().atOffset(ZoneOffset.UTC),
                    filtro.dataFim().atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
            );
        }
        return clienteRepository.findAll(criarSpecificationOperacionais(filtro));
    }

    private boolean isPago(VisaoFaturasGraphqlEntity titulo) {
        return titulo.getPago() != null && titulo.getPago().equalsIgnoreCase("Pago");
    }

    private String agingBucket(VisaoFaturasGraphqlEntity titulo) {
        if (titulo.getVencimento() == null) {
            return "Sem vencimento";
        }

        long dias = ChronoUnit.DAYS.between(titulo.getVencimento(), LocalDate.now());
        if (dias < 0) {
            return "A vencer";
        }
        if (dias <= 15) {
            return "1-15 dias";
        }
        if (dias <= 30) {
            return "16-30 dias";
        }
        if (dias <= 60) {
            return "31-60 dias";
        }
        return "61+ dias";
    }

    private String statusProcesso(VisaoFaturasClienteEntity row) {
        return row.getDocumentoFatura() != null && !row.getDocumentoFatura().isBlank()
                ? "Faturado"
                : "Aguardando Faturamento";
    }

    private BigDecimal valorOperacional(VisaoFaturasClienteEntity row) {
        if (row.getValorFitAnt() != null) {
            return row.getValorFitAnt().setScale(2, RoundingMode.HALF_UP);
        }
        if (row.getValorFatura() != null) {
            return row.getValorFatura().setScale(2, RoundingMode.HALF_UP);
        }
        return ConsultaFiltroUtils.zeroSeNulo(row.getValorFrete()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal somarTitulos(List<VisaoFaturasGraphqlEntity> titulos, java.util.function.Function<VisaoFaturasGraphqlEntity, BigDecimal> extractor) {
        return titulos.stream()
                .map(extractor)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String statusReconciliacao(BigDecimal valorOperacional, BigDecimal valorFinanceiro) {
        if (valorFinanceiro == null) {
            return "sem-titulo";
        }

        if (valorOperacional.subtract(valorFinanceiro).abs().compareTo(new BigDecimal("0.01")) <= 0) {
            return "conciliado";
        }

        return "divergente";
    }

    private String latestUpdate(List<VisaoFaturasGraphqlEntity> titulos, List<VisaoFaturasClienteEntity> operacionais) {
        List<LocalDateTime> updates = new ArrayList<>();
        titulos.stream().map(VisaoFaturasGraphqlEntity::getDataExtracao).forEach(updates::add);
        operacionais.stream().map(VisaoFaturasClienteEntity::getDataExtracao).forEach(updates::add);
        return updates.stream()
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.now())
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private List<VisaoFaturasClienteEntity> buscarOperacionaisPaginados(FiltroConsultaDTO filtro, int limite) {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        if (deveUsarConsultaLegada(filtro, escopo)) {
            return clienteRepository.findByDataEmissaoCteBetween(
                            filtro.dataInicio().atStartOfDay().atOffset(ZoneOffset.UTC),
                            filtro.dataFim().atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
                    ).stream()
                    .sorted(Comparator.comparing(VisaoFaturasClienteEntity::getDataEmissaoCte, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(limite)
                    .toList();
        }
        return clienteRepository.findAll(
                criarSpecificationOperacionais(filtro),
                PageRequest.of(
                        0,
                        limite,
                        Sort.by(
                                Sort.Order.desc("dataEmissaoCte"),
                                Sort.Order.asc("uniqueId")
                        )
                )
        ).getContent();
    }

    @NonNull
    private Specification<VisaoFaturasGraphqlEntity> criarSpecificationTitulos(FiltroConsultaDTO filtro) {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.between("emissao", filtro.dataInicio(), filtro.dataFim()),
                ConsultaSpecificationUtils.escopoFiliais(escopo, "filialNome"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "filiais", "filialNome"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "pago", "pago")
        );
    }

    @NonNull
    private Specification<VisaoFaturasClienteEntity> criarSpecificationOperacionais(FiltroConsultaDTO filtro) {
        OffsetDateTime inicio = filtro.dataInicio().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fim = filtro.dataFim().atTime(23, 59, 59).atOffset(ZoneOffset.UTC);
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.between("dataEmissaoCte", inicio, fim),
                ConsultaSpecificationUtils.escopoFiliais(escopo, "filial"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "filiais", "filial"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "pagadores", "pagadorNome"),
                criarSpecificationStatusProcesso(filtro)
        );
    }

    @NonNull
    private Specification<VisaoFaturasClienteEntity> criarSpecificationStatusProcesso(FiltroConsultaDTO filtro) {
        if (!filtro.temFiltro("statusProcesso")) {
            return ConsultaSpecificationUtils.sempreVerdadeiro();
        }

        List<String> valores = filtro.valores("statusProcesso").stream()
                .map(valor -> valor.trim().toLowerCase())
                .toList();

        return (root, query, cb) -> {
            var documento = root.get("documentoFatura").as(String.class);
            var documentoPreenchido = cb.and(
                    cb.isNotNull(documento),
                    cb.notEqual(cb.trim(documento), "")
            );

            List<jakarta.persistence.criteria.Predicate> permitidos = new ArrayList<>();
            if (valores.contains("faturado")) {
                permitidos.add(documentoPreenchido);
            }
            if (valores.contains("aguardando faturamento")) {
                permitidos.add(cb.not(documentoPreenchido));
            }

            if (permitidos.isEmpty()) {
                return cb.disjunction();
            }
            return cb.or(permitidos.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
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
