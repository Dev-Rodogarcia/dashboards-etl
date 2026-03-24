package com.dashboard.api.controller;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.etl.EtlSaudeChartsDTO;
import com.dashboard.api.dto.etl.EtlExecucaoResumoDTO;
import com.dashboard.api.dto.etl.EtlExecucaoTrendPointDTO;
import com.dashboard.api.dto.etl.EtlSaudeOverviewDTO;
import com.dashboard.api.service.EtlSaudeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/api/painel/etl-saude")
@PreAuthorize("@acessoSeguranca.podeAcessar('etlSaude')")
public class EtlSaudeController {

    private static final Logger log = LoggerFactory.getLogger(EtlSaudeController.class);
    private final EtlSaudeService etlSaudeService;

    public EtlSaudeController(EtlSaudeService etlSaudeService) {
        this.etlSaudeService = etlSaudeService;
    }

    @GetMapping
    public ResponseEntity<EtlSaudeOverviewDTO> overview(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        FiltroConsultaDTO filtro = FiltroRequestMapper.from(dataInicio, dataFim, params);
        log.info("GET /api/painel/etl-saude - periodo: {} a {}", dataInicio, dataFim);
        return ResponseEntity.ok(etlSaudeService.buscarOverview(filtro));
    }

    @GetMapping("/serie")
    public ResponseEntity<List<EtlExecucaoTrendPointDTO>> serie(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(etlSaudeService.buscarSerie(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/graficos")
    public ResponseEntity<EtlSaudeChartsDTO> graficos(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(etlSaudeService.buscarGraficos(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/tabela")
    public ResponseEntity<List<EtlExecucaoResumoDTO>> tabela(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "100") int limite,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(etlSaudeService.buscarTabela(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }
}
