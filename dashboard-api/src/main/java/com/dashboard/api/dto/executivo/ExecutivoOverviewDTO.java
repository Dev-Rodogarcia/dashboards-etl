package com.dashboard.api.dto.executivo;

import java.math.BigDecimal;

public record ExecutivoOverviewDTO(
    String updatedAt,
    BigDecimal receitaOperacional,
    BigDecimal valorFaturado,
    BigDecimal saldoAReceber,
    BigDecimal saldoAPagar,
    int backlogColetas,
    int cargasPrevisaoVencida,
    double ocupacaoMediaManifestos
) {}
