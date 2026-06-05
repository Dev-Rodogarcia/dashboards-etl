package com.dashboard.api.dto.indicadoresgestao;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record KpiGoalBranchDTO(
        String branchId,
        LocalDate competencia,
        Map<String, BigDecimal> goals,
        Instant updatedAt,
        KpiGoalUserDTO updatedBy
) {
}
