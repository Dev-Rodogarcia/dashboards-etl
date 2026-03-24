package com.dashboard.api.dto.etl;

public record EtlExecucaoTrendPointDTO(
        String date,
        int execucoes,
        int erros,
        int volumeProcessado,
        double duracaoMedia
) {}
