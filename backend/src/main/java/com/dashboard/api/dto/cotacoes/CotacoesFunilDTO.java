package com.dashboard.api.dto.cotacoes;

import java.math.BigDecimal;

public record CotacoesFunilDTO(
        String etapa,
        int total,
        BigDecimal valor
) {
}
