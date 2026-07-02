package com.dashboard.api.dto.indicadoresgestao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ViagemJustificativaRequestDTO(
        @NotNull(message = "O codigo da solicitacao e obrigatorio")
        @Positive(message = "O codigo da solicitacao deve ser positivo")
        Long codSolicitacao,

        @NotBlank(message = "A justificativa e obrigatoria")
        @Size(max = 1000, message = "A justificativa deve ter no maximo 1000 caracteres")
        String justificativa
) {
}
