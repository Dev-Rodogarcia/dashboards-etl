package com.dashboard.api.dto.fretes;

import java.math.BigDecimal;

public record FretesTrendPointDTO(
    String date,
    BigDecimal receitaBruta,
    BigDecimal valorFrete,
    int fretes
) {}
