package com.dashboard.api.dto.acesso;

import java.util.List;
import java.util.Map;

public record SetorDTO(
        String id,
        String nome,
        String descricao,
        boolean sistema,
        int totalUsuarios,
        Map<String, Boolean> permissoes,
        List<String> filiaisPermitidas
) {
}
