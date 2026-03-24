package com.dashboard.api.dto.coletas;

public record ColetasTrendPointDTO(
    String date,
    int total,
    int finalizadas,
    int canceladas,
    int emTratativa
) {}
