package com.dashboard.api.dto;

public record LoginResponseDTO(
        SessaoUsuarioDTO usuario,
        String token,
        boolean exigeTrocaSenha
) {
}
