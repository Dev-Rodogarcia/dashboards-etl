package com.dashboard.api.controller;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.cotacoes.CotacaoResumoDTO;
import com.dashboard.api.dto.cotacoes.CotacoesChartsDTO;
import com.dashboard.api.dto.cotacoes.CotacoesOverviewDTO;
import com.dashboard.api.dto.cotacoes.CotacoesTrendPointDTO;
import com.dashboard.api.service.CotacoesService;
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
@RequestMapping("/api/painel/cotacoes")
@PreAuthorize("@acessoSeguranca.podeAcessar('cotacoes')")
public class CotacoesController {

    private static final Logger log = LoggerFactory.getLogger(CotacoesController.class);
    private final CotacoesService cotacoesService;

    public CotacoesController(CotacoesService cotacoesService) {
        this.cotacoesService = cotacoesService;
    }

    @GetMapping
    public ResponseEntity<CotacoesOverviewDTO> overview(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        FiltroConsultaDTO filtro = FiltroRequestMapper.from(dataInicio, dataFim, params);
        log.info("GET /api/painel/cotacoes - periodo: {} a {}", dataInicio, dataFim);
        return ResponseEntity.ok(cotacoesService.buscarOverview(filtro));
    }

    @GetMapping("/serie")
    public ResponseEntity<List<CotacoesTrendPointDTO>> serie(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(cotacoesService.buscarSerie(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/graficos")
    public ResponseEntity<CotacoesChartsDTO> graficos(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(cotacoesService.buscarGraficos(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/tabela")
    public ResponseEntity<List<CotacaoResumoDTO>> tabela(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "100") int limite,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(cotacoesService.buscarTabela(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }
}
