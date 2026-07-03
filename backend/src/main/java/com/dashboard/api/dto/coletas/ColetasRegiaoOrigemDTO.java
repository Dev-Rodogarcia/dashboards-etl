package com.dashboard.api.dto.coletas;

import java.math.BigDecimal;

public record ColetasRegiaoOrigemDTO(
        String regiaoLogistica,
        int totalColetas,
        BigDecimal pesoTaxado
) {
}
