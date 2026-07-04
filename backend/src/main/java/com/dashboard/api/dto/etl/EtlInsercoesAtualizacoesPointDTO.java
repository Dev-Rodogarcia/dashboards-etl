package com.dashboard.api.dto.etl;

import java.time.LocalDate;

public record EtlInsercoesAtualizacoesPointDTO(
        LocalDate dataReferencia,
        int insercoes,
        int atualizacoes
) {}
