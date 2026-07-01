package com.dashboard.api.dto.manifestos;

import com.dashboard.api.model.acesso.ManifestosCostGoalEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ManifestosCostGoalDTO(
        Long id,
        String branchId,
        LocalDate yearMonth,
        String contractType,
        String contractTypeKey,
        String classificationKey,
        BigDecimal costGoal,
        Instant createdAt,
        Instant updatedAt
) {
    public static ManifestosCostGoalDTO from(ManifestosCostGoalEntity entity) {
        return new ManifestosCostGoalDTO(
                entity.getId(),
                entity.getBranchId(),
                entity.getYearMonth(),
                entity.getContractType(),
                entity.getContractTypeKey(),
                entity.getClassificationKey(),
                entity.getCostGoal(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
