package com.dashboard.api.dto.indicadoresgestao;

public record HorariosCorteSeriePointDTO(
        String date,
        String filial,
        int saidasNoHorario,
        int totalProgramado,
        double pctNoHorario
) {
}
