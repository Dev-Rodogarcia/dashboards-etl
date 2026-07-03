package com.dashboard.api.dto.indicadoresgestao;

import com.dashboard.api.util.CsvColumn;

public record HorarioCorteRowDTO(
        long id,
        String data,
        String filial,
        String linhaOuOperacao,
        String origemSm,
        String destinoSm,
        String origemDestino,
        String origem,
        String ordem,
        String destino,
        String horarioCorteSm,
        String previsaoChegadaDestino,
        String transitTime,
        String inicio,
        String manifestado,
        String smGerada,
        String corte,
        String saidaEfetiva,
        String horarioCorte,
        Boolean saiuNoHorario,
        Integer atrasoMinutos,
        @CsvColumn("Justificativa")
        String justificativa,
        String observacao,
        String nomeArquivo,
        String importadoEm,
        String importadoPor
) {
}
