package com.dashboard.api.service;

import com.dashboard.api.dto.dimensoes.DimensaoOpcaoDTO;
import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.dto.performance.PerformanceAgingPointDTO;
import com.dashboard.api.dto.performance.PerformanceDrilldownPointDTO;
import com.dashboard.api.dto.performance.PerformanceEntregaRowDTO;
import com.dashboard.api.dto.performance.PerformanceHistoricoPointDTO;
import com.dashboard.api.dto.performance.PerformanceOverviewDTO;
import com.dashboard.api.dto.performance.PerformanceSerieTemporalPointDTO;
import com.dashboard.api.dto.performance.PerformanceStatusDistribuicaoDTO;
import com.dashboard.api.dto.performance.PerformanceTabelaProjection;
import com.dashboard.api.repository.PerformanceDashboardSqlRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PerformanceDashboardService {

    private final PerformanceDashboardSqlRepository repository;

    public PerformanceDashboardService(PerformanceDashboardSqlRepository repository) {
        this.repository = repository;
    }

    public List<DimensaoOpcaoDTO> listarResponsaveis(FiltroConsultaDTO filtro) {
        return repository.listarResponsaveis(filtro);
    }

    public List<String> listarRegioesDestino(FiltroConsultaDTO filtro) {
        return repository.listarRegioesDestino(filtro);
    }

    public List<String> listarCidadesDestino(FiltroConsultaDTO filtro) {
        return repository.listarCidadesDestino(filtro);
    }

    public PerformanceOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        return repository.buscarOverview(filtro);
    }

    public List<PerformanceSerieTemporalPointDTO> buscarSerieTemporal(
            FiltroConsultaDTO filtro,
            String nivel,
            Integer ano,
            Integer mes
    ) {
        return repository.buscarSerieTemporal(filtro, nivel, ano, mes);
    }

    public List<PerformanceStatusDistribuicaoDTO> buscarStatus(FiltroConsultaDTO filtro) {
        return repository.buscarStatus(filtro);
    }

    public List<PerformanceHistoricoPointDTO> buscarHistorico(FiltroConsultaDTO filtro) {
        return repository.buscarHistorico(filtro);
    }

    public List<PerformanceDrilldownPointDTO> buscarDrilldown(
            FiltroConsultaDTO filtro,
            String nivel,
            String responsavel,
            String regiaoDestino
    ) {
        return repository.buscarDrilldown(filtro, nivel, responsavel, regiaoDestino);
    }

    public List<PerformanceAgingPointDTO> buscarAging(FiltroConsultaDTO filtro) {
        return repository.buscarAging(filtro);
    }

    public Page<PerformanceTabelaProjection> buscarTabela(FiltroConsultaDTO filtro, Pageable pageable) {
        return repository.buscarTabela(filtro, pageable);
    }

    public List<PerformanceTabelaProjection> buscarExportacao(FiltroConsultaDTO filtro) {
        return repository.buscarTabelaExportacao(filtro);
    }

    public PaginaDTO<PerformanceEntregaRowDTO> buscarTabelaPaginada(
            FiltroConsultaDTO filtro,
            int pagina,
            int tamanhoPagina
    ) {
        return repository.buscarTabelaPaginada(filtro, pagina, tamanhoPagina);
    }
}
