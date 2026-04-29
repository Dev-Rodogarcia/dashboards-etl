package com.dashboard.api.dto.acesso;

public record UsuarioImportacaoCredencialTemporariaDTO(
        String email,
        String senhaProvisoria
) {
}
