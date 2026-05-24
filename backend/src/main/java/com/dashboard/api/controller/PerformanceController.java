package com.dashboard.api.controller;

import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.dto.performance.PerformanceAgingPointDTO;
import com.dashboard.api.dto.performance.PerformanceDrilldownPointDTO;
import com.dashboard.api.dto.performance.PerformanceEntregaRowDTO;
import com.dashboard.api.dto.performance.PerformanceHistoricoPointDTO;
import com.dashboard.api.dto.performance.PerformanceOverviewDTO;
import com.dashboard.api.dto.performance.PerformanceSerieTemporalPointDTO;
import com.dashboard.api.dto.performance.PerformanceStatusDistribuicaoDTO;
import com.dashboard.api.service.PerformanceDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/painel/performance")
@PreAuthorize("@acessoSeguranca.podeAcessar('performance')")
public class PerformanceController {

    private final PerformanceDashboardService service;

    public PerformanceController(PerformanceDashboardService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public ResponseEntity<PerformanceOverviewDTO> overview(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(service.buscarOverview(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/serie-temporal")
    public ResponseEntity<List<PerformanceSerieTemporalPointDTO>> serieTemporal(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "dia") String nivel,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(service.buscarSerieTemporal(
                FiltroRequestMapper.from(dataInicio, dataFim, params),
                nivel,
                ano,
                mes
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<List<PerformanceStatusDistribuicaoDTO>> status(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(service.buscarStatus(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/historico")
    public ResponseEntity<List<PerformanceHistoricoPointDTO>> historico(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(service.buscarHistorico(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/drilldown")
    public ResponseEntity<List<PerformanceDrilldownPointDTO>> drilldown(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "responsavel") String nivel,
            @RequestParam(required = false) String responsavel,
            @RequestParam(required = false) String regiaoDestino,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(service.buscarDrilldown(
                FiltroRequestMapper.from(dataInicio, dataFim, params),
                nivel,
                responsavel,
                regiaoDestino
        ));
    }

    @GetMapping("/aging")
    public ResponseEntity<List<PerformanceAgingPointDTO>> aging(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(service.buscarAging(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/tabela/paginada")
    public ResponseEntity<PaginaDTO<PerformanceEntregaRowDTO>> tabelaPaginada(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanhoPagina,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(service.buscarTabelaPaginada(
                FiltroRequestMapper.from(dataInicio, dataFim, params),
                pagina,
                tamanhoPagina
        ));
    }
}
