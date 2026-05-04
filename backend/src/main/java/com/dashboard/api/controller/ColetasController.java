package com.dashboard.api.controller;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.coletas.ColetaResumoDTO;
import com.dashboard.api.dto.coletas.ColetasChartsDTO;
import com.dashboard.api.dto.coletas.ColetasOverviewDTO;
import com.dashboard.api.dto.coletas.ColetasTrendPointDTO;
import com.dashboard.api.service.ColetasService;
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
@RequestMapping("/api/painel/coletas")
@PreAuthorize("@acessoSeguranca.podeAcessar('coletas')")
public class ColetasController {

    private static final Logger log = LoggerFactory.getLogger(ColetasController.class);
    private final ColetasService coletasService;

    public ColetasController(ColetasService coletasService) {
        this.coletasService = coletasService;
    }

    @GetMapping
    public ResponseEntity<ColetasOverviewDTO> overview(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        FiltroConsultaDTO filtro = FiltroRequestMapper.from(dataInicio, dataFim, params);
        log.info("GET /api/painel/coletas - periodo: {} a {}", dataInicio, dataFim);
        return ResponseEntity.ok(coletasService.buscarOverview(filtro));
    }

    @GetMapping("/serie")
    public ResponseEntity<List<ColetasTrendPointDTO>> serie(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(coletasService.buscarSerieTemporal(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/graficos")
    public ResponseEntity<ColetasChartsDTO> graficos(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(coletasService.buscarGraficos(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/tabela")
    public ResponseEntity<List<ColetaResumoDTO>> tabela(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "100") int limite,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(coletasService.buscarTabela(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }
}
