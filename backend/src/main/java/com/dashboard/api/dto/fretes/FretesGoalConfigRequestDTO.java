package com.dashboard.api.dto.fretes;

import java.math.BigDecimal;

public record FretesGoalConfigRequestDTO(
        String branchId,
        int ano,
        int mes,
        BigDecimal metaFaturamento
) {
}
