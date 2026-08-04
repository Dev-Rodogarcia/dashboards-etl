package com.dashboard.api.dto.contaspagar;

import java.math.BigDecimal;

public record ContasAPagarDrilldownPointDTO(
        String label,
        BigDecimal valor,
        int titulos
) {
}
