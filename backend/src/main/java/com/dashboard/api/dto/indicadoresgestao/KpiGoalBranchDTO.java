package com.dashboard.api.dto.indicadoresgestao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record KpiGoalBranchDTO(
        String branchId,
        Map<String, BigDecimal> goals,
        Instant updatedAt,
        KpiGoalUserDTO updatedBy
) {
}
