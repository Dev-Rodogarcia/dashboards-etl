package com.dashboard.api.dto.fretes;

import java.math.BigDecimal;

public record FretesOverviewDTO(
    String updatedAt,
    int totalFretes,
    BigDecimal receitaBruta,
    BigDecimal valorFrete,
    BigDecimal ticketMedio,
    BigDecimal pesoTaxadoTotal,
    int volumesTotais,
    double pctCteEmitido,
    double pctNfseEmitida,
    int fretesPrevisaoVencida
) {}
