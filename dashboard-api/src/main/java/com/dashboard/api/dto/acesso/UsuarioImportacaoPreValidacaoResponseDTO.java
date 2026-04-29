package com.dashboard.api.dto.acesso;

import java.util.List;

public record UsuarioImportacaoPreValidacaoResponseDTO(
        String importacaoId,
        String arquivo,
        UsuarioImportacaoTotaisDTO totais,
        List<String> setoresInexistentes,
        boolean podeImportar,
        List<UsuarioImportacaoPreviewLinhaDTO> linhasPreview
) {
}
