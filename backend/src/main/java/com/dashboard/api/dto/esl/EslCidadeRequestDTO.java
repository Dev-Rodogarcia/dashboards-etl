package com.dashboard.api.dto.esl;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EslCidadeRequestDTO(
        @NotBlank(message = "A cidade é obrigatória")
        @Size(max = 120, message = "A cidade deve ter no máximo 120 caracteres")
        String nome,
        @NotBlank(message = "A UF é obrigatória")
        @Pattern(regexp = "[A-Za-z]{2}", message = "A UF deve ter duas letras")
        String uf
) {
}
