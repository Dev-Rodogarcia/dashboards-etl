package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.coletas.ColetaResumoDTO;
import com.dashboard.api.dto.coletas.ColetasAgingBucketDTO;
import com.dashboard.api.dto.coletas.ColetasChartsDTO;
import com.dashboard.api.dto.coletas.ColetasOverviewDTO;
import com.dashboard.api.dto.coletas.ColetasRegiaoVolumeDTO;
import com.dashboard.api.dto.coletas.ColetasSlaPorFilialDTO;
import com.dashboard.api.dto.coletas.ColetasStatusDistribuicaoDTO;
import com.dashboard.api.dto.coletas.ColetasTrendPointDTO;
import com.dashboard.api.model.VisaoColetasEntity;
import com.dashboard.api.repository.VisaoColetasRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ColetasService {

    private static final Logger log = LoggerFactory.getLogger(ColetasService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<String> ORDEM_AGING = List.of("0-2 dias", "3-5 dias", "6-10 dias", "11+ dias");

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoColetasRepository repository;
    private final EscopoFilialService escopoFilialService;

    public ColetasService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoColetasRepository repository,
            EscopoFilialService escopoFilialService
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.repository = repository;
        this.escopoFilialService = escopoFilialService;
    }

    public ColetasOverviewDTO buscarOverview(LocalDate dataInicio, LocalDate dataFim) {
        return buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public ColetasOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoColetasEntity> coletas = buscarRegistros(filtro);
        int totalColetas = coletas.size();

        if (totalColetas == 0) {
            return new ColetasOverviewDTO(
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    0, 0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    BigDecimal.ZERO, BigDecimal.ZERO
            );
        }

        long finalizadasCount = coletas.stream()
                .filter(this::isFinalizada)
                .count();

        double taxaSucesso = percentual(finalizadasCount, totalColetas);

        long canceladasCount = coletas.stream()
                .filter(c -> "Cancelada".equalsIgnoreCase(c.getStatus()))
                .count();

        double taxaCancelamento = percentual(canceladasCount, totalColetas);

        List<VisaoColetasEntity> finalizadas = coletas.stream()
                .filter(this::isFinalizada)
                .toList();

        double slaNoAgendamento = 0.0;
        if (!finalizadas.isEmpty()) {
            long dentroSla = finalizadas.stream()
                    .filter(c -> c.getFinalizacao() != null
                            && c.getAgendamento() != null
                            && !c.getFinalizacao().isAfter(c.getAgendamento()))
                    .count();
            slaNoAgendamento = percentual(dentroSla, finalizadas.size());
        }

        double leadTimeMedioDias = finalizadas.stream()
                .filter(c -> c.getSolicitacao() != null && c.getFinalizacao() != null)
                .mapToLong(c -> ChronoUnit.DAYS.between(c.getSolicitacao(), c.getFinalizacao()))
                .average()
                .orElse(0.0);
        leadTimeMedioDias = BigDecimal.valueOf(leadTimeMedioDias).setScale(2, RoundingMode.HALF_UP).doubleValue();

        double tentativasMedias = 0.0;

        BigDecimal pesoTaxadoTotal = coletas.stream()
                .map(VisaoColetasEntity::getPesoTaxado)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorNfTotal = coletas.stream()
                .map(VisaoColetasEntity::getValorNf)
                .map(ConsultaFiltroUtils::zeroSeNulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Overview coletas calculado: totalColetas={}, finalizadas={}, periodo={} a {}",
                totalColetas, finalizadasCount, filtro.dataInicio(), filtro.dataFim());

        return new ColetasOverviewDTO(
                ConsultaFiltroUtils.latestUpdate(coletas, VisaoColetasEntity::getDataExtracao),
                totalColetas,
                (int) finalizadasCount,
                taxaSucesso,
                taxaCancelamento,
                slaNoAgendamento,
                leadTimeMedioDias,
                tentativasMedias,
                pesoTaxadoTotal.setScale(2, RoundingMode.HALF_UP),
                valorNfTotal.setScale(2, RoundingMode.HALF_UP)
        );
    }

    public List<ColetasTrendPointDTO> buscarSerieTemporal(LocalDate dataInicio, LocalDate dataFim) {
        return buscarSerieTemporal(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public List<ColetasTrendPointDTO> buscarSerieTemporal(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        Map<LocalDate, List<VisaoColetasEntity>> agrupado = buscarRegistros(filtro).stream()
                .filter(c -> c.getSolicitacao() != null)
                .collect(Collectors.groupingBy(VisaoColetasEntity::getSolicitacao));

        return agrupado.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<VisaoColetasEntity> grupo = entry.getValue();
                    int finalizadas = (int) grupo.stream().filter(this::isFinalizada).count();
                    int canceladas = (int) grupo.stream().filter(c -> "Cancelada".equalsIgnoreCase(c.getStatus())).count();
                    int emTratativa = (int) grupo.stream().filter(c -> "Em tratativa".equalsIgnoreCase(c.getStatus())).count();

                    return new ColetasTrendPointDTO(
                            entry.getKey().format(DATE_FMT),
                            grupo.size(),
                            finalizadas,
                            canceladas,
                            emTratativa
                    );
                })
                .toList();
    }

    public List<ColetaResumoDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 200);

        return buscarRegistrosTabela(filtro, limiteAplicado).stream()
                .map(c -> new ColetaResumoDTO(
                        c.getId(),
                        c.getColeta(),
                        c.getSolicitacao() != null ? c.getSolicitacao().toString() : null,
                        c.getAgendamento() != null ? c.getAgendamento().toString() : null,
                        c.getFinalizacao() != null ? c.getFinalizacao().toString() : null,
                        c.getStatus(),
                        c.getVolumes(),
                        ConsultaFiltroUtils.zeroSeNulo(c.getPesoTaxado()).setScale(2, RoundingMode.HALF_UP),
                        ConsultaFiltroUtils.zeroSeNulo(c.getValorNf()).setScale(2, RoundingMode.HALF_UP),
                        c.getNumeroManifesto(),
                        c.getClienteNome(),
                        c.getCidadeColeta(),
                        c.getUfColeta(),
                        c.getRegiaoColeta(),
                        c.getFilialNome(),
                        c.getUsuarioNome(),
                        c.getMotivoCancelamento(),
                        null
                ))
                .toList();
    }

    public ColetasChartsDTO buscarGraficos(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoColetasEntity> coletas = buscarRegistros(filtro);

        List<ColetasStatusDistribuicaoDTO> statusDistribuicao = coletas.stream()
                .collect(Collectors.groupingBy(c -> textoOuPadrao(c.getStatus(), "Sem status"), Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new ColetasStatusDistribuicaoDTO(entry.getKey(), entry.getValue().intValue()))
                .sorted(Comparator.comparing(ColetasStatusDistribuicaoDTO::total).reversed()
                        .thenComparing(ColetasStatusDistribuicaoDTO::status))
                .toList();

        List<ColetasSlaPorFilialDTO> slaPorFilial = coletas.stream()
                .filter(this::isFinalizada)
                .filter(c -> temTexto(c.getFilialNome()))
                .collect(Collectors.groupingBy(VisaoColetasEntity::getFilialNome))
                .entrySet().stream()
                .map(entry -> {
                    long dentroSla = entry.getValue().stream()
                            .filter(c -> c.getFinalizacao() != null
                                    && c.getAgendamento() != null
                                    && !c.getFinalizacao().isAfter(c.getAgendamento()))
                            .count();
                    return new ColetasSlaPorFilialDTO(
                            entry.getKey(),
                            percentual(dentroSla, entry.getValue().size()),
                            entry.getValue().size()
                    );
                })
                .sorted(Comparator.comparing(ColetasSlaPorFilialDTO::slaPct).reversed()
                        .thenComparing(ColetasSlaPorFilialDTO::filial))
                .limit(8)
                .toList();

        List<ColetasRegiaoVolumeDTO> regiaoVolume = coletas.stream()
                .collect(Collectors.groupingBy(c -> textoOuPadrao(c.getRegiaoColeta(), "Sem regiao")))
                .entrySet().stream()
                .map(entry -> new ColetasRegiaoVolumeDTO(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .map(VisaoColetasEntity::getPesoTaxado)
                                .map(ConsultaFiltroUtils::zeroSeNulo)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP),
                        entry.getValue().stream().mapToInt(c -> ConsultaFiltroUtils.zeroSeNulo(c.getVolumes())).sum()
                ))
                .sorted(Comparator.comparing(ColetasRegiaoVolumeDTO::totalColetas).reversed()
                        .thenComparing(ColetasRegiaoVolumeDTO::regiao))
                .toList();

        Map<String, Integer> agingBuckets = new LinkedHashMap<>();
        for (VisaoColetasEntity coleta : coletas) {
            if (isFinalizada(coleta) || "Cancelada".equalsIgnoreCase(coleta.getStatus())) {
                continue;
            }

            String faixa = agingBucket(coleta.getSolicitacao());
            agingBuckets.put(faixa, agingBuckets.getOrDefault(faixa, 0) + 1);
        }

        List<ColetasAgingBucketDTO> agingAbertas = ORDEM_AGING.stream()
                .map(faixa -> new ColetasAgingBucketDTO(faixa, agingBuckets.getOrDefault(faixa, 0)))
                .toList();

        return new ColetasChartsDTO(statusDistribuicao, slaPorFilial, regiaoVolume, agingAbertas);
    }

    @SuppressWarnings("null")
    private List<VisaoColetasEntity> buscarRegistros(FiltroConsultaDTO filtro) {
        Map<String, VisaoColetasEntity> deduplicado = new LinkedHashMap<>();
        Specification<VisaoColetasEntity> specification = criarSpecification(filtro);
        repository.findAll(
                        specification,
                        Sort.by(
                                Sort.Order.desc("dataExtracao"),
                                Sort.Order.desc("solicitacao"),
                                Sort.Order.asc("id")
                        )
                ).forEach(row -> deduplicado.putIfAbsent(row.getId(), row));
        return List.copyOf(deduplicado.values());
    }

    private boolean isFinalizada(VisaoColetasEntity coleta) {
        return "Finalizada".equalsIgnoreCase(coleta.getStatus())
                || "Coletada".equalsIgnoreCase(coleta.getStatus());
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

    private String agingBucket(LocalDate solicitacao) {
        long dias = solicitacao == null ? 0 : ChronoUnit.DAYS.between(solicitacao, LocalDate.now());
        if (dias <= 2) {
            return "0-2 dias";
        }
        if (dias <= 5) {
            return "3-5 dias";
        }
        if (dias <= 10) {
            return "6-10 dias";
        }
        return "11+ dias";
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String textoOuPadrao(String valor, String padrao) {
        return Objects.requireNonNullElse(valor, "").isBlank() ? padrao : valor;
    }

    @SuppressWarnings("null")
    private List<VisaoColetasEntity> buscarRegistrosTabela(FiltroConsultaDTO filtro, int limite) {
        Map<String, VisaoColetasEntity> deduplicado = new LinkedHashMap<>();
        int tamanhoPagina = Math.min(Math.max(limite * 3, 100), 500);
        int paginaAtual = 0;
        Specification<VisaoColetasEntity> specification = criarSpecification(filtro);

        while (deduplicado.size() < limite) {
            Page<VisaoColetasEntity> pagina = repository.findAll(
                    specification,
                    PageRequest.of(
                            paginaAtual,
                            tamanhoPagina,
                            Sort.by(
                                    Sort.Order.desc("solicitacao"),
                                    Sort.Order.desc("dataExtracao"),
                                    Sort.Order.asc("id")
                            )
                    )
            );

            pagina.getContent().forEach(row -> deduplicado.putIfAbsent(row.getId(), row));

            if (pagina.isLast() || pagina.getContent().isEmpty()) {
                break;
            }
            paginaAtual++;
        }

        return deduplicado.values().stream()
                .limit(limite)
                .toList();
    }

    private Specification<VisaoColetasEntity> criarSpecification(FiltroConsultaDTO filtro) {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.between("solicitacao", filtro.dataInicio(), filtro.dataFim()),
                ConsultaSpecificationUtils.escopoFiliais(escopo, "filialNome"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "filiais", "filialNome"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "clientes", "clienteNome"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "status", "status"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "regioes", "regiaoColeta"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "usuarios", "usuarioNome")
        );
    }
}
