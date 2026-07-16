package com.dashboard.api.dto.acesso;

import jakarta.validation.constraints.NotBlank;

public record RedefinirSenhaUsuarioRequestDTO(
        @NotBlank(message = "O campo 'senhaTemporaria' é obrigatório")
        String senhaTemporaria
) {
}
