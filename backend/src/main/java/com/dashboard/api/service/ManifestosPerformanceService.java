package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.manifestos.ManifestosPerformanceDTO;
import com.dashboard.api.repository.ManifestosPerformanceSqlRepository;
import org.springframework.stereotype.Service;

@Service
public class ManifestosPerformanceService {

    private final ManifestosPerformanceSqlRepository repository;

    public ManifestosPerformanceService(ManifestosPerformanceSqlRepository repository) {
        this.repository = repository;
    }

    public ManifestosPerformanceDTO buscarPerformance(
            FiltroConsultaDTO filtro,
            String nivel,
            Integer ano,
            Integer mes
    ) {
        return repository.buscarPerformance(filtro, nivel, ano, mes);
    }
}
