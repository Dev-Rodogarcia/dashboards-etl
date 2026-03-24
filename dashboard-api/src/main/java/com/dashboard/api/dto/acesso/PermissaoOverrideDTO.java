package com.dashboard.api.dto.acesso;

import jakarta.validation.constraints.NotBlank;

public record PermissaoOverrideDTO(
        @NotBlank String permissaoChave,
        @NotBlank String tipo
) {
}
