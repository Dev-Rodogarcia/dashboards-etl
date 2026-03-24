package com.dashboard.api.dto.acesso;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AtribuirPapeisRequestDTO(
        @NotNull List<Long> papelIds
) {
}
