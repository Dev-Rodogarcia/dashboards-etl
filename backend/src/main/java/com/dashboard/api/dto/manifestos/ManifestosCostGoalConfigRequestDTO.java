package com.dashboard.api.dto.manifestos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ManifestosCostGoalConfigRequestDTO(
        @Size(max = 120, message = "A filial da meta deve ter no máximo 120 caracteres")
        String branchId,
        @Size(max = 100, message = "O tipo de contrato deve ter no máximo 100 caracteres")
        String contractType,
        @Size(max = 100, message = "A chave do tipo de contrato deve ter no máximo 100 caracteres")
        String contractTypeKey,
        @Size(max = 120, message = "A classificação deve ter no máximo 120 caracteres")
        String classificationKey,
        @Min(value = 2000, message = "Ano da meta deve estar entre 2000 e 2100")
        @Max(value = 2100, message = "Ano da meta deve estar entre 2000 e 2100")
        int ano,
        @Min(value = 1, message = "Mês da meta deve estar entre 1 e 12")
        @Max(value = 12, message = "Mês da meta deve estar entre 1 e 12")
        int mes,
        @NotNull(message = "A meta mensal de custo é obrigatória")
        @DecimalMin(value = "0.00", message = "Meta mensal de custo não pode ser negativa")
        BigDecimal costGoal
) {
}
