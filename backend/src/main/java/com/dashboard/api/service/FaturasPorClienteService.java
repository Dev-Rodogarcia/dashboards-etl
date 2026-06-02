package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.faturascliente.FaturaPorClienteResumoDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteAgingBucketDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteMensalDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteOverviewDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteStatusProcessoDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteTopClienteDTO;
import com.dashboard.api.repository.FaturasPorClienteSqlRepository;
import com.dashboard.api.util.ConsultaLimiteUtils;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FaturasPorClienteService {

    private final ValidadorPeriodoService validadorPeriodo;
    private final Clock clock;
    private final FaturasPorClienteSqlRepository sqlRepository;
    private final DashboardTabelaPaginadaService tabelaPaginadaService;

    @Autowired
    public FaturasPorClienteService(
            ValidadorPeriodoService validadorPeriodo,
            FaturasPorClienteSqlRepository sqlRepository,
            DashboardTabelaPaginadaService tabelaPaginadaService
    ) {
        this(validadorPeriodo, sqlRepository, Clock.systemDefaultZone(), tabelaPaginadaService);
    }

    FaturasPorClienteService(
            ValidadorPeriodoService validadorPeriodo,
            FaturasPorClienteSqlRepository sqlRepository,
            Clock clock,
            DashboardTabelaPaginadaService tabelaPaginadaService
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.sqlRepository = sqlRepository;
        this.clock = clock;
        this.tabelaPaginadaService = tabelaPaginadaService;
    }

    public FaturasPorClienteOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarOverview(filtro, hoje());
    }

    public List<FaturasPorClienteMensalDTO> buscarMensal(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarMensal(filtro);
    }

    public List<FaturasPorClienteAgingBucketDTO> buscarAging(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarAging(filtro, hoje());
    }

    public List<FaturasPorClienteTopClienteDTO> buscarTopClientes(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 10, 50);
        return sqlRepository.buscarTopClientes(filtro, limiteAplicado);
    }

    public List<FaturasPorClienteStatusProcessoDTO> buscarStatusProcesso(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarStatusProcesso(filtro);
    }

    public List<FaturaPorClienteResumoDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 200);
        return tabelaPaginadaService().buscarPrimeiraPaginaFaturasPorCliente(filtro, limiteAplicado);
    }

    private DashboardTabelaPaginadaService tabelaPaginadaService() {
        if (tabelaPaginadaService == null) {
            throw new IllegalStateException("Tabela de Faturas por Cliente exige DashboardTabelaPaginadaService configurado.");
        }
        return tabelaPaginadaService;
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }
}
