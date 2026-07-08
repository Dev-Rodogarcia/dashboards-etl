package com.dashboard.api.dto.acesso;

import java.util.List;

public record UsuarioSessaoResumoDTO(
        long totalUsuarios,
        long usuariosAtivos,
        long usuariosInativos,
        long usuariosOnline,
        List<UsuarioOnlineResumoDTO> usuariosOnlineDetalhes
) {
    public UsuarioSessaoResumoDTO {
        usuariosOnlineDetalhes = usuariosOnlineDetalhes == null ? List.of() : List.copyOf(usuariosOnlineDetalhes);
    }

    public UsuarioSessaoResumoDTO(
            long totalUsuarios,
            long usuariosAtivos,
            long usuariosInativos,
            long usuariosOnline
    ) {
        this(totalUsuarios, usuariosAtivos, usuariosInativos, usuariosOnline, List.of());
    }
}
