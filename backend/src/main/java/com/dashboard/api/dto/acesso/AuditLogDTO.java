package com.dashboard.api.dto.acesso;

import java.time.Instant;

public record AuditLogDTO(
        Long id,
        Instant timestamp,
        String usuarioLogin,
        String acao,
        String recurso,
        String detalhesJson,
        String ipAddress
) {
}
