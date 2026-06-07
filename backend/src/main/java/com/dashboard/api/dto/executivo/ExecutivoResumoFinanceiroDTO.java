package com.dashboard.api.dto.executivo;

import java.math.BigDecimal;

public record ExecutivoResumoFinanceiroDTO(
        String filial,
        BigDecimal totalFaturado,
        BigDecimal fretePeso,
        BigDecimal freteValor,
        BigDecimal ticketMedio
) {}
