package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.cotacoes.CotacaoResumoDTO;
import com.dashboard.api.dto.cotacoes.CotacoesChartsDTO;
import com.dashboard.api.dto.cotacoes.CotacoesOverviewDTO;
import com.dashboard.api.dto.cotacoes.CotacoesResumoAgregadoDTO;
import com.dashboard.api.dto.cotacoes.CotacoesTrendPointDTO;
import com.dashboard.api.repository.CotacoesDashboardSqlRepository;
import com.dashboard.api.util.ConsultaLimiteUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CotacoesService {

    private static final Logger log = LoggerFactory.getLogger(CotacoesService.class);

    private final ValidadorPeriodoService validadorPeriodo;
    private final CotacoesDashboardSqlRepository dashboardSqlRepository;

    @Autowired
    public CotacoesService(
            ValidadorPeriodoService validadorPeriodo,
            CotacoesDashboardSqlRepository dashboardSqlRepository
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.dashboardSqlRepository = Objects.requireNonNull(dashboardSqlRepository, "dashboardSqlRepository");
    }

    public CotacoesOverviewDTO buscarOverview(LocalDate dataInicio, LocalDate dataFim) {
        return buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public CotacoesOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        CotacoesOverviewDTO overview = dashboardSqlRepository.buscarOverview(filtro);
        log.info("Overview cotacoes calculado via SQL: total={}, periodo={} a {}",
                overview.totalCotacoes(), filtro.dataInicio(), filtro.dataFim());
        return overview;
    }

    public List<CotacoesTrendPointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return dashboardSqlRepository.buscarSerie(filtro);
    }

    public CotacoesChartsDTO buscarGraficos(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return dashboardSqlRepository.buscarGraficos(filtro);
    }

    public List<CotacaoResumoDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 200);

        return dashboardSqlRepository.buscarTabela(filtro, limiteAplicado);
    }

    public List<CotacoesResumoAgregadoDTO> buscarResumoPorUsuario(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return dashboardSqlRepository.buscarResumoPorUsuario(filtro);
    }

    public List<CotacoesResumoAgregadoDTO> buscarResumoPorFilial(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return dashboardSqlRepository.buscarResumoPorFilial(filtro);
    }

    public List<CotacoesResumoAgregadoDTO> buscarResumoPorCliente(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return dashboardSqlRepository.buscarResumoPorCliente(filtro);
    }
}
