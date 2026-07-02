package com.dashboard.api.controller;

import com.dashboard.api.dto.manifestos.ManifestosCostGoalConfigDTO;
import com.dashboard.api.dto.manifestos.ManifestosCostGoalConfigRequestDTO;
import com.dashboard.api.dto.manifestos.ManifestosGoalReplicarRequestDTO;
import com.dashboard.api.dto.manifestos.ManifestosMetasImportacaoPreviewResponseDTO;
import com.dashboard.api.dto.manifestos.ManifestosMetasImportacaoResultadoDTO;
import com.dashboard.api.service.ManifestosCostGoalService;
import com.dashboard.api.service.ManifestosMetasImportacaoService;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/painel/manifestos/metas")
@PreAuthorize("@acessoSeguranca.podeAcessar('manifestos')")
public class ManifestosCostGoalController {

    private final ManifestosCostGoalService service;
    private final ManifestosMetasImportacaoService importacaoService;

    public ManifestosCostGoalController(
            ManifestosCostGoalService service,
            ManifestosMetasImportacaoService importacaoService
    ) {
        this.service = service;
        this.importacaoService = importacaoService;
    }

    @GetMapping
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public ResponseEntity<List<ManifestosCostGoalConfigDTO>> listar(
            @RequestParam int ano,
            @RequestParam int mes
    ) {
        return ResponseEntity.ok(service.listar(ano, mes));
    }

    @PostMapping
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public ResponseEntity<ManifestosCostGoalConfigDTO> salvar(
            @RequestBody ManifestosCostGoalConfigRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.salvar(request, authenticationName(authentication)));
    }

    @PostMapping("/replicar")
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public ResponseEntity<List<ManifestosCostGoalConfigDTO>> replicar(
            @RequestBody ManifestosGoalReplicarRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.replicar(
                request.ano(),
                request.mes(),
                authenticationName(authentication)
        ));
    }

    @DeleteMapping
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public ResponseEntity<Void> remover(
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) String contractTypeKey,
            @RequestParam(required = false) String classificationKey,
            @RequestParam int ano,
            @RequestParam int mes
    ) {
        service.remover(branchId, contractTypeKey, classificationKey, ano, mes);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/importacao/template")
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public ResponseEntity<byte[]> templateImportacao() {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("manifestos-metas-modelo.xlsx")
                                .build()
                                .toString()
                )
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(importacaoService.gerarTemplateExcel());
    }

    @PostMapping(path = "/importacao/pre-validacao", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public ResponseEntity<ManifestosMetasImportacaoPreviewResponseDTO> preValidacaoImportacao(
            @RequestPart("arquivo") MultipartFile arquivo
    ) {
        return ResponseEntity.ok(importacaoService.preValidar(arquivo));
    }

    @PostMapping(path = "/importacao", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@acessoSeguranca.podeGerenciarKpiGoals()")
    public ResponseEntity<ManifestosMetasImportacaoResultadoDTO> importar(
            @RequestPart("arquivo") MultipartFile arquivo,
            Authentication authentication
    ) {
        return ResponseEntity.ok(importacaoService.importar(arquivo, authenticationName(authentication)));
    }

    private String authenticationName(Authentication authentication) {
        return authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "";
    }
}
