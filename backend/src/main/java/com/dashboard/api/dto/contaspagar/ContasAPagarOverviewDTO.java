package com.dashboard.api.dto.contaspagar;

import java.math.BigDecimal;

public record ContasAPagarOverviewDTO(
    String updatedAt,
    BigDecimal valorAPagar,
    BigDecimal valorPago,
    BigDecimal saldoAberto,
    double taxaLiquidacao,
    double leadTimeLiquidacaoDias,
    double pctConciliado
) {}
