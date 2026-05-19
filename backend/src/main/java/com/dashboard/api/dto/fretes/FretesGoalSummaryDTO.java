package com.dashboard.api.dto.fretes;

import java.math.BigDecimal;
import java.util.List;

public record FretesGoalSummaryDTO(
        String dataInicio,
        String dataFim,
        BigDecimal metaFaturamento,
        BigDecimal realizadoFaturamento,
        double percentualAtingimentoFaturamento,
        List<FretesGoalBranchSummaryDTO> branches
) {
}
