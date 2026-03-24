package com.dashboard.api.dto;

import java.util.List;

public record SessaoUsuarioDTO(
        String id,
        String login,
        String nome,
        String email,
        boolean admin,
        SetorSessaoDTO setor,
        List<String> papeis,
        boolean exigeTrocaSenha
) {
}
