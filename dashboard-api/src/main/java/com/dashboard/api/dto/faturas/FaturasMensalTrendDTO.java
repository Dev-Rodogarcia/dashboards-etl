package com.dashboard.api.dto.faturas;

import java.math.BigDecimal;

public record FaturasMensalTrendDTO(
        String month,
        BigDecimal faturado,
        BigDecimal pago,
        BigDecimal saldoAberto
) {}
