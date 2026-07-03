package com.dashboard.api.dto.fretes;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FretesGoalConfigRequestDTO(
        @Size(max = 120, message = "A filial da meta deve ter no máximo 120 caracteres")
        String branchId,
        @Min(value = 2000, message = "Ano da meta deve estar entre 2000 e 2100")
        @Max(value = 2100, message = "Ano da meta deve estar entre 2000 e 2100")
        int ano,
        @Min(value = 1, message = "Mês da meta deve estar entre 1 e 12")
        @Max(value = 12, message = "Mês da meta deve estar entre 1 e 12")
        int mes,
        @NotNull(message = "A meta de faturamento é obrigatória")
        @DecimalMin(value = "0.00", message = "Meta de faturamento não pode ser negativa")
        BigDecimal metaFaturamento
) {
}
