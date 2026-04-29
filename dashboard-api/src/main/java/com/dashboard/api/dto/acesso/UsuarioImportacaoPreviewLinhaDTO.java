package com.dashboard.api.dto.acesso;

import java.util.List;

public record UsuarioImportacaoPreviewLinhaDTO(
        int linha,
        String nome,
        String email,
        String setorOriginal,
        String setorResolvido,
        String status,
        List<String> mensagens
) {
}
