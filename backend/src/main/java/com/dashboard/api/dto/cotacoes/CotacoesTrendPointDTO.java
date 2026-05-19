package com.dashboard.api.dto.cotacoes;

import java.math.BigDecimal;

public record CotacoesTrendPointDTO(
        String date,
        int cotacoes,
        int convertidas,
        int reprovadas,
        BigDecimal valorPotencial,
        BigDecimal valorConvertido
) {}
