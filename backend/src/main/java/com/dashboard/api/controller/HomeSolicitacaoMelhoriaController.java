package com.dashboard.api.controller;

import com.dashboard.api.dto.home.HomeSolicitacaoMelhoriaDTO;
import com.dashboard.api.dto.home.HomeSolicitacaoMelhoriaRequestDTO;
import com.dashboard.api.security.AcessoSeguranca;
import com.dashboard.api.service.HomeSolicitacaoMelhoriaService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/painel/home/solicitacoes")
public class HomeSolicitacaoMelhoriaController {

    private final HomeSolicitacaoMelhoriaService service;
    private final AcessoSeguranca acessoSeguranca;

    public HomeSolicitacaoMelhoriaController(HomeSolicitacaoMelhoriaService service, AcessoSeguranca acessoSeguranca) {
        this.service = service;
        this.acessoSeguranca = acessoSeguranca;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HomeSolicitacaoMelhoriaDTO> criar(
            @Valid @RequestPart("solicitacao") HomeSolicitacaoMelhoriaRequestDTO request,
            @RequestPart(name = "anexos", required = false) List<MultipartFile> anexos,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.criar(request, usuarioLogin(authentication), anexos));
    }

    @GetMapping
    @PreAuthorize("@acessoSeguranca.podeGerenciarHomeComunicados()")
    public List<HomeSolicitacaoMelhoriaDTO> listar() {
        return service.listarAtivas();
    }

    @GetMapping("/minhas")
    public List<HomeSolicitacaoMelhoriaDTO> listarMinhas(Authentication authentication) {
        return service.listarAtivasDoSolicitante(usuarioLogin(authentication));
    }

    @PatchMapping("/{id}/concluir")
    @PreAuthorize("@acessoSeguranca.podeGerenciarHomeComunicados()")
    public ResponseEntity<HomeSolicitacaoMelhoriaDTO> concluir(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(service.concluir(id, usuarioLogin(authentication)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@acessoSeguranca.podeGerenciarHomeComunicados()")
    public ResponseEntity<Void> arquivar(@PathVariable Long id, Authentication authentication) {
        service.arquivar(id, usuarioLogin(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/anexos/{anexoId}")
    public ResponseEntity<byte[]> baixarAnexo(
            @PathVariable Long id,
            @PathVariable Long anexoId,
            Authentication authentication
    ) {
        var anexo = service.baixarAnexo(id, anexoId, usuarioLogin(authentication), acessoSeguranca.podeGerenciarHomeComunicados());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(anexo.tipoConteudo()))
                .contentLength(anexo.conteudo().length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(anexo.nomeOriginal(), StandardCharsets.UTF_8).build().toString()
                )
                .body(anexo.conteudo());
    }

    private String usuarioLogin(Authentication authentication) {
        return authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "";
    }
}
