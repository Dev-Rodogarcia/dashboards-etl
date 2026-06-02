package com.dashboard.api.service.acesso;

import com.dashboard.api.model.acesso.AcaoAudit;
import com.dashboard.api.model.acesso.AuditLog;
import com.dashboard.api.repository.acesso.AuditLogRepository;
import com.dashboard.api.security.IpClienteResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;
    private final IpClienteResolver ipClienteResolver;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public AuditService(
            AuditLogRepository auditLogRepository,
            IpClienteResolver ipClienteResolver,
            ApplicationEventPublisher eventPublisher
    ) {
        this.auditLogRepository = auditLogRepository;
        this.ipClienteResolver = ipClienteResolver;
        this.eventPublisher = eventPublisher;
    }

    public AuditService(
            AuditLogRepository auditLogRepository,
            IpClienteResolver ipClienteResolver
    ) {
        this(auditLogRepository, ipClienteResolver, null);
    }

    public void registrar(AcaoAudit acao, Long usuarioId, String usuarioLogin, String recurso, String detalhesJson) {
        try {
            AuditLog entry = criarEntry(acao, usuarioId, usuarioLogin, recurso, detalhesJson);
            if (eventPublisher != null) {
                eventPublisher.publishEvent(Objects.requireNonNull(new AuditLogRegistradoEvent(Objects.requireNonNull(entry))));
            } else {
                auditLogRepository.save(Objects.requireNonNull(entry));
            }
        } catch (Exception ex) {
            log.error("Falha ao gravar audit log: acao={}, usuario={}", acao, usuarioLogin, ex);
        }
    }

    public void registrarSync(AcaoAudit acao, Long usuarioId, String usuarioLogin, String recurso, String detalhesJson) {
        try {
            AuditLog entry = criarEntry(acao, usuarioId, usuarioLogin, recurso, detalhesJson);
            auditLogRepository.save(Objects.requireNonNull(entry));
        } catch (Exception ex) {
            log.error("Falha ao gravar audit log: acao={}, usuario={}", acao, usuarioLogin, ex);
        }
    }

    @NonNull
    private AuditLog criarEntry(AcaoAudit acao, Long usuarioId, String usuarioLogin, String recurso, String detalhesJson) {
        AuditLog entry = new AuditLog();
        entry.setAcao(acao.name());
        entry.setUsuarioId(usuarioId);
        entry.setUsuarioLogin(usuarioLogin);
        entry.setRecurso(recurso);
        entry.setDetalhesJson(detalhesJson);
        extrairDadosRequest(entry);
        return entry;
    }

    private void extrairDadosRequest(AuditLog entry) {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                HttpServletRequest request = sra.getRequest();
                entry.setIpAddress(ipClienteResolver.resolver(request));
                entry.setUserAgent(truncar(request.getHeader("User-Agent"), 500));
            }
        } catch (Exception ignored) {
            // fora de contexto de request (ex: migracao)
        }
    }

    private String truncar(String valor, int max) {
        if (valor == null) return null;
        return valor.length() > max ? valor.substring(0, max) : valor;
    }

    public record AuditLogRegistradoEvent(@NonNull AuditLog auditLog) {}
}
