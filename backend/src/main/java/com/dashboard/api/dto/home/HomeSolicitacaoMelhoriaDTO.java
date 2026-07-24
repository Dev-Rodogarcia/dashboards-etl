package com.dashboard.api.dto.home;

import java.time.Instant;

public record HomeSolicitacaoMelhoriaDTO(
        String id,
        String tipo,
        String titulo,
        String descricao,
        String resultadoEsperado,
        String status,
        String solicitanteNome,
        String solicitanteEmail,
        Instant criadoEm,
        Instant concluidoEm,
        String atualizadoPor
) {
}
