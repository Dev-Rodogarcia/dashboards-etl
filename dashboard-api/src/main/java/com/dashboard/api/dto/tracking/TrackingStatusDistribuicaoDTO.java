package com.dashboard.api.dto.tracking;

import java.math.BigDecimal;

public record TrackingStatusDistribuicaoDTO(
        String status,
        int total,
        BigDecimal valorFrete
) {
}
