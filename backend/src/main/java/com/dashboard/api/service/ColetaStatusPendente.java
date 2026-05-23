package com.dashboard.api.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

enum ColetaStatusPendente {
    PENDENTE("Pendente"),
    EM_ABERTO("Em aberto");

    private final String rotulo;

    ColetaStatusPendente(String rotulo) {
        this.rotulo = rotulo;
    }

    static List<String> normalizados() {
        return Arrays.stream(values())
                .map(status -> status.rotulo.trim().toLowerCase(Locale.ROOT))
                .toList();
    }
}
