package com.dashboard.api.dto.indicadoresgestao;

import java.math.BigDecimal;
import java.util.Map;

public record KpiGoalEffectiveDTO(
        String branchId,
        String source,
        Map<String, BigDecimal> goals
) {
}
