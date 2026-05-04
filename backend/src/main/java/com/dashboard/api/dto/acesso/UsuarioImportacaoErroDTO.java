package com.dashboard.api.dto.acesso;

public record UsuarioImportacaoErroDTO(
        int linha,
        String email,
        String motivo,
        String tipoErro
) {
}
