package com.dashboard.api.dto.home;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HomeComunicadoComentarioRequestDTO(@NotBlank @Size(max = 700) String corpo) {}
