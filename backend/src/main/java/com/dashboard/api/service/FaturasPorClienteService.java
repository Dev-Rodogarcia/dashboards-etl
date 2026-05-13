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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FaturasPorClienteService {

    private static final Logger log = LoggerFactory.getLogger(FaturasPorClienteService.class);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<String> ORDEM_AGING = List.of(
            "A vencer",
            "1-15 dias",
            "16-30 dias",
            "31-60 dias",
            "61+ dias",
            "Sem vencimento"
    );

    private record ClienteResumo(String chave, String nome, String cnpj) {
    }

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoFaturasClienteRepository repository;
    private final EscopoFilialService escopoFilialService;
    private final Clock clock;
    private final PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DashboardExportSqlBuilder sqlBuilder;

    @Autowired
    public FaturasPorClienteService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFaturasClienteRepository repository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper,
            NamedParameterJdbcTemplate jdbcTemplate,
            DashboardExportSqlBuilder sqlBuilder
    ) {
        this(
                validadorPeriodo,
                repository,
                escopoFilialService,
                Clock.systemDefaultZone(),
                periodoOffsetDateTimeHelper,
                jdbcTemplate,
                sqlBuilder
        );
    }

    FaturasPorClienteService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFaturasClienteRepository repository,
            Clock clock
    ) {
        this(validadorPeriodo, repository, escopoSemRestricao(), clock, PeriodoOffsetDateTimeHelper.padrao(), null, null);
    }

    FaturasPorClienteService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFaturasClienteRepository repository,
            EscopoFilialService escopoFilialService,
            Clock clock,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper,
            NamedParameterJdbcTemplate jdbcTemplate,
            DashboardExportSqlBuilder sqlBuilder
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.repository = repository;
        this.escopoFilialService = escopoFilialService;
        this.clock = clock;
        this.periodoOffsetDateTimeHelper = periodoOffsetDateTimeHelper;
        this.jdbcTemplate = jdbcTemplate;
        this.sqlBuilder = sqlBuilder;
    }

    public FaturasPorClienteOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<LinhaFaturasCliente> linhas = buscarLinhas(filtro);
        if (linhas.isEmpty()) {
            return new FaturasPorClienteOverviewDTO(
                    ConsultaFiltroUtils.latestUpdate(List.<LinhaFaturasCliente>of(), LinhaFaturasCliente::getDataExtracao),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    0,
                    0,
                    0,
                    0.0,
                    0
            );
        }

        List<LinhaFaturasCliente> faturadas = linhas.stream()
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
                .map(this::clienteResumo)
                .filter(Objects::nonNull)
                .map(ClienteResumo::chave)
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
                ConsultaFiltroUtils.latestUpdate(linhas, LinhaFaturasCliente::getDataExtracao),
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

        Map<YearMonth, List<LinhaFaturasCliente>> agrupado = buscarLinhas(filtro).stream()
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

        Map<String, List<LinhaFaturasCliente>> agrupado = buscarLinhas(filtro).stream()
                .filter(this::deveEntrarNoAging)
                .collect(Collectors.groupingBy(this::agingBucket));

        return ORDEM_AGING.stream()
                .map(bucket -> {
                    List<LinhaFaturasCliente> grupo = agrupado.getOrDefault(bucket, List.of());
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

        Map<String, List<LinhaFaturasCliente>> agrupado = buscarLinhas(filtro).stream()
                .filter(this::temDocumentoFatura)
                .filter(row -> clienteResumo(row) != null)
                .collect(Collectors.groupingBy(row -> Objects.requireNonNull(clienteResumo(row)).chave()));

        return agrupado.values().stream()
                .map(rows -> {
                    ClienteResumo cliente = clienteRepresentante(rows);
                    return new FaturasPorClienteTopClienteDTO(
                            cliente.nome(),
                            cliente.cnpj(),
                            somarValorOperacional(rows)
                    );
                })
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
                        clienteCnpj(row),
                        row.getNumeroCte(),
                        valorOperacional(row),
                        statusProcesso(row)
                ))
                .toList();
    }

    private List<LinhaFaturasCliente> buscarLinhas(FiltroConsultaDTO filtro) {
        if (deveUsarJdbc()) {
            return buscarLinhasJdbc(filtro);
        }

        Map<String, LinhaFaturasCliente> normalizadas = new LinkedHashMap<>();
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        if (deveUsarConsultaLegada(filtro, escopo)) {
            JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
            for (VisaoFaturasClienteEntity entity : repository.findPowerBiRowsByDataEmissaoCteNaJanela(
                    janela.inicioInclusivo(),
                    janela.fimExclusivo()
            )) {
                LinhaFaturasCliente row = LinhaFaturasCliente.fromEntity(entity);
                String chave = chaveNormalizacao(row);
                if (!temTexto(chave)) {
                    continue;
                }
                normalizadas.merge(chave, row, this::escolherRepresentante);
            }
            return normalizadas.values().stream().toList();
        }

        for (VisaoFaturasClienteEntity entity : repository.findAll(
                criarSpecification(filtro),
                Sort.by(
                        Sort.Order.desc("dataExtracao"),
                        Sort.Order.desc("dataEmissaoCte"),
                        Sort.Order.asc("uniqueId")
                )
        )) {
            LinhaFaturasCliente row = LinhaFaturasCliente.fromEntity(entity);
            String chave = chaveNormalizacao(row);
            if (!temTexto(chave)) {
                continue;
            }
            normalizadas.merge(chave, row, this::escolherRepresentante);
        }

        return normalizadas.values().stream().toList();
    }

    private List<LinhaFaturasCliente> buscarLinhasJdbc(FiltroConsultaDTO filtro) {
        DashboardExportSqlBuilder.ExportSql query = sqlBuilder.buildSelect(
                DashboardExportDefinition.FATURAS_POR_CLIENTE,
                filtro,
                escopoFilialService.escopoAtual(),
                Set.of()
        );

        Map<String, LinhaFaturasCliente> normalizadas = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(query.sql(), query.params())) {
            LinhaFaturasCliente linha = LinhaFaturasCliente.fromMap(row);
            String chave = chaveNormalizacao(linha);
            if (!temTexto(chave)) {
                continue;
            }
            normalizadas.merge(chave, linha, this::escolherRepresentante);
        }
        return normalizadas.values().stream().toList();
    }

    private boolean deveUsarJdbc() {
        return jdbcTemplate != null && sqlBuilder != null;
    }

    private LinhaFaturasCliente escolherRepresentante(
            LinhaFaturasCliente atual,
            LinhaFaturasCliente candidato
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

    private String chaveNormalizacao(LinhaFaturasCliente row) {
        if (row == null) {
            return null;
        }

        if (temTexto(row.getUniqueId())) {
            return "linha|" + row.getUniqueId().trim();
        }

        String documento = textoLimpo(row.getDocumentoFatura());
        if (temTexto(documento)) {
            return String.join("|",
                    "fatura",
                    documento.toLowerCase(Locale.ROOT)
            );
        }

        return null;
    }

    private boolean temDocumentoFatura(LinhaFaturasCliente row) {
        return temTexto(row.getDocumentoFatura());
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String textoLimpo(String valor) {
        return temTexto(valor) ? valor.trim() : null;
    }

    private String clienteCnpj(LinhaFaturasCliente row) {
        String cnpj = textoLimpo(row.getClienteCnpj());
        if (temTexto(cnpj)) {
            return cnpj;
        }

        String documentoPagador = normalizarDocumento(row.getPagadorDocumento());
        return documentoPagador != null
                && documentoPagador.length() == 14
                && documentoPagador.chars().allMatch(Character::isDigit)
                ? documentoPagador
                : null;
    }

    private ClienteResumo clienteResumo(LinhaFaturasCliente row) {
        String cnpj = clienteCnpj(row);
        String chaveCnpj = normalizarDocumento(cnpj);
        String nome = textoLimpo(row.getPagadorNome());

        if (temTexto(chaveCnpj)) {
            return new ClienteResumo("cnpj:" + chaveCnpj, temTexto(nome) ? nome : cnpj, cnpj);
        }
        if (temTexto(nome)) {
            return new ClienteResumo("nome:" + nome.toLowerCase(Locale.ROOT), nome, null);
        }
        return null;
    }

    private ClienteResumo clienteRepresentante(List<LinhaFaturasCliente> rows) {
        return rows.stream()
                .map(this::clienteResumo)
                .filter(Objects::nonNull)
                .filter(cliente -> temTexto(cliente.nome()))
                .findFirst()
                .orElseGet(() -> Objects.requireNonNull(clienteResumo(rows.get(0))));
    }

    private String normalizarDocumento(String valor) {
        if (!temTexto(valor)) {
            return null;
        }
        String normalizado = valor.chars()
                .filter(Character::isDigit)
                .mapToObj(Character::toString)
                .collect(Collectors.joining());
        return normalizado.isBlank() ? valor.trim().toLowerCase(Locale.ROOT) : normalizado;
    }

    private String statusProcesso(LinhaFaturasCliente row) {
        return temDocumentoFatura(row) ? "Faturado" : "Aguardando Faturamento";
    }

    private BigDecimal valorOperacional(LinhaFaturasCliente row) {
        if (row.getValorFitAnt() != null) {
            return row.getValorFitAnt().setScale(2, RoundingMode.HALF_UP);
        }
        if (row.getValorFatura() != null) {
            return row.getValorFatura().setScale(2, RoundingMode.HALF_UP);
        }
        return ConsultaFiltroUtils.zeroSeNulo(row.getValorFrete()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal somarValorOperacional(List<LinhaFaturasCliente> rows) {
        return rows.stream()
                .map(this::valorOperacional)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDate dataBasePrazo(LinhaFaturasCliente row) {
        if (row.getEmissaoFatura() != null) {
            return row.getEmissaoFatura();
        }
        return row.getDataEmissaoFatura();
    }

    private LocalDate dataReferenciaMensal(LinhaFaturasCliente row) {
        if (row.getEmissaoFatura() != null) {
            return row.getEmissaoFatura();
        }
        if (row.getDataEmissaoFatura() != null) {
            return row.getDataEmissaoFatura();
        }
        return row.getDataEmissaoCte() != null ? row.getDataEmissaoCte().toLocalDate() : null;
    }

    private boolean isTituloEmAtraso(LinhaFaturasCliente row) {
        return row.getDataVencimentoFatura() != null
                && row.getDataVencimentoFatura().isBefore(hoje())
                && row.getDataBaixaFatura() == null;
    }

    private boolean deveEntrarNoAging(LinhaFaturasCliente row) {
        return temDocumentoFatura(row) && row.getDataBaixaFatura() == null;
    }

    private String agingBucket(LinhaFaturasCliente row) {
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

    private List<LinhaFaturasCliente> buscarLinhasTabela(FiltroConsultaDTO filtro, int limite) {
        if (deveUsarJdbc()) {
            return buscarLinhasJdbc(filtro).stream()
                    .limit(limite)
                    .toList();
        }

        Map<String, LinhaFaturasCliente> normalizadas = new LinkedHashMap<>();
        int tamanhoPagina = Math.min(Math.max(limite * 3, 100), 500);
        int paginaAtual = 0;
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        if (deveUsarConsultaLegada(filtro, escopo)) {
            JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
            return repository.findPowerBiRowsByDataEmissaoCteNaJanela(
                            janela.inicioInclusivo(),
                            janela.fimExclusivo()
                    ).stream()
                    .map(LinhaFaturasCliente::fromEntity)
                    .filter(row -> temTexto(chaveNormalizacao(row)))
                    .sorted(Comparator
                            .comparing(LinhaFaturasCliente::getDataEmissaoCte, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(LinhaFaturasCliente::getUniqueId))
                    .collect(Collectors.toMap(
                            this::chaveNormalizacao,
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

            for (VisaoFaturasClienteEntity entity : pagina.getContent()) {
                LinhaFaturasCliente row = LinhaFaturasCliente.fromEntity(entity);
                String chave = chaveNormalizacao(row);
                if (!temTexto(chave)) {
                    continue;
                }
                normalizadas.merge(chave, row, this::escolherRepresentante);
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
        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.greaterThanOrEqualTo("dataEmissaoCte", janela.inicioInclusivo()),
                ConsultaSpecificationUtils.lessThan("dataEmissaoCte", janela.fimExclusivo()),
                ConsultaSpecificationUtils.escopoFiliais(escopo, "filial"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "filiais", "filial"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "pagadores", "pagadorNome"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "clientesCnpj", "clienteCnpj"),
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

    private static final class LinhaFaturasCliente {

        private final String uniqueId;
        private final String filial;
        private final Long numeroCte;
        private final OffsetDateTime dataEmissaoCte;
        private final BigDecimal valorFrete;
        private final String pagadorNome;
        private final String pagadorDocumento;
        private final String clienteCnpj;
        private final String documentoFatura;
        private final LocalDate emissaoFatura;
        private final BigDecimal valorFitAnt;
        private final BigDecimal valorFatura;
        private final LocalDate dataEmissaoFatura;
        private final LocalDate dataVencimentoFatura;
        private final LocalDate dataBaixaFatura;
        private final LocalDateTime dataExtracao;

        private LinhaFaturasCliente(
                String uniqueId,
                String filial,
                Long numeroCte,
                OffsetDateTime dataEmissaoCte,
                BigDecimal valorFrete,
                String pagadorNome,
                String pagadorDocumento,
                String clienteCnpj,
                String documentoFatura,
                LocalDate emissaoFatura,
                BigDecimal valorFitAnt,
                BigDecimal valorFatura,
                LocalDate dataEmissaoFatura,
                LocalDate dataVencimentoFatura,
                LocalDate dataBaixaFatura,
                LocalDateTime dataExtracao
        ) {
            this.uniqueId = uniqueId;
            this.filial = filial;
            this.numeroCte = numeroCte;
            this.dataEmissaoCte = dataEmissaoCte;
            this.valorFrete = valorFrete;
            this.pagadorNome = pagadorNome;
            this.pagadorDocumento = pagadorDocumento;
            this.clienteCnpj = clienteCnpj;
            this.documentoFatura = documentoFatura;
            this.emissaoFatura = emissaoFatura;
            this.valorFitAnt = valorFitAnt;
            this.valorFatura = valorFatura;
            this.dataEmissaoFatura = dataEmissaoFatura;
            this.dataVencimentoFatura = dataVencimentoFatura;
            this.dataBaixaFatura = dataBaixaFatura;
            this.dataExtracao = dataExtracao;
        }

        static LinhaFaturasCliente fromEntity(VisaoFaturasClienteEntity entity) {
            return new LinhaFaturasCliente(
                    entity.getUniqueId(),
                    entity.getFilial(),
                    entity.getNumeroCte(),
                    entity.getDataEmissaoCte(),
                    entity.getValorFrete(),
                    entity.getPagadorNome(),
                    entity.getPagadorDocumento(),
                    entity.getClienteCnpj(),
                    entity.getDocumentoFatura(),
                    entity.getEmissaoFatura(),
                    entity.getValorFitAnt(),
                    entity.getValorFatura(),
                    entity.getDataEmissaoFatura(),
                    entity.getDataVencimentoFatura(),
                    entity.getDataBaixaFatura(),
                    entity.getDataExtracao()
            );
        }

        static LinhaFaturasCliente fromMap(Map<String, Object> row) {
            String documentoOriginal = texto(row, "Fatura/N° Documento", "Fatura/Nº Documento");
            boolean viewDeslocada = ehStatusProcesso(documentoOriginal);

            return new LinhaFaturasCliente(
                    texto(row, "ID Único", "ID Unico"),
                    texto(row, "Filial"),
                    longo(row, "CT-e/Número", "CT-e/Numero"),
                    offsetDateTime(row, "CT-e/Data de emissão", "CT-e/Data de emissao"),
                    decimal(row, "Frete/Valor dos CT-es"),
                    texto(row, "Pagador do frete/Nome"),
                    texto(row, "Pagador do frete/Documento"),
                    texto(row, "Cliente/CNPJ"),
                    viewDeslocada ? texto(row, "Fatura/Emissão", "Fatura/Emissao") : documentoOriginal,
                    localDate(viewDeslocada
                            ? valor(row, "Fatura/Valor", "CT-e/Data de emissão", "CT-e/Data de emissao")
                            : valor(row, "Fatura/Emissão", "Fatura/Emissao")),
                    decimal(viewDeslocada ? valor(row, "Fatura/Valor Total") : valor(row, "Fatura/Valor")),
                    decimal(viewDeslocada
                            ? valor(row, "Fatura/Número", "Fatura/Numero")
                            : valor(row, "Fatura/Valor Total")),
                    localDate(viewDeslocada
                            ? valor(row, "Parcelas/Vencimento")
                            : valor(row, "Fatura/Emissão Fatura", "Fatura/Emissao Fatura")),
                    localDate(viewDeslocada ? valor(row, "Fatura/Baixa") : valor(row, "Parcelas/Vencimento")),
                    localDate(viewDeslocada ? valor(row, "Fatura/Data Vencimento Original") : valor(row, "Fatura/Baixa")),
                    localDateTime(row, "Data da Última Atualização", "Data da Ultima Atualizacao")
            );
        }

        String getUniqueId() {
            return uniqueId;
        }

        String getFilial() {
            return filial;
        }

        Long getNumeroCte() {
            return numeroCte;
        }

        OffsetDateTime getDataEmissaoCte() {
            return dataEmissaoCte;
        }

        BigDecimal getValorFrete() {
            return valorFrete;
        }

        String getPagadorNome() {
            return pagadorNome;
        }

        String getPagadorDocumento() {
            return pagadorDocumento;
        }

        String getClienteCnpj() {
            return clienteCnpj;
        }

        String getDocumentoFatura() {
            return documentoFatura;
        }

        LocalDate getEmissaoFatura() {
            return emissaoFatura;
        }

        BigDecimal getValorFitAnt() {
            return valorFitAnt;
        }

        BigDecimal getValorFatura() {
            return valorFatura;
        }

        LocalDate getDataEmissaoFatura() {
            return dataEmissaoFatura;
        }

        LocalDate getDataVencimentoFatura() {
            return dataVencimentoFatura;
        }

        LocalDate getDataBaixaFatura() {
            return dataBaixaFatura;
        }

        LocalDateTime getDataExtracao() {
            return dataExtracao;
        }

        private static Object valor(Map<String, Object> row, String... colunas) {
            for (String coluna : colunas) {
                Object valor = row.get(coluna);
                if (valor != null) {
                    return valor;
                }
            }
            return null;
        }

        private static String texto(Map<String, Object> row, String... colunas) {
            Object valor = valor(row, colunas);
            return valor != null ? valor.toString() : null;
        }

        private static Long longo(Map<String, Object> row, String... colunas) {
            Object valor = valor(row, colunas);
            if (valor instanceof Number numero) {
                return numero.longValue();
            }
            if (valor == null || valor.toString().isBlank()) {
                return null;
            }
            try {
                return new BigDecimal(valor.toString().trim().replace(",", ".")).longValue();
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        private static BigDecimal decimal(Map<String, Object> row, String... colunas) {
            return decimal(valor(row, colunas));
        }

        private static BigDecimal decimal(Object valor) {
            if (valor instanceof BigDecimal decimal) {
                return decimal.setScale(2, RoundingMode.HALF_UP);
            }
            if (valor instanceof Number numero) {
                return BigDecimal.valueOf(numero.doubleValue()).setScale(2, RoundingMode.HALF_UP);
            }
            return ConsultaFiltroUtils.parseBigDecimal(valor != null ? valor.toString() : null)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        private static LocalDate localDate(Object valor) {
            if (valor instanceof LocalDate data) {
                return data;
            }
            if (valor instanceof LocalDateTime dataHora) {
                return dataHora.toLocalDate();
            }
            if (valor instanceof OffsetDateTime dataHora) {
                return dataHora.toLocalDate();
            }
            if (valor == null || valor.toString().isBlank()) {
                return null;
            }

            String texto = valor.toString().trim();
            if (texto.length() >= 10 && texto.charAt(4) == '-' && texto.charAt(7) == '-') {
                return tryParseDate(texto.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
            }
            if (texto.length() >= 10 && texto.charAt(2) == '/' && texto.charAt(5) == '/') {
                return tryParseDate(texto.substring(0, 10), DATA_BR);
            }
            if (texto.length() == 8 && texto.chars().allMatch(Character::isDigit)) {
                return tryParseDate(texto, DateTimeFormatter.BASIC_ISO_DATE);
            }
            return tryParseDate(texto, DateTimeFormatter.ISO_LOCAL_DATE);
        }

        private static LocalDate tryParseDate(String valor, DateTimeFormatter formatter) {
            try {
                return LocalDate.parse(valor, formatter);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }

        private static LocalDateTime localDateTime(Map<String, Object> row, String... colunas) {
            Object valor = valor(row, colunas);
            if (valor instanceof LocalDateTime dataHora) {
                return dataHora;
            }
            if (valor instanceof OffsetDateTime dataHora) {
                return dataHora.toLocalDateTime();
            }
            if (valor instanceof LocalDate data) {
                return data.atStartOfDay();
            }
            if (valor == null || valor.toString().isBlank()) {
                return null;
            }

            String texto = valor.toString().trim();
            if (texto.length() >= 19 && texto.charAt(4) == '-' && texto.charAt(7) == '-') {
                String normalizado = texto.substring(0, 19).replace('T', ' ');
                return tryParseDateTime(normalizado, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            return tryParseDateTime(texto, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        private static LocalDateTime tryParseDateTime(String valor, DateTimeFormatter formatter) {
            try {
                return LocalDateTime.parse(valor, formatter);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }

        private static OffsetDateTime offsetDateTime(Map<String, Object> row, String... colunas) {
            Object valor = valor(row, colunas);
            if (valor instanceof OffsetDateTime dataHora) {
                return dataHora;
            }
            if (valor == null || valor.toString().isBlank()) {
                return null;
            }
            try {
                return OffsetDateTime.parse(valor.toString().trim());
            } catch (DateTimeParseException ex) {
                return null;
            }
        }

        private static boolean ehStatusProcesso(String valor) {
            return valor != null
                    && (valor.equalsIgnoreCase("Faturado")
                    || valor.equalsIgnoreCase("Aguardando Faturamento"));
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
