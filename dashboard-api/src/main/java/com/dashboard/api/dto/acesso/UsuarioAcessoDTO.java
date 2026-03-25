package com.dashboard.api.dto.acesso;

import java.util.List;
import java.util.Map;

public record UsuarioAcessoDTO(
        String id,
        String nome,
        String email,
        boolean ativo,
        String setorId,
        String setorNome,
        String papel,
        Map<String, Boolean> permissoesEfetivas,
        List<String> filiaisPermitidasEfetivas,
        List<String> permissoesNegadas,
        List<String> permissoesConcedidas
) {
}
