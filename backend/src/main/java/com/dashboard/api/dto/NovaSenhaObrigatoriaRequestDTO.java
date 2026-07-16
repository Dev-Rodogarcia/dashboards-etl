package com.dashboard.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NovaSenhaObrigatoriaRequestDTO(
        @Email(message = "O e-mail informado é inválido")
        @NotBlank(message = "O campo 'email' é obrigatório")
        String email,

        @NotBlank(message = "O campo 'senhaTemporaria' é obrigatório")
        String senhaTemporaria,

        @NotBlank(message = "O campo 'novaSenha' é obrigatório")
        String novaSenha
) {
}
