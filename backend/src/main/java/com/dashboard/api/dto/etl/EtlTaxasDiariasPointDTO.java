package com.dashboard.api.dto.etl;

import java.time.LocalDate;

public record EtlTaxasDiariasPointDTO(
        LocalDate dataReferencia,
        int qtdSucesso,
        int qtdFalha
) {}
