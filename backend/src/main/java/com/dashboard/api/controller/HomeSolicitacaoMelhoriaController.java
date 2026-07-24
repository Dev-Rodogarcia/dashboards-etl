package com.dashboard.api.controller;

import com.dashboard.api.dto.home.HomeSolicitacaoMelhoriaDTO;
import com.dashboard.api.dto.home.HomeSolicitacaoMelhoriaRequestDTO;
import com.dashboard.api.service.HomeSolicitacaoMelhoriaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/painel/home/solicitacoes")
public class HomeSolicitacaoMelhoriaController {

    private final HomeSolicitacaoMelhoriaService service;

    public HomeSolicitacaoMelhoriaController(HomeSolicitacaoMelhoriaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<HomeSolicitacaoMelhoriaDTO> criar(
            @Valid @RequestBody HomeSolicitacaoMelhoriaRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.criar(request, usuarioLogin(authentication)));
    }

    @GetMapping
    @PreAuthorize("@acessoSeguranca.podeGerenciarHomeComunicados()")
    public List<HomeSolicitacaoMelhoriaDTO> listar() {
        return service.listarAtivas();
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

    private String usuarioLogin(Authentication authentication) {
        return authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "";
    }
}
