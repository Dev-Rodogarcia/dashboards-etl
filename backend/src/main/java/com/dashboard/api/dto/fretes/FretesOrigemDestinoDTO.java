package com.dashboard.api.dto.fretes;

import java.math.BigDecimal;

public record FretesOrigemDestinoDTO(
        String origemUf,
        String destinoUf,
        BigDecimal receita,
        int fretes
) {
}
