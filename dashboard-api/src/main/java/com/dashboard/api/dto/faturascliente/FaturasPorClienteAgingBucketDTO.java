package com.dashboard.api.dto.faturascliente;

import java.math.BigDecimal;

public record FaturasPorClienteAgingBucketDTO(
        String faixa,
        BigDecimal valor,
        int titulos
) {
}
