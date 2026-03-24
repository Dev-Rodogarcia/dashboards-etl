package com.dashboard.api.dto.executivo;

import java.math.BigDecimal;

public record ExecutivoTrendPointDTO(
        String month,
        BigDecimal receitaOperacional,
        BigDecimal valorFaturado,
        BigDecimal saldoAReceber,
        BigDecimal saldoAPagar,
        int backlogColetas
) {}
