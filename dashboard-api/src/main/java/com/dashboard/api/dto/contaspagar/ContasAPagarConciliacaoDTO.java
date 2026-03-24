package com.dashboard.api.dto.contaspagar;

import java.math.BigDecimal;

public record ContasAPagarConciliacaoDTO(
        String status,
        int total,
        BigDecimal valor
) {
}
