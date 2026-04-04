package com.dashboard.api.dto.indicadoresgestao;

import java.util.List;

public record HorariosCorteImportacaoResultadoDTO(
        String arquivo,
        String importadoEm,
        int linhasProcessadas,
        int linhasImportadas,
        int linhasSubstituidas,
        int linhasIgnoradas,
        List<HorariosCorteImportacaoMensagemDTO> avisos,
        List<HorariosCorteImportacaoMensagemDTO> rejeicoes
) {
}
