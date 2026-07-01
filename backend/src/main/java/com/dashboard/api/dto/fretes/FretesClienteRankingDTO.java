package com.dashboard.api.dto.fretes;

import java.math.BigDecimal;

public record FretesClienteRankingDTO(
    String cliente,
    String cnpjBase,
    BigDecimal receita,
    int fretes,
    BigDecimal ticketMedio
) {}
