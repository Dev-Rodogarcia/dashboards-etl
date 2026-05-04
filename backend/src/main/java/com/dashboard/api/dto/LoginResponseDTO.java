package com.dashboard.api.dto;

import java.time.Instant;

public record LoginResponseDTO(
        SessaoUsuarioDTO usuario,
        String token,
        boolean exigeTrocaSenha,
        Instant sessaoExpiraEm
) {
    public LoginResponseDTO(SessaoUsuarioDTO usuario, String token, boolean exigeTrocaSenha) {
        this(usuario, token, exigeTrocaSenha, null);
    }
}
