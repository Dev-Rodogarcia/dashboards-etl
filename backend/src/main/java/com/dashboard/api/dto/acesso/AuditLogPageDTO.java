package com.dashboard.api.dto.acesso;

import java.util.List;

public record AuditLogPageDTO(
        List<AuditLogDTO> content,
        int totalPages,
        long totalElements
) {
}
