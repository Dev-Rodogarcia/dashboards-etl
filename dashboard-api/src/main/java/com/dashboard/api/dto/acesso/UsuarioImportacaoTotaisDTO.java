package com.dashboard.api.dto.acesso;

public record UsuarioImportacaoTotaisDTO(
        int totalLinhas,
        int validas,
        int invalidas
) {
}
