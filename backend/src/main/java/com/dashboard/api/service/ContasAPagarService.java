package com.dashboard.api.service;

import com.dashboard.api.dto.contaspagar.ContaPagarResumoDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarChartsDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarDrilldownNivel;
import com.dashboard.api.dto.contaspagar.ContasAPagarDrilldownPointDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarGranularidade;
import com.dashboard.api.dto.contaspagar.ContasAPagarMensalTrendDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarMetrica;
import com.dashboard.api.dto.contaspagar.ContasAPagarOverviewDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarReferenciaTemporal;
import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.repository.ContasAPagarSqlRepository;
import com.dashboard.api.util.ConsultaLimiteUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ContasAPagarService {

    private final ValidadorPeriodoService validadorPeriodo;
    private final ContasAPagarSqlRepository sqlRepository;
    private final DashboardTabelaPaginadaService tabelaPaginadaService;

    public ContasAPagarService(
            ValidadorPeriodoService validadorPeriodo,
            ContasAPagarSqlRepository sqlRepository,
            DashboardTabelaPaginadaService tabelaPaginadaService
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.sqlRepository = sqlRepository;
        this.tabelaPaginadaService = tabelaPaginadaService;
    }

    public ContasAPagarOverviewDTO buscarOverview(LocalDate dataInicio, LocalDate dataFim) {
        return buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public ContasAPagarOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarOverview(filtro);
    }

    public List<ContasAPagarMensalTrendDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarSerie(filtro);
    }

    public List<ContasAPagarMensalTrendDTO> buscarSerie(
            FiltroConsultaDTO filtro,
            String granularidade,
            String referencia
    ) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarSerie(
                filtro,
                ContasAPagarGranularidade.from(granularidade),
                ContasAPagarReferenciaTemporal.from(referencia)
        );
    }

    public List<ContaPagarResumoDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 200);
        return tabelaPaginadaService.buscarPrimeiraPaginaContasAPagar(filtro, limiteAplicado);
    }

    public ContasAPagarChartsDTO buscarGraficos(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarGraficos(filtro);
    }

    public List<ContasAPagarDrilldownPointDTO> buscarDrilldownFornecedores(
            FiltroConsultaDTO filtro,
            int limite,
            String metrica,
            String nivel,
            String fornecedor,
            String classificacao
    ) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarDrilldownFornecedores(filtro, normalizarLimite(limite), ContasAPagarMetrica.from(metrica),
                ContasAPagarDrilldownNivel.from(nivel), fornecedor, classificacao);
    }

    public List<ContasAPagarDrilldownPointDTO> buscarDrilldownCentroCusto(
            FiltroConsultaDTO filtro,
            int limite,
            String metrica,
            String nivel,
            String centroCusto,
            String classificacao
    ) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarDrilldownCentroCusto(filtro, normalizarLimite(limite), ContasAPagarMetrica.from(metrica),
                ContasAPagarDrilldownNivel.from(nivel), centroCusto, classificacao);
    }

    private int normalizarLimite(int limite) {
        return limite == 5 || limite == 15 ? limite : 10;
    }

}
