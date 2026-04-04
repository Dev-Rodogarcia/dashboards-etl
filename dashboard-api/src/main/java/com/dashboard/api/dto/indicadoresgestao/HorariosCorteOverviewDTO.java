package com.dashboard.api.dto.indicadoresgestao;

public record HorariosCorteOverviewDTO(
        String updatedAt,
        int saidasNoHorario,
        int totalProgramado,
        double pctNoHorario,
        String ultimaImportacaoEm,
        String ultimaImportacaoArquivo
) {
}
