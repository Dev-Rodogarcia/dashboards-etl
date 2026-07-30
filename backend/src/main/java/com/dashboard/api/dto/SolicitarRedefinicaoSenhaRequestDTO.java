package com.dashboard.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitarRedefinicaoSenhaRequestDTO(
        @Email(message = "O e-mail informado é inválido")
        @NotBlank(message = "O campo 'email' é obrigatório")
        String email
) {
}
