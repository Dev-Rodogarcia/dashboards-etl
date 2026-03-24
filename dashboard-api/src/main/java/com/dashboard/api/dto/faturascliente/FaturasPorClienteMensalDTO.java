package com.dashboard.api.dto.faturascliente;

import java.math.BigDecimal;

public record FaturasPorClienteMensalDTO(
        String month,
        BigDecimal valorFaturado,
        int registrosFaturados
) {
}
