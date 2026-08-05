package com.dashboard.api.dto.faturascliente;

import java.util.Locale;

public enum FaturasPorClienteAgingEscopo {
    TODOS,
    A_VENCER,
    EM_ATRASO;

    public static FaturasPorClienteAgingEscopo from(String valor) {
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            return TODOS;
        }
    }
}
