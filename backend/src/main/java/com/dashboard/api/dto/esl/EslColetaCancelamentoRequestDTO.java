package com.dashboard.api.dto.esl;

import jakarta.validation.constraints.NotNull;

public record EslColetaCancelamentoRequestDTO(
        @NotNull(message = "O motivo canônico do cancelamento é obrigatório")
        EslMotivoCancelamento motivo
) {
}
