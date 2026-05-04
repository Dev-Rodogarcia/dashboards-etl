package com.dashboard.api.dto.manifestos;

import java.math.BigDecimal;

public record ManifestosOverviewDTO(
    String updatedAt,
    int totalManifestos,
    int emTransito,
    int encerrados,
    BigDecimal kmTotal,
    BigDecimal custoTotal,
    BigDecimal custoPorKm,
    double ocupacaoPesoMediaPct,
    double ocupacaoCubagemMediaPct
) {}
