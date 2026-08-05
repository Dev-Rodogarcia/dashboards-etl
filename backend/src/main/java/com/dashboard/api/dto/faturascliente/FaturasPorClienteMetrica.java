package com.dashboard.api.dto.faturascliente;

import java.util.Locale;

public enum FaturasPorClienteMetrica {
    VALOR_FATURADO,
    REGISTROS_FATURADOS,
    TICKET_MEDIO,
    VALOR_EM_ATRASO;

    public static FaturasPorClienteMetrica from(String valor) {
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            return VALOR_FATURADO;
        }
    }
}
