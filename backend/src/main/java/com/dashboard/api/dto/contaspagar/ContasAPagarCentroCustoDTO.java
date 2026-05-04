package com.dashboard.api.dto.contaspagar;

import java.math.BigDecimal;

public record ContasAPagarCentroCustoDTO(
        String centroCusto,
        BigDecimal valor
) {
}
