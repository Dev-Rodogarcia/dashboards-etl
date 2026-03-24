package com.dashboard.api.controller;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.contaspagar.ContaPagarResumoDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarChartsDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarMensalTrendDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarOverviewDTO;
import com.dashboard.api.service.ContasAPagarService;
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
@RequestMapping("/api/painel/contas-a-pagar")
@PreAuthorize("@acessoSeguranca.podeAcessar('contasAPagar')")
public class ContasAPagarController {

    private static final Logger log = LoggerFactory.getLogger(ContasAPagarController.class);
    private final ContasAPagarService contasAPagarService;

    public ContasAPagarController(ContasAPagarService contasAPagarService) {
        this.contasAPagarService = contasAPagarService;
    }

    @GetMapping
    public ResponseEntity<ContasAPagarOverviewDTO> overview(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        FiltroConsultaDTO filtro = FiltroRequestMapper.from(dataInicio, dataFim, params);
        log.info("GET /api/painel/contas-a-pagar - periodo: {} a {}", dataInicio, dataFim);
        return ResponseEntity.ok(contasAPagarService.buscarOverview(filtro));
    }

    @GetMapping("/serie")
    public ResponseEntity<List<ContasAPagarMensalTrendDTO>> serie(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(contasAPagarService.buscarSerie(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/graficos")
    public ResponseEntity<ContasAPagarChartsDTO> graficos(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(contasAPagarService.buscarGraficos(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/tabela")
    public ResponseEntity<List<ContaPagarResumoDTO>> tabela(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "100") int limite,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(contasAPagarService.buscarTabela(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }
}
