package com.dashboard.api.dto.fretes;

import java.math.BigDecimal;

public record FretesGoalConfigDTO(
        String branchId,
        int ano,
        int mes,
        BigDecimal metaFaturamento,
        String updatedAt,
        String updatedByName,
        boolean configurado,
        String mensagem
) {
}
