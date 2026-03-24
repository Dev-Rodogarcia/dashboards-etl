package com.dashboard.api.dto;

import java.time.LocalDate;

public record FiltroPeriodoDTO(
    LocalDate dataInicio,
    LocalDate dataFim
) {}
