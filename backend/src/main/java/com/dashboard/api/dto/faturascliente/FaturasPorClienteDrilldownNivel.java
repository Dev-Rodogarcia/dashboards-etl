package com.dashboard.api.dto.faturascliente;

import java.util.Locale;

public enum FaturasPorClienteDrilldownNivel {
    CLIENTE,
    CNPJ,
    FATURA;

    public static FaturasPorClienteDrilldownNivel from(String valor) {
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            return CLIENTE;
        }
    }
}
