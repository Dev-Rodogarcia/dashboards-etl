package com.dashboard.api.dto.coletas;

import java.math.BigDecimal;

public record ColetasRegiaoOrigemDTO(
        String regiao,
        int totalColetas,
        BigDecimal pesoTaxado
) {
}
