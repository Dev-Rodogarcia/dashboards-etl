package com.dashboard.api.dto.indicadoresgestao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record KpiGoalsFullDTO(
        LocalDate competencia,
        Map<String, BigDecimal> global,
        List<KpiGoalBranchDTO> branches
) {
}
