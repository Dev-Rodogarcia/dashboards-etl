package com.dashboard.api.service;

import com.dashboard.api.dto.etl.EtlExecucaoTrendPointDTO;
import com.dashboard.api.dto.etl.EtlInsercoesAtualizacoesPointDTO;
import com.dashboard.api.dto.etl.EtlLogExtracaoAuditoriaDTO;
import com.dashboard.api.dto.etl.EtlSaudeChartsDTO;
import com.dashboard.api.dto.etl.EtlSaudeOverviewDTO;
import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.repository.EtlSaudeSqlRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EtlSaudeService {

    private static final Logger log = LoggerFactory.getLogger(EtlSaudeService.class);

    private final ValidadorPeriodoService validadorPeriodo;
    private final EtlSaudeSqlRepository sqlRepository;

    public EtlSaudeService(
            ValidadorPeriodoService validadorPeriodo,
            EtlSaudeSqlRepository sqlRepository
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.sqlRepository = Objects.requireNonNull(sqlRepository, "sqlRepository");
    }

    public EtlSaudeOverviewDTO buscarOverview(LocalDate dataInicio, LocalDate dataFim) {
        return buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public EtlSaudeOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        EtlSaudeOverviewDTO overview = sqlRepository.buscarOverview(filtro);
        log.info("ETL Saude overview calculado via SQL: totalExecucoes={}, periodo={} a {}",
                overview.totalExecucoes(), filtro.dataInicio(), filtro.dataFim());
        return overview;
    }

    public List<EtlExecucaoTrendPointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarSerie(filtro);
    }

    public List<EtlInsercoesAtualizacoesPointDTO> buscarEvolucaoInsercoesAtualizacoes(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarEvolucaoInsercoesAtualizacoes(filtro);
    }

    public List<EtlLogExtracaoAuditoriaDTO> buscarTabela(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return sqlRepository.buscarTabela(filtro);
    }

    public long totalTabela(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.totalTabela(filtro);
    }

    public EtlSaudeChartsDTO buscarGraficos(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarGraficos(filtro);
    }
}
