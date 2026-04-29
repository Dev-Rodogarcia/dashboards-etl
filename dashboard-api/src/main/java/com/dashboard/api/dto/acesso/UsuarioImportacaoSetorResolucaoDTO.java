package com.dashboard.api.dto.acesso;

import jakarta.validation.constraints.NotBlank;

public record UsuarioImportacaoSetorResolucaoDTO(
        @NotBlank(message = "O setor original é obrigatório")
        String setorOriginal,
        @NotBlank(message = "O setor de destino é obrigatório")
        String setorDestinoId
) {
}
