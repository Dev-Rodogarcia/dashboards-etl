package com.dashboard.api.dto.contaspagar;

import java.util.Locale;

public enum ContasAPagarMetrica {
    VALOR_A_PAGAR,
    SALDO_ABERTO,
    VALOR_PAGO,
    TITULOS;

    public static ContasAPagarMetrica from(String value) {
        if (value == null || value.isBlank()) {
            return VALOR_A_PAGAR;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "saldoaberto" -> SALDO_ABERTO;
            case "valorpago" -> VALOR_PAGO;
            case "titulos" -> TITULOS;
            default -> VALOR_A_PAGAR;
        };
    }
}
