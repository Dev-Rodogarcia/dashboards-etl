package com.dashboard.api.dto.coletas;

import java.time.LocalDate;
import java.util.Locale;

public enum ColetasHistoricoPeriodo {
    DIAS("dias", 0, false),
    TRES_MESES("3meses", 3, true),
    SEIS_MESES("6meses", 6, true),
    UM_ANO("1ano", 12, true);

    private final String valor;
    private final int meses;
    private final boolean mensal;

    ColetasHistoricoPeriodo(String valor, int meses, boolean mensal) {
        this.valor = valor;
        this.meses = meses;
        this.mensal = mensal;
    }

    public static ColetasHistoricoPeriodo from(String valor) {
        if (valor == null || valor.isBlank()) {
            return DIAS;
        }

        String normalizado = valor.trim().toLowerCase(Locale.ROOT);
        for (ColetasHistoricoPeriodo periodo : values()) {
            if (periodo.valor.equals(normalizado)) {
                return periodo;
            }
        }
        return DIAS;
    }

    public boolean mensal() {
        return mensal;
    }

    public boolean usaJanelaRelativa() {
        return meses > 0;
    }

    public LocalDate inicioJanela(LocalDate dataReferencia) {
        if (!usaJanelaRelativa()) {
            return null;
        }
        return dataReferencia.withDayOfMonth(1).minusMonths(meses - 1L);
    }
}
