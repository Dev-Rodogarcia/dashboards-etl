package com.dashboard.api.dto.home;

public record HomeSolicitacaoMelhoriaAnexoConteudoDTO(
        String nomeOriginal,
        String tipoConteudo,
        byte[] conteudo
) {
}
