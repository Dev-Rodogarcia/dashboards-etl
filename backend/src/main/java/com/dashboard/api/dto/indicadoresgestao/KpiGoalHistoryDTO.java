package com.dashboard.api.dto.indicadoresgestao;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record KpiGoalHistoryDTO(
        String branchId,
        String indicatorKey,
        LocalDate competencia,
        BigDecimal oldValue,
        BigDecimal newValue,
        KpiGoalUserDTO updatedBy,
        Instant updatedAt,
        String action
) {
}
