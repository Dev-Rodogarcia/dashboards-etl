package com.dashboard.api.dto.faturas;

import java.math.BigDecimal;

public record FaturasOverviewDTO(
    String updatedAt,
    BigDecimal valorFaturado,
    BigDecimal valorRecebido,
    BigDecimal saldoAberto,
    double taxaAdimplencia,
    double dsoMedioDias,
    int titulosEmAtraso,
    int clientesAtivos,
    boolean hasFinancialData
) {}
