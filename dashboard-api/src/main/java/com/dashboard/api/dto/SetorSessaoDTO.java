package com.dashboard.api.dto;

import java.util.Map;

public record SetorSessaoDTO(
        String id,
        String nome,
        Map<String, Boolean> permissoes
) {
}
