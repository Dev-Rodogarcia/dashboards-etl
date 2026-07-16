package com.dashboard.api.controller;

import com.dashboard.api.dto.acesso.RedefinirSenhaUsuarioRequestDTO;
import com.dashboard.api.service.acesso.GestaoUsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsuarioSenhaController {

    private final GestaoUsuarioService gestaoUsuarioService;

    public AdminUsuarioSenhaController(GestaoUsuarioService gestaoUsuarioService) {
        this.gestaoUsuarioService = gestaoUsuarioService;
    }

    @PostMapping("/{usuarioId}/reset-senha")
    public ResponseEntity<Void> resetSenha(
            @PathVariable Long usuarioId,
            @Valid @RequestBody RedefinirSenhaUsuarioRequestDTO request
    ) {
        gestaoUsuarioService.redefinirSenhaPorAdmin(usuarioId, request.senhaTemporaria());
        return ResponseEntity.noContent().build();
    }
}
