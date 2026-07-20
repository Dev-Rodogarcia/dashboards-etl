package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.manifestos.ManifestosPerformanceDTO;
import com.dashboard.api.repository.ManifestosPerformanceSqlRepository;
import com.dashboard.api.util.FilialKeyUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        FiltroConsultaDTO filtroOrcamento = filtroOrcamentario(filtro);
        return new ManifestosPerformanceDTO(
                performance.updatedAt(),
                diasUteisNoPeriodo(filtro),
                performance.kpis(),
                performance.remuneracao(),
                performance.aproveitamento(),
                performance.efetividade(),
                performance.statusSazonal(),
                performance.custosContrato(),
                performance.tiposVeiculo(),
                costGoalService.calcular(filtro, filtroOrcamento, performance.kpis().custoTotal())
        );
    }

    private int diasUteisNoPeriodo(FiltroConsultaDTO filtro) {
        Integer total = repository.contarDiasUteisCalendario(filtro.dataInicio(), filtro.dataFim());
        return total == null ? 0 : total;
    }

    private FiltroConsultaDTO filtroOrcamentario(FiltroConsultaDTO filtro) {
        List<String> filiaisOriginais = filtro.valores("filiais");
        if (filiaisOriginais.isEmpty()) {
            return filtro;
        }

        List<String> filiaisMetas = FilialKeyUtils.normalizarCodigosParaOrcamento(filiaisOriginais);
        if (filiaisMetas.isEmpty()) {
            return filtro;
        }

        Map<String, List<String>> filtrosOrcamento = new LinkedHashMap<>(filtro.filtros());
        filtrosOrcamento.put("filiais", filiaisMetas);
        return new FiltroConsultaDTO(filtro.dataInicio(), filtro.dataFim(), filtrosOrcamento);
    }
}
