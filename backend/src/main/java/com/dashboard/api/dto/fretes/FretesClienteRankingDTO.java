package com.dashboard.api.dto.fretes;

import java.math.BigDecimal;

public record FretesClienteRankingDTO(
    String cliente,
    BigDecimal receita,
    int fretes,
    BigDecimal ticketMedio
) {}
