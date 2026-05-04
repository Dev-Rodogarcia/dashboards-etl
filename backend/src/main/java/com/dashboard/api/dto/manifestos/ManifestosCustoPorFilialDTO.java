package com.dashboard.api.dto.manifestos;

import java.math.BigDecimal;

public record ManifestosCustoPorFilialDTO(
        String filial,
        BigDecimal custoTotal
) {
}
