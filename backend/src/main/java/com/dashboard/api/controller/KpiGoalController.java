package com.dashboard.api.controller;

import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalConflictDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalEffectiveDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalHistoryDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalOverridesByIndicatorDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalsFullDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalsUpdateRequestDTO;
import com.dashboard.api.exception.KpiGoalOverrideConflictException;
import com.dashboard.api.service.KpiGoalService;
import com.dashboard.api.service.acesso.KpiGoalsSchemaInitializer;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/kpi-goals")
public class KpiGoalController {

    private final KpiGoalService service;
    private final KpiGoalsSchemaInitializer schemaInitializer;

    public KpiGoalController(KpiGoalService service, KpiGoalsSchemaInitializer schemaInitializer) {
        this.service = service;
        this.schemaInitializer = schemaInitializer;
    }

    @GetMapping
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public KpiGoalsFullDTO buscarCompletas() {
        schemaInitializer.garantirSchema();
        return service.buscarMetasCompletas();
    }

    @GetMapping("/effective")
    @PreAuthorize("@acessoSeguranca.podeAcessar('indicadoresGestaoAVista')")
    public KpiGoalEffectiveDTO buscarEfetiva(@RequestParam(required = false) String branchId) {
        return service.buscarMetaEfetiva(branchId);
    }

    @PutMapping("/global")
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public KpiGoalsFullDTO atualizarGlobal(
            @Valid @RequestBody KpiGoalsUpdateRequestDTO request,
            Authentication authentication
    ) {
        schemaInitializer.garantirSchema();
        return service.atualizarMetasGlobais(request, usuarioLogin(authentication));
    }

    @PutMapping("/branch/{branchId}")
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public KpiGoalEffectiveDTO atualizarFilial(
            @PathVariable String branchId,
            @Valid @RequestBody KpiGoalsUpdateRequestDTO request,
            Authentication authentication
    ) {
        schemaInitializer.garantirSchema();
        return service.atualizarMetasFilial(branchId, request, usuarioLogin(authentication));
    }

    @DeleteMapping("/branch/{branchId}")
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public KpiGoalEffectiveDTO removerOverrideFilial(
            @PathVariable String branchId,
            Authentication authentication
    ) {
        schemaInitializer.garantirSchema();
        return service.removerOverrideFilial(branchId, usuarioLogin(authentication));
    }

    @GetMapping("/history")
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public List<KpiGoalHistoryDTO> historico(
            @RequestParam(required = false) String branchId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        schemaInitializer.garantirSchema();
        return service.buscarHistorico(branchId, limit);
    }

    @GetMapping("/history/page")
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public PaginaDTO<KpiGoalHistoryDTO> historicoPaginado(
            @RequestParam(required = false) String branchId,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanhoPagina
    ) {
        schemaInitializer.garantirSchema();
        return service.buscarHistoricoPaginado(branchId, pagina, tamanhoPagina);
    }

    @GetMapping("/overrides")
    @PreAuthorize("@acessoSeguranca.podeAcessar('indicadoresGestaoAVista')")
    public KpiGoalOverridesByIndicatorDTO overrides(@RequestParam String indicatorKey) {
        return service.buscarOverridesPorIndicador(indicatorKey);
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
