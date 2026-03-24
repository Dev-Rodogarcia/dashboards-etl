package com.dashboard.api.dto.acesso;

public record PapelDTO(
        Long id,
        String nome,
        String descricao,
        int nivel
) {
}
