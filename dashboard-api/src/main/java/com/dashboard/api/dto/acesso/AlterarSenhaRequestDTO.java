package com.dashboard.api.dto.acesso;

import jakarta.validation.constraints.NotBlank;

public record AlterarSenhaRequestDTO(
        @NotBlank(message = "A senha atual é obrigatória")
        String senhaAtual,
        @NotBlank(message = "A nova senha é obrigatória")
        String novaSenha
) {
}
