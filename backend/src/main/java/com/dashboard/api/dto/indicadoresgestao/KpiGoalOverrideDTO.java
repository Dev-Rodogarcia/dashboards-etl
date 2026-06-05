package com.dashboard.api.dto.indicadoresgestao;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record KpiGoalOverrideDTO(
        String branchId,
        String branchName,
        LocalDate competencia,
        BigDecimal goalValue,
        Instant updatedAt,
        KpiGoalUserDTO updatedBy
) {
}
