package com.dashboard.api.controller;

import com.dashboard.api.dto.indicadoresgestao.KpiGoalConflictDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalEffectiveDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalHistoryDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalOverridesByIndicatorDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalsFullDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalsUpdateRequestDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.exception.KpiGoalOverrideConflictException;
import com.dashboard.api.service.KpiGoalService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpi-goals")
public class KpiGoalController {

    private final KpiGoalService service;

    public KpiGoalController(KpiGoalService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public KpiGoalsFullDTO buscarCompletas(@RequestParam(required = false) String competencia) {
        return service.buscarMetasCompletas(competencia);
    }

    @GetMapping("/effective")
    @PreAuthorize("@acessoSeguranca.podeAcessar('indicadoresGestaoAVista')")
    public KpiGoalEffectiveDTO buscarEfetiva(
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) String competencia
    ) {
        return service.buscarMetaEfetiva(branchId, competencia);
    }

    @PutMapping("/global")
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public KpiGoalsFullDTO atualizarGlobal(
            @RequestParam(required = false) String competencia,
            @Valid @RequestBody KpiGoalsUpdateRequestDTO request,
            Authentication authentication
    ) {
        return service.atualizarMetasGlobais(competencia, request, usuarioLogin(authentication));
    }

    @PutMapping("/branch/{branchId}")
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public KpiGoalEffectiveDTO atualizarFilial(
            @PathVariable String branchId,
            @RequestParam(required = false) String competencia,
            @Valid @RequestBody KpiGoalsUpdateRequestDTO request,
            Authentication authentication
    ) {
        return service.atualizarMetasFilial(branchId, competencia, request, usuarioLogin(authentication));
    }

    @DeleteMapping("/branch/{branchId}")
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public KpiGoalEffectiveDTO removerOverrideFilial(
            @PathVariable String branchId,
            @RequestParam(required = false) String competencia,
            Authentication authentication
    ) {
        return service.removerOverrideFilial(branchId, competencia, usuarioLogin(authentication));
    }

    @GetMapping("/history")
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public List<KpiGoalHistoryDTO> historico(
            @RequestParam(required = false) String branchId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return service.buscarHistorico(branchId, limit);
    }

    @GetMapping("/history/page")
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public PaginaDTO<KpiGoalHistoryDTO> historicoPaginado(
            @RequestParam(required = false) String branchId,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanhoPagina
    ) {
        return service.buscarHistoricoPaginado(branchId, pagina, tamanhoPagina);
    }

    @GetMapping("/overrides")
    @PreAuthorize("@acessoSeguranca.podeAcessar('indicadoresGestaoAVista')")
    public KpiGoalOverridesByIndicatorDTO overrides(
            @RequestParam String indicatorKey,
            @RequestParam(required = false) String competencia
    ) {
        return service.buscarOverridesPorIndicador(indicatorKey, competencia);
    }

    @ExceptionHandler(KpiGoalOverrideConflictException.class)
    public ResponseEntity<KpiGoalConflictDTO> handleOverrideConflict(KpiGoalOverrideConflictException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new KpiGoalConflictDTO(ex.getMessage(), ex.getBranches()));
    }

    private String usuarioLogin(Authentication authentication) {
        return authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "";
    }
}
