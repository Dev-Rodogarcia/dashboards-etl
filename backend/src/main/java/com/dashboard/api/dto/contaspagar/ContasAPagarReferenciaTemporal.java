package com.dashboard.api.dto.contaspagar;

import java.util.Locale;

public enum ContasAPagarReferenciaTemporal {
    EMISSAO,
    COMPETENCIA,
    LIQUIDACAO;

    public static ContasAPagarReferenciaTemporal from(String value) {
        if (value == null || value.isBlank()) {
            return EMISSAO;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "competencia" -> COMPETENCIA;
            case "liquidacao" -> LIQUIDACAO;
            default -> EMISSAO;
        };
    }
}
