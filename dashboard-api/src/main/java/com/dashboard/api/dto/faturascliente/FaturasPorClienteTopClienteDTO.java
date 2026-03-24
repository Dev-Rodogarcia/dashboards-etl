package com.dashboard.api.dto.faturascliente;

import java.math.BigDecimal;

public record FaturasPorClienteTopClienteDTO(
        String cliente,
        BigDecimal valorFaturado
) {
}
