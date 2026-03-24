package com.dashboard.api.dto.manifestos;

public record ManifestosTrendPointDTO(
    String date,
    int encerrado,
    int emTransito,
    int pendente
) {}
