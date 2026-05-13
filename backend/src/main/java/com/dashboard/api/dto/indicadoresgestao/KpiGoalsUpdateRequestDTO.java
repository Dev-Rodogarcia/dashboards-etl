package com.dashboard.api.dto.indicadoresgestao;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record KpiGoalsUpdateRequestDTO(
        @NotNull Map<String, BigDecimal> goals,
        Boolean forceOverride
) {
}
