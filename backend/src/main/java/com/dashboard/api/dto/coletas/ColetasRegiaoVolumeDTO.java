package com.dashboard.api.dto.coletas;

import java.math.BigDecimal;

public record ColetasRegiaoVolumeDTO(
        String regiao,
        int totalColetas,
        BigDecimal pesoTaxado,
        int volumes
) {
}
