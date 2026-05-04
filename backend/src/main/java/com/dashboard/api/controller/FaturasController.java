package com.dashboard.api.controller;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.faturas.FaturaReconciliacaoDTO;
import com.dashboard.api.dto.faturas.FaturaResumoDTO;
import com.dashboard.api.dto.faturas.FaturasAgingBucketDTO;
import com.dashboard.api.dto.faturas.FaturasClienteTopDTO;
import com.dashboard.api.dto.faturas.FaturasMensalTrendDTO;
import com.dashboard.api.dto.faturas.FaturasOverviewDTO;
import com.dashboard.api.dto.faturas.FaturasStatusProcessoDTO;
import com.dashboard.api.service.FaturasService;
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
@RequestMapping("/api/painel/faturas")
@PreAuthorize("@acessoSeguranca.podeAcessar('faturas')")
public class FaturasController {

    private static final Logger log = LoggerFactory.getLogger(FaturasController.class);
    private final FaturasService faturasService;

    public FaturasController(FaturasService faturasService) {
        this.faturasService = faturasService;
    }

    @GetMapping
    public ResponseEntity<FaturasOverviewDTO> overview(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        FiltroConsultaDTO filtro = FiltroRequestMapper.from(dataInicio, dataFim, params);
        log.info("GET /api/painel/faturas - periodo: {} a {}", dataInicio, dataFim);
        return ResponseEntity.ok(faturasService.buscarOverview(filtro));
    }

    @GetMapping({"/mensal", "/serie"})
    public ResponseEntity<List<FaturasMensalTrendDTO>> mensal(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(faturasService.buscarMensal(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/aging")
    public ResponseEntity<List<FaturasAgingBucketDTO>> aging(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(faturasService.buscarAging(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/top-clientes")
    public ResponseEntity<List<FaturasClienteTopDTO>> topClientes(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "10") int limite,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(faturasService.buscarTopClientes(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }

    @GetMapping("/status-processo")
    public ResponseEntity<List<FaturasStatusProcessoDTO>> statusProcesso(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(faturasService.buscarStatusProcesso(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/reconciliacao")
    public ResponseEntity<List<FaturaReconciliacaoDTO>> reconciliacao(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "50") int limite,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(faturasService.buscarReconciliacao(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }

    @GetMapping("/tabela")
    public ResponseEntity<List<FaturaResumoDTO>> tabela(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "100") int limite,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(faturasService.buscarTabela(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }
}
