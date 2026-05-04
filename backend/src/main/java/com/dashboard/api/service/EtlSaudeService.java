package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.etl.EtlCategoriaErroDTO;
import com.dashboard.api.dto.etl.EtlExecucaoResumoDTO;
import com.dashboard.api.dto.etl.EtlExecucaoTrendPointDTO;
import com.dashboard.api.dto.etl.EtlSaudeChartsDTO;
import com.dashboard.api.dto.etl.EtlSaudeOverviewDTO;
import com.dashboard.api.model.VisaoMonitoramentoEntity;
import com.dashboard.api.repository.VisaoMonitoramentoRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class EtlSaudeService {

    private static final Logger log = LoggerFactory.getLogger(EtlSaudeService.class);

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoMonitoramentoRepository repository;

    public EtlSaudeService(ValidadorPeriodoService validadorPeriodo, VisaoMonitoramentoRepository repository) {
        this.validadorPeriodo = validadorPeriodo;
        this.repository = repository;
    }

    public EtlSaudeOverviewDTO buscarOverview(LocalDate dataInicio, LocalDate dataFim) {
        return buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public EtlSaudeOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<VisaoMonitoramentoEntity> execucoes = buscarRegistros(filtro);
        int totalExecucoes = execucoes.size();

        if (totalExecucoes == 0) {
            return new EtlSaudeOverviewDTO(
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    0.0, 0, 0, 0, 0.0
            );
        }

        double tempoMedioExecucaoSegundos = execucoes.stream()
                .mapToInt(e -> e.getDuracaoSegundos() != null ? e.getDuracaoSegundos() : 0)
                .average()
                .orElse(0.0);
        tempoMedioExecucaoSegundos = BigDecimal.valueOf(tempoMedioExecucaoSegundos)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        int execucoesComErro = (int) execucoes.stream()
                .filter(e -> !"SUCCESS".equalsIgnoreCase(e.getStatus()))
                .count();

        int volumeProcessadoTotal = execucoes.stream()
                .mapToInt(e -> e.getTotalRegistros() != null ? e.getTotalRegistros() : 0)
                .sum();

        double taxaSucesso = percentual(execucoes.stream()
                .filter(e -> "SUCCESS".equalsIgnoreCase(e.getStatus()))
                .count(), totalExecucoes);

        log.info("ETL Saude overview calculado: totalExecucoes={}, periodo={} a {}", totalExecucoes, filtro.dataInicio(), filtro.dataFim());

        return new EtlSaudeOverviewDTO(
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                tempoMedioExecucaoSegundos,
                execucoesComErro,
                totalExecucoes,
                volumeProcessadoTotal,
                taxaSucesso
        );
    }

    public List<EtlExecucaoTrendPointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return buscarRegistros(filtro).stream()
                .collect(Collectors.groupingBy(VisaoMonitoramentoEntity::getData))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<VisaoMonitoramentoEntity> grupo = entry.getValue();
                    double duracaoMedia = grupo.stream()
                            .mapToInt(e -> e.getDuracaoSegundos() != null ? e.getDuracaoSegundos() : 0)
                            .average()
                            .orElse(0.0);

                    return new EtlExecucaoTrendPointDTO(
                            entry.getKey().toString(),
                            grupo.size(),
                            (int) grupo.stream().filter(e -> !"SUCCESS".equalsIgnoreCase(e.getStatus())).count(),
                            grupo.stream().mapToInt(e -> e.getTotalRegistros() != null ? e.getTotalRegistros() : 0).sum(),
                            BigDecimal.valueOf(duracaoMedia).setScale(2, RoundingMode.HALF_UP).doubleValue()
                    );
                })
                .toList();
    }

    public List<EtlExecucaoResumoDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 200);

        return repository.findAll(
                        criarSpecification(filtro),
                        PageRequest.of(0, limiteAplicado, Sort.by(Sort.Direction.DESC, "inicio"))
                ).getContent().stream()
                .map(e -> new EtlExecucaoResumoDTO(
                        e.getId(),
                        e.getInicio() != null ? e.getInicio().toString() : null,
                        e.getFim() != null ? e.getFim().toString() : null,
                        e.getDuracaoSegundos(),
                        e.getData() != null ? e.getData().toString() : null,
                        e.getStatus(),
                        e.getTotalRegistros(),
                        e.getCategoriaErro(),
                        e.getMensagemErro()
                ))
                .toList();
    }

    public EtlSaudeChartsDTO buscarGraficos(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<EtlCategoriaErroDTO> categoriasErro = buscarRegistros(filtro).stream()
                .map(VisaoMonitoramentoEntity::getCategoriaErro)
                .filter(valor -> !Objects.requireNonNullElse(valor, "").isBlank())
                .collect(Collectors.groupingBy(categoria -> categoria, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new EtlCategoriaErroDTO(entry.getKey(), entry.getValue().intValue()))
                .sorted(Comparator.comparing(EtlCategoriaErroDTO::total).reversed()
                        .thenComparing(EtlCategoriaErroDTO::categoria))
                .toList();

        return new EtlSaudeChartsDTO(categoriasErro);
    }

    private List<VisaoMonitoramentoEntity> buscarRegistros(FiltroConsultaDTO filtro) {
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

    @NonNull
    private Specification<VisaoMonitoramentoEntity> criarSpecification(FiltroConsultaDTO filtro) {
        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.between("data", filtro.dataInicio(), filtro.dataFim()),
                ConsultaSpecificationUtils.filtroTexto(filtro, "status", "status")
        );
    }
}
