package com.dashboard.api.dto.indicadoresgestao;

public record HorariosCorteImportacaoMensagemDTO(
        int linha,
        String linhaOuOperacao,
        String mensagem
) {
}
