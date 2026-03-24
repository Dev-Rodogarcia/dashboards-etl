package com.dashboard.api.dto.faturas;

import java.math.BigDecimal;

public record FaturasClienteTopDTO(
        String cliente,
        BigDecimal faturado,
        BigDecimal saldoAberto
) {}
