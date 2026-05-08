package com.dashboard.api.dto.home;

import java.time.Instant;

public record HomeComunicadoDTO(
        String id,
        String titulo,
        String corpo,
        String tag,
        String publicoAlvo,
        Instant publicadoEm,
        String atualizadoPor,
        Instant atualizadoEm
) {
}
