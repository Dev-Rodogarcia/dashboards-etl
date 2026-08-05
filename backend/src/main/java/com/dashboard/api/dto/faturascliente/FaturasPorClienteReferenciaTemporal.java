package com.dashboard.api.dto.faturascliente;

import java.util.Locale;

public enum FaturasPorClienteReferenciaTemporal {
    EMISSAO,
    VENCIMENTO,
    BAIXA;

    public static FaturasPorClienteReferenciaTemporal from(String valor) {
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            return EMISSAO;
        }
    }
}
