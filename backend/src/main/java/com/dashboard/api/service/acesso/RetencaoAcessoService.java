package com.dashboard.api.service.acesso;

import com.dashboard.api.repository.acesso.AuditLogRepository;
import com.dashboard.api.repository.acesso.RefreshTokenSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RetencaoAcessoService {

    private static final Logger log = LoggerFactory.getLogger(RetencaoAcessoService.class);

    private final RefreshTokenSessionRepository refreshTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final TransactionTemplate transactionTemplate;
    private final long refreshTokenRetencaoDias;
    private final long auditLogRetencaoDias;

    @Autowired
    public RetencaoAcessoService(
            RefreshTokenSessionRepository refreshTokenRepository,
            AuditLogRepository auditLogRepository,
            TransactionTemplate transactionTemplate,
            @Value("${acesso.retencao.refresh-token-expirado-dias:30}") long refreshTokenRetencaoDias,
            @Value("${acesso.retencao.audit-log-dias:90}") long auditLogRetencaoDias
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogRepository = auditLogRepository;
        this.transactionTemplate = transactionTemplate;
        this.refreshTokenRetencaoDias = refreshTokenRetencaoDias;
        this.auditLogRetencaoDias = auditLogRetencaoDias;
    }

    public RetencaoAcessoService(
            RefreshTokenSessionRepository refreshTokenRepository,
            AuditLogRepository auditLogRepository,
            long refreshTokenRetencaoDias,
            long auditLogRetencaoDias
    ) {
        this(refreshTokenRepository, auditLogRepository, null, refreshTokenRetencaoDias, auditLogRetencaoDias);
    }

    @Scheduled(cron = "${acesso.retencao.cron:0 30 3 * * *}")
    public void executarLimpezaAgendada() {
        try {
            ResultadoRetencao resultado = limparAntigos(Instant.now());
            if (resultado.refreshTokensRemovidos() > 0 || resultado.auditLogsRemovidos() > 0) {
                log.info(
                        "Retencao de acesso concluida: refreshTokensRemovidos={}, auditLogsRemovidos={}",
                        resultado.refreshTokensRemovidos(),
                        resultado.auditLogsRemovidos()
                );
            }
        } catch (Exception ex) {
            log.error("Falha na retencao de acesso.", ex);
        }
    }

    public ResultadoRetencao limparAntigos(Instant referencia) {
        if (transactionTemplate == null) {
            return executarLimpeza(referencia);
        }

        return transactionTemplate.execute(status -> executarLimpeza(referencia));
    }

    private ResultadoRetencao executarLimpeza(Instant referencia) {
        Instant limiteRefreshTokens = referencia.minus(refreshTokenRetencaoDias, ChronoUnit.DAYS);
        Instant limiteAuditLogs = referencia.minus(auditLogRetencaoDias, ChronoUnit.DAYS);

        long refreshTokensRemovidos = refreshTokenRepository.deleteByExpiraEmBefore(limiteRefreshTokens);
        long auditLogsRemovidos = auditLogRepository.deleteByTimestampUtcBefore(limiteAuditLogs);

        return new ResultadoRetencao(refreshTokensRemovidos, auditLogsRemovidos);
    }

    public record ResultadoRetencao(long refreshTokensRemovidos, long auditLogsRemovidos) {}
}
