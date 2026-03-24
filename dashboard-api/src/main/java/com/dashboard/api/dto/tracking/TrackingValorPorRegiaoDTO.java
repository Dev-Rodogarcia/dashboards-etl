package com.dashboard.api.dto.tracking;

import java.math.BigDecimal;

public record TrackingValorPorRegiaoDTO(
        String regiaoDestino,
        BigDecimal valorFrete,
        int cargas
) {
}
