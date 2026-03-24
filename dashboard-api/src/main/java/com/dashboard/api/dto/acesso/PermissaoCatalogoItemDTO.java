package com.dashboard.api.dto.acesso;

public record PermissaoCatalogoItemDTO(
        String chave,
        String nome,
        String descricao,
        String rota
) {
}
