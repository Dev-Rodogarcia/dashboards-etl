package com.dashboard.api.dto.acesso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record SetorRequestDTO(
        @NotBlank(message = "O nome do setor é obrigatório")
        String nome,
        String descricao,
        @NotNull(message = "As permissões do setor são obrigatórias")
        Map<String, Boolean> permissoes,
        @NotNull(message = "As filiais permitidas do setor são obrigatórias")
        @Size(min = 1, message = "Informe ao menos uma filial permitida para o setor")
        List<String> filiaisPermitidas
) {
}
