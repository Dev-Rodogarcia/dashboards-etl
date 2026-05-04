package com.dashboard.api.controller;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.manifestos.ManifestoResumoDTO;
import com.dashboard.api.dto.manifestos.ManifestosChartsDTO;
import com.dashboard.api.dto.manifestos.ManifestosOverviewDTO;
import com.dashboard.api.dto.manifestos.ManifestosTrendPointDTO;
import com.dashboard.api.service.ManifestosService;
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
@RequestMapping("/api/painel/manifestos")
@PreAuthorize("@acessoSeguranca.podeAcessar('manifestos')")
public class ManifestosController {

    private static final Logger log = LoggerFactory.getLogger(ManifestosController.class);
    private final ManifestosService manifestosService;

    public ManifestosController(ManifestosService manifestosService) {
        this.manifestosService = manifestosService;
    }

    @GetMapping
    public ResponseEntity<ManifestosOverviewDTO> overview(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        FiltroConsultaDTO filtro = FiltroRequestMapper.from(dataInicio, dataFim, params);
        log.info("GET /api/painel/manifestos - periodo: {} a {}", dataInicio, dataFim);
        return ResponseEntity.ok(manifestosService.buscarOverview(filtro));
    }

    @GetMapping("/serie")
    public ResponseEntity<List<ManifestosTrendPointDTO>> serie(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(manifestosService.buscarSerieTemporal(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/graficos")
    public ResponseEntity<ManifestosChartsDTO> graficos(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(manifestosService.buscarGraficos(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/tabela")
    public ResponseEntity<List<ManifestoResumoDTO>> tabela(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "100") int limite,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(manifestosService.buscarTabela(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }
}
