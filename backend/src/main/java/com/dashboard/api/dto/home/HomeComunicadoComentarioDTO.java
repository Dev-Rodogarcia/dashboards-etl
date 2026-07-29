package com.dashboard.api.dto.home;

import java.time.Instant;

public record HomeComunicadoComentarioDTO(
        String id,
        String autorNome,
        String corpo,
        Instant criadoEm,
        boolean podeExcluir
) {}
