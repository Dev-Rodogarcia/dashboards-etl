package com.dashboard.api.dto.contaspagar;

import java.math.BigDecimal;

public record ContasAPagarMensalTrendDTO(
        String month,
        BigDecimal pago,
        BigDecimal aberto
) {}
