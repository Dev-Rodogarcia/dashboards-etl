package com.dashboard.api.dto.acesso;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioRequestDTO(
        @NotBlank(message = "O login do usuário é obrigatório")
        String login,
        @NotBlank(message = "O nome do usuário é obrigatório")
        String nome,
        @Email(message = "O e-mail informado é inválido")
        @NotBlank(message = "O e-mail do usuário é obrigatório")
        String email,
        String senha,
        @NotBlank(message = "O setor do usuário é obrigatório")
        String setorId,
        Boolean admin,
        Boolean ativo
) {
}
