package com.dashboard.api.dto.contaspagar;

import java.util.Locale;

public enum ContasAPagarDrilldownNivel {
    RAIZ,
    CLASSIFICACAO,
    DESPESA;

    public static ContasAPagarDrilldownNivel from(String value) {
        if (value == null || value.isBlank()) {
            return RAIZ;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "classificacao" -> CLASSIFICACAO;
            case "despesa" -> DESPESA;
            default -> RAIZ;
        };
    }
}
