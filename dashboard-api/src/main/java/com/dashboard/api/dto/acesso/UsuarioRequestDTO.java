package com.dashboard.api.dto.acesso;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UsuarioRequestDTO(
        @NotBlank(message = "O nome do usuário é obrigatório")
        String nome,
        @Email(message = "O e-mail informado é inválido")
        @NotBlank(message = "O e-mail do usuário é obrigatório")
        String email,
        String senha,
        String confirmacaoSenha,
        @NotBlank(message = "O setor do usuário é obrigatório")
        String setorId,
        @NotBlank(message = "O papel do usuário é obrigatório")
        String papel,
        @NotNull(message = "A lista de permissões negadas é obrigatória")
        List<String> permissoesNegadas,
        List<String> permissoesConcedidas,
        Boolean ativo
) {
}
