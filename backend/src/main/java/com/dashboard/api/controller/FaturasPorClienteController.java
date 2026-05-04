package com.dashboard.api.controller;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.faturascliente.FaturaPorClienteResumoDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteAgingBucketDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteMensalDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteOverviewDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteStatusProcessoDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteTopClienteDTO;
import com.dashboard.api.service.FaturasPorClienteService;
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
@RequestMapping("/api/painel/faturas-por-cliente")
@PreAuthorize("@acessoSeguranca.podeAcessar('faturasPorCliente')")
public class FaturasPorClienteController {

    private static final Logger log = LoggerFactory.getLogger(FaturasPorClienteController.class);
    private final FaturasPorClienteService service;

    public FaturasPorClienteController(FaturasPorClienteService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<FaturasPorClienteOverviewDTO> overview(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        FiltroConsultaDTO filtro = FiltroRequestMapper.from(dataInicio, dataFim, params);
        log.info("GET /api/painel/faturas-por-cliente - periodo: {} a {}", dataInicio, dataFim);
        return ResponseEntity.ok(service.buscarOverview(filtro));
    }

    @GetMapping("/mensal")
    public ResponseEntity<List<FaturasPorClienteMensalDTO>> mensal(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(service.buscarMensal(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/aging")
    public ResponseEntity<List<FaturasPorClienteAgingBucketDTO>> aging(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(service.buscarAging(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/top-clientes")
    public ResponseEntity<List<FaturasPorClienteTopClienteDTO>> topClientes(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "10") int limite,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(service.buscarTopClientes(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }

    @GetMapping("/status-processo")
    public ResponseEntity<List<FaturasPorClienteStatusProcessoDTO>> statusProcesso(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(service.buscarStatusProcesso(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/tabela")
    public ResponseEntity<List<FaturaPorClienteResumoDTO>> tabela(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "100") int limite,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(service.buscarTabela(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }
}
