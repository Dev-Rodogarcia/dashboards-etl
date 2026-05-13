package com.dashboard.api.dto.indicadoresgestao;

import java.math.BigDecimal;
import java.time.Instant;

public record KpiGoalOverrideDTO(
        String branchId,
        String branchName,
        BigDecimal goalValue,
        Instant updatedAt,
        KpiGoalUserDTO updatedBy
) {
}
