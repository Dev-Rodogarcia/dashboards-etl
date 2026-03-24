package com.dashboard.api.dto.etl;

public record EtlSaudeOverviewDTO(
    String updatedAt,
    double tempoMedioExecucaoSegundos,
    int execucoesComErro,
    int totalExecucoes,
    int volumeProcessadoTotal,
    double taxaSucesso
) {}
