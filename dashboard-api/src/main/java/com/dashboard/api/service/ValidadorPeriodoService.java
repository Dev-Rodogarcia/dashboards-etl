package com.dashboard.api.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class ValidadorPeriodoService {

    private static final long PERIODO_MAXIMO_DIAS = 365;

    public void validar(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException("As datas de início e fim são obrigatórias");
        }

        if (dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("A data de início não pode ser posterior à data de fim");
        }

        long dias = ChronoUnit.DAYS.between(dataInicio, dataFim);
        if (dias > PERIODO_MAXIMO_DIAS) {
            throw new IllegalArgumentException(
                    "O período máximo de consulta é de " + PERIODO_MAXIMO_DIAS + " dias. Período solicitado: " + dias + " dias");
        }
    }
}
