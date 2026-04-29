package com.dashboard.api.service.acesso;

import com.dashboard.api.repository.acesso.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;

@Component
public class AuditLogAsyncListener {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAsyncListener.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogAsyncListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void salvar(@NonNull AuditService.AuditLogRegistradoEvent event) {
        try {
            auditLogRepository.save(Objects.requireNonNull(event.auditLog()));
        } catch (Exception ex) {
            log.error(
                    "Falha ao gravar audit log assincrono: acao={}, usuario={}",
                    event.auditLog().getAcao(),
                    event.auditLog().getUsuarioLogin(),
                    ex
            );
        }
    }
}
