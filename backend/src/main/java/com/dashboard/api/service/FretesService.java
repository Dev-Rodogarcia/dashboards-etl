package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.fretes.FreteResumoDTO;
import com.dashboard.api.dto.fretes.FretesChartsDTO;
import com.dashboard.api.dto.fretes.FretesClienteRankingDTO;
import com.dashboard.api.dto.fretes.FretesDocumentMixDTO;
import com.dashboard.api.dto.fretes.FretesFaturamentoGrupoDTO;
import com.dashboard.api.dto.fretes.FretesGoalSummaryDTO;
import com.dashboard.api.dto.fretes.FretesOrigemDestinoDTO;
import com.dashboard.api.dto.fretes.FretesOverviewDTO;
import com.dashboard.api.dto.fretes.FretesPrevisaoPorStatusDTO;
import com.dashboard.api.dto.fretes.FretesTrendPointDTO;
import com.dashboard.api.model.VisaoFretesEntity;
import com.dashboard.api.repository.VisaoFretesRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FretesService {

    private static final Logger log = LoggerFactory.getLogger(FretesService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoFretesRepository repository;
    private final EscopoFilialService escopoFilialService;
    private final PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper;
    private final FretesGoalService fretesGoalService;

    FretesService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFretesRepository repository
    ) {
        this(validadorPeriodo, repository, escopoSemRestricao(), PeriodoOffsetDateTimeHelper.padrao(), null);
    }

    @Autowired
    public FretesService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFretesRepository repository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper,
            FretesGoalService fretesGoalService
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.repository = repository;
        this.escopoFilialService = escopoFilialService;
        this.periodoOffsetDateTimeHelper = periodoOffsetDateTimeHelper;
        this.fretesGoalService = fretesGoalService;
    }

    FretesService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFretesRepository repository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper
    ) {
        this(validadorPeriodo, repository, escopoFilialService, periodoOffsetDateTimeHelper, null);
    }

    public FretesOverviewDTO buscarOverview(LocalDate dataInicio, LocalDate dataFim) {
        return buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public FretesOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoFretesEntity> fretes = buscarRegistros(filtro);
        int totalFretes = fretes.size();

        if (totalFretes == 0) {
            FretesGoalSummaryDTO metas = buscarResumoMetas(filtro, List.of());
            return new FretesOverviewDTO(
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, 0, 0.0, 0.0, 0,
                    metas.metaFaturamento(),
                    metas.percentualAtingimentoFaturamento(),
                    metas.metaFretes(),
                    metas.percentualAtingimentoFretes()
            );
        }

        BigDecimal receitaBruta = fretes.stream()
                .map(VisaoFretesEntity::getValorTotal)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorFrete = fretes.stream()
                .map(VisaoFretesEntity::getSubtotal)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ticketMedio = receitaBruta.divide(BigDecimal.valueOf(totalFretes), 2, RoundingMode.HALF_UP);

        BigDecimal pesoTaxadoTotal = fretes.stream()
                .map(VisaoFretesEntity::getPesoTaxado)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int volumesTotais = fretes.stream()
                .mapToInt(f -> ConsultaFiltroUtils.zeroSeNulo(f.getVolumes()))
                .sum();

        double pctCteEmitido = percentual(fretes.stream().filter(f -> f.getCteId() != null).count(), totalFretes);
        double pctNfseEmitida = percentual(fretes.stream().filter(f -> f.getNfseNumero() != null).count(), totalFretes);

        int fretesPrevisaoVencida = (int) fretes.stream()
                .filter(f -> f.getPrevisaoEntrega() != null
                        && f.getPrevisaoEntrega().isBefore(LocalDate.now())
                        && !"finalizado".equalsIgnoreCase(f.getStatus()))
                .count();

        log.info("Overview fretes calculado: totalFretes={}, periodo={} a {}", totalFretes, filtro.dataInicio(), filtro.dataFim());

        FretesGoalSummaryDTO metas = buscarResumoMetas(filtro, fretes);

        return new FretesOverviewDTO(
                ConsultaFiltroUtils.latestUpdate(fretes, VisaoFretesEntity::getDataExtracao),
                totalFretes,
                receitaBruta.setScale(2, RoundingMode.HALF_UP),
                valorFrete.setScale(2, RoundingMode.HALF_UP),
                ticketMedio,
                pesoTaxadoTotal.setScale(2, RoundingMode.HALF_UP),
                volumesTotais,
                pctCteEmitido,
                pctNfseEmitida,
                fretesPrevisaoVencida,
                metas.metaFaturamento(),
                metas.percentualAtingimentoFaturamento(),
                metas.metaFretes(),
                metas.percentualAtingimentoFretes()
        );
    }

    public FretesGoalSummaryDTO buscarResumoMetas(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return buscarResumoMetas(filtro, buscarRegistros(filtro));
    }

    public List<FretesTrendPointDTO> buscarSerieTemporal(LocalDate dataInicio, LocalDate dataFim) {
        return buscarSerieTemporal(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public List<FretesTrendPointDTO> buscarSerieTemporal(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        Map<LocalDate, List<VisaoFretesEntity>> agrupado = buscarRegistros(filtro).stream()
                .filter(f -> f.getDataFrete() != null)
                .collect(Collectors.groupingBy(f -> f.getDataFrete().toLocalDate()));

        return agrupado.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<VisaoFretesEntity> grupo = entry.getValue();

                    BigDecimal receitaBruta = grupo.stream()
                            .map(VisaoFretesEntity::getValorTotal)
                            .map(ConsultaFiltroUtils::zeroSeNulo)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);

                    BigDecimal valorFrete = grupo.stream()
                            .map(VisaoFretesEntity::getSubtotal)
                            .map(ConsultaFiltroUtils::zeroSeNulo)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);

                    return new FretesTrendPointDTO(
                            entry.getKey().format(DATE_FMT),
                            receitaBruta,
                            valorFrete,
                            grupo.size()
                    );
                })
                .toList();
    }

    public List<FretesClienteRankingDTO> buscarTopClientes(LocalDate dataInicio, LocalDate dataFim, int limite) {
        return buscarTopClientes(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()), limite);
    }

    public List<FretesClienteRankingDTO> buscarTopClientes(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 10, 50);

        return buscarRegistros(filtro).stream()
                .filter(f -> f.getPagadorNome() != null && !f.getPagadorNome().isBlank())
                .collect(Collectors.groupingBy(VisaoFretesEntity::getPagadorNome))
                .entrySet().stream()
                .map(entry -> {
                    BigDecimal receita = entry.getValue().stream()
                            .map(VisaoFretesEntity::getValorTotal)
                            .map(ConsultaFiltroUtils::zeroSeNulo)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);

                    int totalFretes = entry.getValue().size();
                    BigDecimal ticketMedio = totalFretes > 0
                            ? receita.divide(BigDecimal.valueOf(totalFretes), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return new FretesClienteRankingDTO(entry.getKey(), receita, totalFretes, ticketMedio);
                })
                .sorted(Comparator.comparing(FretesClienteRankingDTO::receita).reversed())
                .limit(limiteAplicado)
                .toList();
    }

    public List<FretesDocumentMixDTO> buscarMixDocumental(LocalDate dataInicio, LocalDate dataFim) {
        return buscarMixDocumental(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public List<FretesDocumentMixDTO> buscarMixDocumental(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoFretesEntity> fretes = buscarRegistros(filtro);

        int cteCount = 0;
        int nfseCount = 0;
        int pendenteCount = 0;

        for (VisaoFretesEntity f : fretes) {
            if (f.getCteId() != null) {
                cteCount++;
            } else if (f.getNfseNumero() != null) {
                nfseCount++;
            } else {
                pendenteCount++;
            }
        }

        return List.of(
                new FretesDocumentMixDTO("CT-e", cteCount),
                new FretesDocumentMixDTO("NFS-e", nfseCount),
                new FretesDocumentMixDTO("Pendente", pendenteCount)
        );
    }

    public List<FreteResumoDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 200);

        return repository.findAll(
                        criarSpecification(filtro),
                        PageRequest.of(0, limiteAplicado, Sort.by(Sort.Direction.DESC, "dataFrete"))
                ).getContent().stream()
                .map(f -> new FreteResumoDTO(
                        f.getId(),
                        f.getNumeroMinuta(),
                        f.getDataFrete() != null ? f.getDataFrete().toString() : null,
                        f.getStatus(),
                        f.getFilialNome(),
                        f.getPagadorNome(),
                        f.getRemetenteNome(),
                        f.getDestinatarioNome(),
                        f.getOrigemUf(),
                        f.getDestinoUf(),
                        ConsultaFiltroUtils.zeroSeNulo(f.getValorTotal()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(f.getSubtotal()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(f.getPesoTaxado()).setScale(2, RoundingMode.HALF_UP),
                        f.getVolumes(),
                        f.getPrevisaoEntrega() != null ? f.getPrevisaoEntrega().toString() : null,
                        f.getCteId() != null ? "CT-e" : f.getNfseNumero() != null ? "NFS-e" : "Pendente",
                        f.getNumeroCte(),
                        f.getNfseNumero(),
                        ConsultaFiltroUtils.zeroSeNulo(f.getValorIcms()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(f.getValorPis()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(f.getValorCofins()).setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    public FretesChartsDTO buscarGraficos(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoFretesEntity> fretes = buscarRegistros(filtro);

        List<FretesPrevisaoPorStatusDTO> previsaoPorStatus = fretes.stream()
                .collect(Collectors.groupingBy(f -> textoOuPadrao(f.getStatus(), "Sem status")))
                .entrySet().stream()
                .map(entry -> new FretesPrevisaoPorStatusDTO(
                        entry.getKey(),
                        (int) entry.getValue().stream().filter(this::isVencido).count(),
                        (int) entry.getValue().stream().filter(f -> !isVencido(f)).count()
                ))
                .sorted(Comparator.comparing(FretesPrevisaoPorStatusDTO::status))
                .toList();

        List<FretesOrigemDestinoDTO> topRotasPorReceita = fretes.stream()
                .collect(Collectors.groupingBy(f -> rotaKey(f.getOrigemUf(), f.getDestinoUf())))
                .entrySet().stream()
                .map(entry -> {
                    String[] rota = splitRota(entry.getKey());
                    return new FretesOrigemDestinoDTO(
                            rota[0],
                            rota[1],
                            entry.getValue().stream()
                                    .map(VisaoFretesEntity::getValorTotal)
                                    .map(ConsultaFiltroUtils::zeroSeNulo)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .setScale(2, RoundingMode.HALF_UP),
                            entry.getValue().size()
                    );
                })
                .sorted(Comparator.comparing(FretesOrigemDestinoDTO::receita).reversed()
                        .thenComparing(FretesOrigemDestinoDTO::origemUf)
                        .thenComparing(FretesOrigemDestinoDTO::destinoUf))
                .limit(10)
                .toList();

        List<FretesFaturamentoGrupoDTO> faturamentoPorClassificacao = agruparFaturamento(
                fretes,
                this::classificacaoFaturamento,
                "Sem classificação",
                8
        );

        List<FretesFaturamentoGrupoDTO> faturamentoPorResponsavelDestino = agruparFaturamento(
                fretes,
                this::responsavelDestino,
                "Responsável não informado",
                10
        );

        List<FretesFaturamentoGrupoDTO> faturamentoPorUfOrigem = agruparFaturamento(
                fretes,
                VisaoFretesEntity::getOrigemUf,
                "UF não informada",
                10
        );

        List<FretesFaturamentoGrupoDTO> faturamentoPorUfDestino = agruparFaturamento(
                fretes,
                VisaoFretesEntity::getDestinoUf,
                "UF não informada",
                10
        );

        List<FretesFaturamentoGrupoDTO> faturamentoPorCidadeDestino = agruparFaturamento(
                fretes,
                VisaoFretesEntity::getDestinoCidade,
                "Cidade não informada",
                10
        );

        return new FretesChartsDTO(
                previsaoPorStatus,
                topRotasPorReceita,
                faturamentoPorClassificacao,
                faturamentoPorResponsavelDestino,
                faturamentoPorUfOrigem,
                faturamentoPorUfDestino,
                faturamentoPorCidadeDestino
        );
    }

    private List<VisaoFretesEntity> buscarRegistros(FiltroConsultaDTO filtro) {
        return repository.findAll(criarSpecification(filtro));
    }

    private FretesGoalSummaryDTO buscarResumoMetas(FiltroConsultaDTO filtro, List<VisaoFretesEntity> fretes) {
        if (fretesGoalService == null) {
            return fallbackResumoMetas(filtro, fretes);
        }
        try {
            return fretesGoalService.buscarResumo(
                    filtro.dataInicio(),
                    filtro.dataFim(),
                    realizadosPorFilial(fretes),
                    filtro.valores("filiais")
            );
        } catch (RuntimeException ex) {
            log.warn("Não foi possível carregar metas de fretes. O relatório seguirá sem metas configuradas. Motivo: {}", ex.getMessage());
            return fallbackResumoMetas(filtro, fretes);
        }
    }

    private FretesGoalSummaryDTO fallbackResumoMetas(FiltroConsultaDTO filtro, List<VisaoFretesEntity> fretes) {
        BigDecimal realizadoFaturamento = fretes.stream()
                .map(VisaoFretesEntity::getValorTotal)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new FretesGoalSummaryDTO(
                filtro.dataInicio().format(DATE_FMT),
                filtro.dataFim().format(DATE_FMT),
                BigDecimal.ZERO,
                realizadoFaturamento,
                0.0,
                0,
                fretes.size(),
                0.0,
                List.of()
        );
    }

    private List<FretesGoalService.FretesBranchRealizado> realizadosPorFilial(List<VisaoFretesEntity> fretes) {
        return fretes.stream()
                .collect(Collectors.groupingBy(
                        frete -> textoOuPadrao(frete.getFilialNome(), "Filial não informada"),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> new FretesGoalService.FretesBranchRealizado(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(VisaoFretesEntity::getValorTotal)
                                .map(ConsultaFiltroUtils::zeroSeNulo)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP),
                        entry.getValue().size()
                ))
                .toList();
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

    private boolean isVencido(VisaoFretesEntity frete) {
        return frete.getPrevisaoEntrega() != null
                && frete.getStatus() != null
                && frete.getPrevisaoEntrega().isBefore(LocalDate.now())
                && !"Finalizado".equalsIgnoreCase(frete.getStatus());
    }

    private String rotaKey(String origemUf, String destinoUf) {
        return textoOuPadrao(origemUf, "N/A") + "|" + textoOuPadrao(destinoUf, "N/A");
    }

    private String[] splitRota(String rota) {
        return rota.split("\\|", 2);
    }

    private String textoOuPadrao(String valor, String padrao) {
        return Objects.requireNonNullElse(valor, "").isBlank() ? padrao : valor;
    }

    private List<FretesFaturamentoGrupoDTO> agruparFaturamento(
            List<VisaoFretesEntity> fretes,
            Function<VisaoFretesEntity, String> groupExtractor,
            String fallback,
            int limite
    ) {
        return fretes.stream()
                .collect(Collectors.groupingBy(f -> textoOuPadrao(groupExtractor.apply(f), fallback)))
                .entrySet().stream()
                .map(entry -> new FretesFaturamentoGrupoDTO(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(VisaoFretesEntity::getValorTotal)
                                .map(ConsultaFiltroUtils::zeroSeNulo)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP),
                        entry.getValue().size()
                ))
                .sorted(Comparator.comparing(FretesFaturamentoGrupoDTO::receita).reversed()
                        .thenComparing(FretesFaturamentoGrupoDTO::nome))
                .limit(limite)
                .toList();
    }

    private String classificacaoFaturamento(VisaoFretesEntity frete) {
        String classificacao = textoOuPadrao(frete.getClassificacaoNome(), "Sem classificação");
        String normalizada = classificacao.toUpperCase(Locale.ROOT);

        if (normalizada.contains("FTL")) {
            return "FTL";
        }
        if (normalizada.contains("LTL")) {
            return "LTL";
        }
        if (normalizada.contains("PTL")) {
            return "PTL";
        }

        return classificacao;
    }

    private String responsavelDestino(VisaoFretesEntity frete) {
        String responsavel = textoOuPadrao(frete.getResponsavelRegiaoDestino(), "");
        if (!responsavel.isBlank()) {
            return responsavel;
        }

        String filialEmissora = textoOuPadrao(frete.getFilialEmissora(), "");
        if (!filialEmissora.isBlank()) {
            return filialEmissora;
        }

        return textoOuPadrao(frete.getFilialNome(), "Responsável não informado");
    }

    @NonNull
    private Specification<VisaoFretesEntity> criarSpecification(FiltroConsultaDTO filtro) {
        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.greaterThanOrEqualTo("dataFrete", janela.inicioInclusivo()),
                ConsultaSpecificationUtils.lessThan("dataFrete", janela.fimExclusivo()),
                ConsultaSpecificationUtils.escopoFiliais(escopo, "filialNome"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "filiais", "filialNome"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "status", "status"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "pagadores", "pagadorNome"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "ufOrigem", "origemUf"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "ufDestino", "destinoUf"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "tiposFrete", "tipoFrete"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "modais", "modal")
        );
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
