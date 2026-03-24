package com.dashboard.api.dto.coletas;

import java.math.BigDecimal;

public record ColetasOverviewDTO(
    String updatedAt,
    int totalColetas,
    int finalizadas,
    double taxaSucesso,
    double taxaCancelamento,
    double slaNoAgendamento,
    double leadTimeMedioDias,
    double tentativasMedias,
    BigDecimal pesoTaxadoTotal,
    BigDecimal valorNfTotal
) {}
