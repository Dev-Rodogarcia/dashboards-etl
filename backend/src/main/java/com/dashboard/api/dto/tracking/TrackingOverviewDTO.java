package com.dashboard.api.dto.tracking;

import java.math.BigDecimal;

public record TrackingOverviewDTO(
    String updatedAt,
    int totalCargas,
    int emTransito,
    int previsaoVencida,
    BigDecimal valorFreteEmCarteira,
    BigDecimal pesoTaxadoTotal,
    double pctFinalizado
) {}
