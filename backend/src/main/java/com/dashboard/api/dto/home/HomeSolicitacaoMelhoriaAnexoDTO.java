package com.dashboard.api.dto.home;

public record HomeSolicitacaoMelhoriaAnexoDTO(
        Long id,
        String nomeOriginal,
        String tipoConteudo,
        long tamanhoBytes
) {
}
