package com.dashboard.api.dto.manifestos;

import java.math.BigDecimal;

public record ManifestosCostGoalConfigRequestDTO(
        String branchId,
        String contractType,
        String contractTypeKey,
        int ano,
        int mes,
        BigDecimal costGoal
) {
}
