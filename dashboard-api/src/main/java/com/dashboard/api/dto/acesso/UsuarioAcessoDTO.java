package com.dashboard.api.dto.acesso;

import java.util.List;
import java.util.Map;

public record UsuarioAcessoDTO(
        String id,
        String login,
        String nome,
        String email,
        boolean admin,
        boolean ativo,
        String setorId,
        String setorNome,
        Map<String, Boolean> permissoes,
        List<String> papeis
) {
}
