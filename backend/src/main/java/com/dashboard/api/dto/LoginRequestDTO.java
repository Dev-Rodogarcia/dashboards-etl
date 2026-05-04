package com.dashboard.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
    @Email(message = "O e-mail informado é inválido")
    @NotBlank(message = "O campo 'email' é obrigatório")
    String email,

    @NotBlank(message = "O campo 'senha' é obrigatório")
    String senha
) {}
