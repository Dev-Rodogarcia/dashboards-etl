package com.dashboard.api.dto.fretes;

import java.math.BigDecimal;

public record FretesGoalBranchSummaryDTO(
        String branchId,
        BigDecimal metaFaturamento,
        BigDecimal realizadoFaturamento,
        double percentualAtingimentoFaturamento
) {
}
