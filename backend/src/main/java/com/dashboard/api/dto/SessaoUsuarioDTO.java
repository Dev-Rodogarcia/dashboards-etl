package com.dashboard.api.dto;

import java.util.List;
import java.util.Map;

public record SessaoUsuarioDTO(
        String id,
        String nome,
        String email,
        String papel,
        SetorSessaoDTO setor,
        Map<String, Boolean> permissoesEfetivas,
        List<String> filiaisPermitidasEfetivas,
        boolean exigeTrocaSenha
) {
}
