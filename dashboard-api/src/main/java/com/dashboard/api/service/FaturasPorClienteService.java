package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.faturascliente.FaturaPorClienteResumoDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteAgingBucketDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteMensalDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteOverviewDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteStatusProcessoDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteTopClienteDTO;
import com.dashboard.api.model.VisaoFaturasClienteEntity;
import com.dashboard.api.repository.VisaoFaturasClienteRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FaturasPorClienteService {

    private static final Logger log = LoggerFactory.getLogger(FaturasPorClienteService.class);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final List<String> ORDEM_AGING = List.of(
            "A vencer",
            "1-15 dias",
            "16-30 dias",
            "31-60 dias",
            "61+ dias",
            "Sem vencimento"
    );

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoFaturasClienteRepository repository;
    private final EscopoFilialService escopoFilialService;
    private final Clock clock;

    @Autowired
    public FaturasPorClienteService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFaturasClienteRepository repository,
            EscopoFilialService escopoFilialService
    ) {
        this(validadorPeriodo, repository, escopoFilialService, Clock.systemDefaultZone());
    }

    FaturasPorClienteService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFaturasClienteRepository repository,
            Clock clock
    ) {
        this(validadorPeriodo, repository, escopoSemRestricao(), clock);
    }

    FaturasPorClienteService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFaturasClienteRepository repository,
            EscopoFilialService escopoFilialService,
            Clock clock
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.repository = repository;
        this.escopoFilialService = escopoFilialService;
        this.clock = clock;
    }

    public FaturasPorClienteOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoFaturasClienteEntity> linhas = buscarLinhas(filtro);
        if (linhas.isEmpty()) {
            return new FaturasPorClienteOverviewDTO(
                    ConsultaFiltroUtils.latestUpdate(List.<VisaoFaturasClienteEntity>of(), VisaoFaturasClienteEntity::getDataExtracao),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    0,
                    0,
                    0,
                    0.0,
                    0
            );
        }

        List<VisaoFaturasClienteEntity> faturadas = linhas.stream()
                .filter(this::temDocumentoFatura)
                .toList();

        BigDecimal valorFaturado = faturadas.stream()
                .map(this::valorOperacional)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        int registrosFaturados = faturadas.size();
        int aguardandoFaturamento = (int) linhas.stream().filter(row -> !temDocumentoFatura(row)).count();
        int titulosEmAtraso = (int) faturadas.stream().filter(this::isTituloEmAtraso).count();
        int clientesAtivos = (int) linhas.stream()
                .map(VisaoFaturasClienteEntity::getPagadorNome)
                .filter(this::temTexto)
                .distinct()
                .count();

        double prazoMedioDias = faturadas.stream()
                .filter(row -> dataBasePrazo(row) != null && row.getDataVencimentoFatura() != null)
                .mapToLong(row -> ChronoUnit.DAYS.between(dataBasePrazo(row), row.getDataVencimentoFatura()))
                .average()
                .orElse(0.0);
        prazoMedioDias = BigDecimal.valueOf(prazoMedioDias)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        log.info("Faturas por cliente overview calculado: linhas={}, faturadas={}, periodo={} a {}",
                linhas.size(), registrosFaturados, filtro.dataInicio(), filtro.dataFim());

        return new FaturasPorClienteOverviewDTO(
                ConsultaFiltroUtils.latestUpdate(linhas, VisaoFaturasClienteEntity::getDataExtracao),
                valorFaturado,
                registrosFaturados,
                aguardandoFaturamento,
                titulosEmAtraso,
                prazoMedioDias,
                clientesAtivos
        );
    }

    public List<FaturasPorClienteMensalDTO> buscarMensal(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        Map<YearMonth, List<VisaoFaturasClienteEntity>> agrupado = buscarLinhas(filtro).stream()
                .filter(this::temDocumentoFatura)
                .filter(row -> dataReferenciaMensal(row) != null)
                .collect(Collectors.groupingBy(row -> YearMonth.from(dataReferenciaMensal(row))));

        return agrupado.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new FaturasPorClienteMensalDTO(
                        entry.getKey().format(MONTH_FMT),
                        somarValorOperacional(entry.getValue()),
                        entry.getValue().size()
                ))
                .toList();
    }

    public List<FaturasPorClienteAgingBucketDTO> buscarAging(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        Map<String, List<VisaoFaturasClienteEntity>> agrupado = buscarLinhas(filtro).stream()
                .filter(this::deveEntrarNoAging)
                .collect(Collectors.groupingBy(this::agingBucket));

        return ORDEM_AGING.stream()
                .map(bucket -> {
                    List<VisaoFaturasClienteEntity> grupo = agrupado.getOrDefault(bucket, List.of());
                    return new FaturasPorClienteAgingBucketDTO(
                            bucket,
                            somarValorOperacional(grupo),
                            grupo.size()
                    );
                })
                .toList();
    }

    public List<FaturasPorClienteTopClienteDTO> buscarTopClientes(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 10, 50);

        return buscarLinhas(filtro).stream()
                .filter(this::temDocumentoFatura)
                .filter(row -> temTexto(row.getPagadorNome()))
                .collect(Collectors.groupingBy(VisaoFaturasClienteEntity::getPagadorNome))
                .entrySet().stream()
                .map(entry -> new FaturasPorClienteTopClienteDTO(
                        entry.getKey(),
                        somarValorOperacional(entry.getValue())
                ))
                .sorted(Comparator.comparing(FaturasPorClienteTopClienteDTO::valorFaturado).reversed())
                .limit(limiteAplicado)
                .toList();
    }

    public List<FaturasPorClienteStatusProcessoDTO> buscarStatusProcesso(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return buscarLinhas(filtro).stream()
                .collect(Collectors.groupingBy(this::statusProcesso, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new FaturasPorClienteStatusProcessoDTO(entry.getKey(), entry.getValue().intValue()))
                .toList();
    }

    public List<FaturaPorClienteResumoDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 200);

        return buscarLinhasTabela(filtro, limiteAplicado).stream()
                .map(row -> new FaturaPorClienteResumoDTO(
                        row.getUniqueId(),
                        row.getDocumentoFatura(),
                        formatarData(dataBasePrazo(row)),
                        formatarData(row.getDataVencimentoFatura()),
                        formatarData(row.getDataBaixaFatura()),
                        row.getFilial(),
                        row.getPagadorNome(),
                        row.getNumeroCte(),
                        valorOperacional(row),
                        statusProcesso(row)
                ))
                .toList();
    }

    private List<VisaoFaturasClienteEntity> buscarLinhas(FiltroConsultaDTO filtro) {
        Map<String, VisaoFaturasClienteEntity> normalizadas = new LinkedHashMap<>();
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        if (deveUsarConsultaLegada(filtro, escopo)) {
            for (VisaoFaturasClienteEntity row : repository.findPowerBiRowsByDataEmissaoCteBetween(
                    filtro.dataInicio().atStartOfDay().atOffset(ZoneOffset.UTC),
                    filtro.dataFim().atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
            )) {
                if (!temTexto(row.getUniqueId())) {
                    continue;
                }
                normalizadas.merge(row.getUniqueId(), row, this::escolherRepresentante);
            }
            return normalizadas.values().stream().toList();
        }

        for (VisaoFaturasClienteEntity row : repository.findAll(
                criarSpecification(filtro),
                Sort.by(
                        Sort.Order.desc("dataExtracao"),
                        Sort.Order.desc("dataEmissaoCte"),
                        Sort.Order.asc("uniqueId")
                )
        )) {
            if (!temTexto(row.getUniqueId())) {
                continue;
            }
            normalizadas.merge(row.getUniqueId(), row, this::escolherRepresentante);
        }

        return normalizadas.values().stream().toList();
    }

    private VisaoFaturasClienteEntity escolherRepresentante(
            VisaoFaturasClienteEntity atual,
            VisaoFaturasClienteEntity candidato
    ) {
        LocalDate atualizacaoAtual = atual.getDataExtracao() != null ? atual.getDataExtracao().toLocalDate() : null;
        LocalDate atualizacaoCandidato = candidato.getDataExtracao() != null ? candidato.getDataExtracao().toLocalDate() : null;

        if (atualizacaoAtual == null && atualizacaoCandidato != null) {
            return candidato;
        }
        if (atualizacaoAtual != null && atualizacaoCandidato == null) {
            return atual;
        }
        if (atual.getDataExtracao() == null && candidato.getDataExtracao() == null) {
            return atual;
        }
        return atual.getDataExtracao().isBefore(candidato.getDataExtracao()) ? candidato : atual;
    }

    private boolean temDocumentoFatura(VisaoFaturasClienteEntity row) {
        return temTexto(row.getDocumentoFatura());
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String statusProcesso(VisaoFaturasClienteEntity row) {
        return temDocumentoFatura(row) ? "Faturado" : "Aguardando Faturamento";
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

    private BigDecimal somarValorOperacional(List<VisaoFaturasClienteEntity> rows) {
        return rows.stream()
                .map(this::valorOperacional)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDate dataBasePrazo(VisaoFaturasClienteEntity row) {
        if (row.getEmissaoFatura() != null) {
            return row.getEmissaoFatura();
        }
        return row.getDataEmissaoFatura();
    }

    private LocalDate dataReferenciaMensal(VisaoFaturasClienteEntity row) {
        if (row.getEmissaoFatura() != null) {
            return row.getEmissaoFatura();
        }
        if (row.getDataEmissaoFatura() != null) {
            return row.getDataEmissaoFatura();
        }
        return row.getDataEmissaoCte() != null ? row.getDataEmissaoCte().toLocalDate() : null;
    }

    private boolean isTituloEmAtraso(VisaoFaturasClienteEntity row) {
        return row.getDataVencimentoFatura() != null
                && row.getDataVencimentoFatura().isBefore(hoje())
                && row.getDataBaixaFatura() == null;
    }

    private boolean deveEntrarNoAging(VisaoFaturasClienteEntity row) {
        return temDocumentoFatura(row) && row.getDataBaixaFatura() == null;
    }

    private String agingBucket(VisaoFaturasClienteEntity row) {
        if (row.getDataVencimentoFatura() == null) {
            return "Sem vencimento";
        }

        long dias = ChronoUnit.DAYS.between(row.getDataVencimentoFatura(), hoje());
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

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }

    private String formatarData(LocalDate data) {
        return data != null ? ConsultaFiltroUtils.data(data) : null;
    }

    private List<VisaoFaturasClienteEntity> buscarLinhasTabela(FiltroConsultaDTO filtro, int limite) {
        Map<String, VisaoFaturasClienteEntity> normalizadas = new LinkedHashMap<>();
        int tamanhoPagina = Math.min(Math.max(limite * 3, 100), 500);
        int paginaAtual = 0;
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        if (deveUsarConsultaLegada(filtro, escopo)) {
            return repository.findPowerBiRowsByDataEmissaoCteBetween(
                            filtro.dataInicio().atStartOfDay().atOffset(ZoneOffset.UTC),
                            filtro.dataFim().atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
                    ).stream()
                    .filter(row -> temTexto(row.getUniqueId()))
                    .sorted(Comparator
                            .comparing(VisaoFaturasClienteEntity::getDataEmissaoCte, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(VisaoFaturasClienteEntity::getUniqueId))
                    .collect(Collectors.toMap(
                            VisaoFaturasClienteEntity::getUniqueId,
                            row -> row,
                            (atual, candidato) -> atual,
                            LinkedHashMap::new
                    ))
                    .values().stream()
                    .limit(limite)
                    .toList();
        }

        while (normalizadas.size() < limite) {
            Page<VisaoFaturasClienteEntity> pagina = repository.findAll(
                    criarSpecification(filtro),
                    PageRequest.of(
                            paginaAtual,
                            tamanhoPagina,
                            Sort.by(
                                    Sort.Order.desc("dataEmissaoCte"),
                                    Sort.Order.desc("dataExtracao"),
                                    Sort.Order.asc("uniqueId")
                            )
                    )
            );

            for (VisaoFaturasClienteEntity row : pagina.getContent()) {
                if (!temTexto(row.getUniqueId())) {
                    continue;
                }
                normalizadas.putIfAbsent(row.getUniqueId(), row);
            }

            if (pagina.isLast() || pagina.getContent().isEmpty()) {
                break;
            }
            paginaAtual++;
        }

        return normalizadas.values().stream()
                .limit(limite)
                .toList();
    }

    @NonNull
    private Specification<VisaoFaturasClienteEntity> criarSpecification(FiltroConsultaDTO filtro) {
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
