package com.dashboard.api.dto.acesso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.util.List;

public record UsuarioImportacaoLoteRequestDTO(
        @NotBlank(message = "O identificador da importação é obrigatório")
        String importacaoId,
        @NotNull(message = "As resoluções de setor são obrigatórias")
        List<@Valid UsuarioImportacaoSetorResolucaoDTO> resolucoesSetor
) {
}
