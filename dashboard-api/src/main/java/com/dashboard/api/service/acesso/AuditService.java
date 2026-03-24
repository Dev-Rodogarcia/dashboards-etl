package com.dashboard.api.service.acesso;

import com.dashboard.api.model.acesso.AuditLog;
import com.dashboard.api.repository.acesso.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;
    private final boolean confiarEmForwardedHeaders;

    public AuditService(
            AuditLogRepository auditLogRepository,
            @Value("${security.trust-forwarded-headers:false}") boolean confiarEmForwardedHeaders
    ) {
        this.auditLogRepository = auditLogRepository;
        this.confiarEmForwardedHeaders = confiarEmForwardedHeaders;
    }

    @Async
    public void registrar(AcaoAudit acao, Long usuarioId, String usuarioLogin, String recurso, String detalhesJson) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAcao(acao.name());
            entry.setUsuarioId(usuarioId);
            entry.setUsuarioLogin(usuarioLogin);
            entry.setRecurso(recurso);
            entry.setDetalhesJson(detalhesJson);

            extrairDadosRequest(entry);
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("Falha ao gravar audit log: acao={}, usuario={}", acao, usuarioLogin, ex);
        }
    }

    public void registrarSync(AcaoAudit acao, Long usuarioId, String usuarioLogin, String recurso, String detalhesJson) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAcao(acao.name());
            entry.setUsuarioId(usuarioId);
            entry.setUsuarioLogin(usuarioLogin);
            entry.setRecurso(recurso);
            entry.setDetalhesJson(detalhesJson);

            extrairDadosRequest(entry);
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("Falha ao gravar audit log: acao={}, usuario={}", acao, usuarioLogin, ex);
        }
    }

    private void extrairDadosRequest(AuditLog entry) {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                HttpServletRequest request = sra.getRequest();
                entry.setIpAddress(obterIpReal(request));
                entry.setUserAgent(truncar(request.getHeader("User-Agent"), 500));
            }
        } catch (Exception ignored) {
            // fora de contexto de request (ex: migracao)
        }
    }

    private String obterIpReal(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (confiarEmForwardedHeaders && forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncar(String valor, int max) {
        if (valor == null) return null;
        return valor.length() > max ? valor.substring(0, max) : valor;
    }
}
