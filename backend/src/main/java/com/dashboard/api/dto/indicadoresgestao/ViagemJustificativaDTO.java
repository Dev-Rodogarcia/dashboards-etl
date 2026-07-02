package com.dashboard.api.dto.indicadoresgestao;

import java.time.OffsetDateTime;

public record ViagemJustificativaDTO(
        Long id,
        Long codSolicitacao,
        String justificativa,
        OffsetDateTime criadoEm,
        String criadoPor
) {
}
