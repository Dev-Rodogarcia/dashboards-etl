package com.dashboard.api.dto.indicadoresgestao;

import java.math.BigDecimal;
import java.util.List;

public record KpiGoalOverridesByIndicatorDTO(
        String indicatorKey,
        BigDecimal globalGoal,
        List<KpiGoalOverrideDTO> overrides
) {
}
