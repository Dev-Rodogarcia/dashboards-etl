package com.dashboard.api.dto.home;

import java.time.Instant;
import java.util.List;

public record HomeSolicitacaoMelhoriaDTO(
        String id,
        String tipo,
        String titulo,
        String descricao,
        String resultadoEsperado,
        String localAplicacao,
        String status,
        String solicitanteNome,
        String solicitanteEmail,
        Instant criadoEm,
        Instant concluidoEm,
        String atualizadoPor,
        List<HomeSolicitacaoMelhoriaAnexoDTO> anexos
) {
}
