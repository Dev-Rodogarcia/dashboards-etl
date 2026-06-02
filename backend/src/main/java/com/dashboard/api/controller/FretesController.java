package com.dashboard.api.controller;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.fretes.FreteResumoDTO;
import com.dashboard.api.dto.fretes.FretesChartsDTO;
import com.dashboard.api.dto.fretes.FretesClienteRankingDTO;
import com.dashboard.api.dto.fretes.FretesDocumentMixDTO;
import com.dashboard.api.dto.fretes.FretesGoalConfigDTO;
import com.dashboard.api.dto.fretes.FretesGoalConfigRequestDTO;
import com.dashboard.api.dto.fretes.FretesGoalSummaryDTO;
import com.dashboard.api.dto.fretes.FretesOverviewDTO;
import com.dashboard.api.dto.fretes.FretesTrendPointDTO;
import com.dashboard.api.service.FretesGoalService;
import com.dashboard.api.service.FretesService;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/painel/fretes")
@PreAuthorize("@acessoSeguranca.podeAcessar('fretes')")
public class FretesController {

    private static final Logger log = LoggerFactory.getLogger(FretesController.class);
    private final FretesService fretesService;
    private final FretesGoalService fretesGoalService;

    public FretesController(FretesService fretesService, FretesGoalService fretesGoalService) {
        this.fretesService = fretesService;
        this.fretesGoalService = fretesGoalService;
    }

    @GetMapping
    public ResponseEntity<FretesOverviewDTO> overview(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        FiltroConsultaDTO filtro = FiltroRequestMapper.from(dataInicio, dataFim, params);
        log.info("GET /api/painel/fretes - periodo: {} a {}", dataInicio, dataFim);
        return ResponseEntity.ok(fretesService.buscarOverview(filtro));
    }

    @GetMapping("/serie")
    public ResponseEntity<List<FretesTrendPointDTO>> serie(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(fretesService.buscarSerieTemporal(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/top-clientes")
    public ResponseEntity<List<FretesClienteRankingDTO>> topClientes(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "10") int limite,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(fretesService.buscarTopClientes(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }

    @GetMapping("/mix-documental")
    public ResponseEntity<List<FretesDocumentMixDTO>> mixDocumental(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(fretesService.buscarMixDocumental(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/metas")
    public ResponseEntity<FretesGoalSummaryDTO> metas(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(fretesService.buscarResumoMetas(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/metas/configuracoes")
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public ResponseEntity<List<FretesGoalConfigDTO>> metasConfiguracoes(
            @RequestParam int ano,
            @RequestParam int mes) {
        return ResponseEntity.ok(fretesGoalService.buscarConfiguracoes(ano, mes));
    }

    @PutMapping("/metas/configuracoes")
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public ResponseEntity<FretesGoalConfigDTO> salvarMetaConfiguracao(
            @RequestBody FretesGoalConfigRequestDTO request,
            Authentication authentication) {
        return ResponseEntity.ok(fretesGoalService.salvarConfiguracao(request, usuarioLogin(authentication)));
    }

    @DeleteMapping("/metas/configuracoes")
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public ResponseEntity<Void> removerMetaConfiguracao(
            @RequestParam(required = false) String branchId,
            @RequestParam int ano,
            @RequestParam int mes) {
        fretesGoalService.removerConfiguracao(branchId, ano, mes);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/graficos")
    public ResponseEntity<FretesChartsDTO> graficos(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(fretesService.buscarGraficos(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/tabela")
    public ResponseEntity<List<FreteResumoDTO>> tabela(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "100") int limite,
            @RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.ok(fretesService.buscarTabela(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }

    private String usuarioLogin(Authentication authentication) {
        return authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "";
    }
}
