package com.dashboard.api.dto.indicadoresgestao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record KpiGoalOverridesByIndicatorDTO(
        String indicatorKey,
        LocalDate competencia,
        BigDecimal globalGoal,
        List<KpiGoalOverrideDTO> overrides
) {
}
