package com.dashboard.api.dto.manifestos;

import java.math.BigDecimal;

public record ManifestosComposicaoCustoDTO(
        String categoria,
        BigDecimal valor
) {
}
