package com.dashboard.api.controller;

import com.dashboard.api.dto.indicadoresgestao.CubagemClientesImportacaoPreviewResponseDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemClientesImportacaoResultadoDTO;
import com.dashboard.api.service.CubagemClientesExcecaoImportacaoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/painel/gestao-vista/cubagem/clientes/importacao")
@PreAuthorize("@acessoSeguranca.podeAcessar('indicadoresGestaoAVista')")
public class CubagemClientesExcecaoImportacaoController {

    private final CubagemClientesExcecaoImportacaoService service;

    public CubagemClientesExcecaoImportacaoController(CubagemClientesExcecaoImportacaoService service) {
        this.service = service;
    }

    @PostMapping(path = "/pre-validacao", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CubagemClientesImportacaoPreviewResponseDTO> preValidacao(
            @RequestPart("arquivo") MultipartFile arquivo
    ) {
        return ResponseEntity.ok(service.preValidar(arquivo));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CubagemClientesImportacaoResultadoDTO> importar(
            @RequestPart("arquivo") MultipartFile arquivo,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.importar(arquivo, authenticationName(authentication)));
    }

    private String authenticationName(Authentication authentication) {
        return authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "";
    }
}
