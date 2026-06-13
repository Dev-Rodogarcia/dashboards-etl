package com.dashboard.api.dto.manifestos;

import com.dashboard.api.model.acesso.ManifestosCostGoalEntity;
import java.math.BigDecimal;
import java.time.Instant;

public record ManifestosCostGoalConfigDTO(
        String branchId,
        int ano,
        int mes,
        BigDecimal costGoal,
        Instant updatedAt,
        boolean configurado,
        String mensagem
) {
    public static ManifestosCostGoalConfigDTO from(ManifestosCostGoalEntity entity) {
        return new ManifestosCostGoalConfigDTO(
                entity.getBranchId() == null ? "GLOBAL" : entity.getBranchId(),
                entity.getYearMonth().getYear(),
                entity.getYearMonth().getMonthValue(),
                entity.getCostGoal(),
                entity.getUpdatedAt(),
                true,
                null
        );
    }

    public static ManifestosCostGoalConfigDTO fallback(int ano, int mes, String mensagem) {
        return new ManifestosCostGoalConfigDTO(
                "GLOBAL",
                ano,
                mes,
                BigDecimal.ZERO,
                null,
                false,
                mensagem
        );
    }
}
