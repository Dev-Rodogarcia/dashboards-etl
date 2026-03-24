package com.dashboard.api.dto.faturas;

import java.math.BigDecimal;

public record FaturasAgingBucketDTO(
        String faixa,
        BigDecimal valor,
        int titulos
) {}
