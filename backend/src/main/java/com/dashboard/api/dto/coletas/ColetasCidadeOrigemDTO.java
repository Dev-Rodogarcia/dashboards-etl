package com.dashboard.api.dto.coletas;

import java.math.BigDecimal;

public record ColetasCidadeOrigemDTO(
        String cidade,
        int totalColetas,
        BigDecimal pesoTaxado
) {
}
