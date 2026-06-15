package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.manifestos.ManifestosPerformanceDTO;
import com.dashboard.api.repository.ManifestosPerformanceSqlRepository;
import org.springframework.stereotype.Service;

@Service
public class ManifestosPerformanceService {

    private final ManifestosPerformanceSqlRepository repository;
    private final ManifestosCostGoalService costGoalService;

    public ManifestosPerformanceService(
            ManifestosPerformanceSqlRepository repository,
            ManifestosCostGoalService costGoalService
    ) {
        this.repository = repository;
        this.costGoalService = costGoalService;
    }

    public ManifestosPerformanceDTO buscarPerformance(
            FiltroConsultaDTO filtro,
            String nivel,
            Integer ano,
            Integer mes
    ) {
        ManifestosPerformanceDTO performance = repository.buscarPerformance(filtro, nivel, ano, mes);
        return new ManifestosPerformanceDTO(
                performance.updatedAt(),
                performance.kpis(),
                performance.remuneracao(),
                performance.aproveitamento(),
                performance.efetividade(),
                performance.statusSazonal(),
                performance.custosContrato(),
                performance.tiposVeiculo(),
                costGoalService.calcular(filtro, performance.kpis().custoTotal())
        );
    }
}
