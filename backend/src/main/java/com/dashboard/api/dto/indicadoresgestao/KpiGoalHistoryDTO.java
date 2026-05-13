package com.dashboard.api.dto.indicadoresgestao;

import java.math.BigDecimal;
import java.time.Instant;

public record KpiGoalHistoryDTO(
        String branchId,
        String indicatorKey,
        BigDecimal oldValue,
        BigDecimal newValue,
        KpiGoalUserDTO updatedBy,
        Instant updatedAt,
        String action
) {
}
