package com.dashboard.api.dto.acesso;

public record UsuarioSessaoResumoDTO(
        long totalUsuarios,
        long usuariosAtivos,
        long usuariosInativos,
        long usuariosOnline
) {
}
