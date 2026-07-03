package com.dashboard.api.dto.manifestos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ManifestosGoalReplicarRequestDTO(
        @Min(value = 2000, message = "Ano da meta deve estar entre 2000 e 2100")
        @Max(value = 2100, message = "Ano da meta deve estar entre 2000 e 2100")
        int ano,
        @Min(value = 1, message = "Mês da meta deve estar entre 1 e 12")
        @Max(value = 12, message = "Mês da meta deve estar entre 1 e 12")
        int mes
) {
}
