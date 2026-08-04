package com.dashboard.api.dto.contaspagar;

import java.util.Locale;

public enum ContasAPagarGranularidade {
    DIA,
    SEMANA,
    MES;

    public static ContasAPagarGranularidade from(String value) {
        if (value == null || value.isBlank()) {
            return MES;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "dia" -> DIA;
            case "semana" -> SEMANA;
            default -> MES;
        };
    }
}
