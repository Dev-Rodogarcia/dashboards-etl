package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByTimestampUtcDesc(Pageable pageable);
    Page<AuditLog> findByAcaoOrderByTimestampUtcDesc(String acao, Pageable pageable);
    Page<AuditLog> findByUsuarioIdOrderByTimestampUtcDesc(Long usuarioId, Pageable pageable);
    Page<AuditLog> findByAcaoAndUsuarioIdOrderByTimestampUtcDesc(String acao, Long usuarioId, Pageable pageable);
}
