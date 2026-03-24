package com.dashboard.api.dto.manifestos;

import java.math.BigDecimal;

public record ManifestosOcupacaoScatterDTO(
        BigDecimal pesoTaxado,
        BigDecimal totalM3,
        BigDecimal custoTotal
) {
}
