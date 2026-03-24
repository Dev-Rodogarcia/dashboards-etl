package com.dashboard.api.controller;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.executivo.ExecutivoOverviewDTO;
import com.dashboard.api.dto.executivo.ExecutivoTrendPointDTO;
import com.dashboard.api.service.ExecutivoService;
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
@RequestMapping("/api/painel/executivo")
@PreAuthorize("@acessoSeguranca.podeAcessar('executivo')")
public class ExecutivoController {

    private static final Logger log = LoggerFactory.getLogger(ExecutivoController.class);
    private final ExecutivoService executivoService;

    public ExecutivoController(ExecutivoService executivoService) {
        this.executivoService = executivoService;
    }

    @GetMapping
    public ResponseEntity<ExecutivoOverviewDTO> overview(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        FiltroConsultaDTO filtro = FiltroRequestMapper.from(dataInicio, dataFim, params);
        log.info("GET /api/painel/executivo - periodo: {} a {}", dataInicio, dataFim);
        return ResponseEntity.ok(executivoService.buscarOverview(filtro));
    }

    @GetMapping("/serie")
    public ResponseEntity<List<ExecutivoTrendPointDTO>> serie(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(executivoService.buscarSerie(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }
}
