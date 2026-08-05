package com.dashboard.api.dto.faturascliente;

import java.math.BigDecimal;

public record FaturasPorClienteDrilldownPointDTO(
        String label,
        String detalhe,
        BigDecimal valor,
        int registros,
        double percentualAcumulado
) {
}
