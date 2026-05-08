package com.dashboard.api.dto.indicadoresgestao;

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
        String observacao,
        String nomeArquivo,
        String importadoEm,
        String importadoPor
) {
}
