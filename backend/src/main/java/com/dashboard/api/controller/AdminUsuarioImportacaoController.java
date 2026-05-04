package com.dashboard.api.controller;

import com.dashboard.api.dto.acesso.UsuarioImportacaoLoteRequestDTO;
import com.dashboard.api.dto.acesso.UsuarioImportacaoPreValidacaoResponseDTO;
import com.dashboard.api.dto.acesso.UsuarioImportacaoResultadoDTO;
import com.dashboard.api.service.acesso.UsuarioImportacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/acesso/usuarios/importacao")
@PreAuthorize("@acessoSeguranca.ehAdmin()")
public class AdminUsuarioImportacaoController {

    private final UsuarioImportacaoService usuarioImportacaoService;

    public AdminUsuarioImportacaoController(UsuarioImportacaoService usuarioImportacaoService) {
        this.usuarioImportacaoService = usuarioImportacaoService;
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("usuarios-importacao-modelo.xlsx")
                                .build()
                                .toString()
                )
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(usuarioImportacaoService.gerarTemplateExcel());
    }

    @PostMapping(path = "/pre-validacao", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UsuarioImportacaoPreValidacaoResponseDTO> preValidacao(@RequestPart("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(usuarioImportacaoService.preValidar(arquivo));
    }

    @PostMapping("/revalidacao")
    public ResponseEntity<UsuarioImportacaoPreValidacaoResponseDTO> revalidacao(
            @Valid @RequestBody UsuarioImportacaoLoteRequestDTO request
    ) {
        return ResponseEntity.ok(usuarioImportacaoService.revalidar(request));
    }

    @PostMapping
    public ResponseEntity<UsuarioImportacaoResultadoDTO> importar(
            @Valid @RequestBody UsuarioImportacaoLoteRequestDTO request
    ) {
        return ResponseEntity.ok(usuarioImportacaoService.importar(request));
    }
}
