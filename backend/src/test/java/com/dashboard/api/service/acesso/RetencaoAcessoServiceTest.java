package com.dashboard.api.service.acesso;

import com.dashboard.api.repository.acesso.AuditLogRepository;
import com.dashboard.api.repository.acesso.RefreshTokenSessionRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetencaoAcessoServiceTest {

    @Test
    void limparAntigosRemoveRefreshTokensExpiradosEAuditLogsAntigos() {
        RefreshTokenSessionRepository refreshTokenRepository = mock(RefreshTokenSessionRepository.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        RetencaoAcessoService service = new RetencaoAcessoService(
                refreshTokenRepository,
                auditLogRepository,
                30,
                90
        );
        Instant referencia = Instant.parse("2026-04-28T12:00:00Z");
        Instant limiteRefresh = Instant.parse("2026-03-29T12:00:00Z");
        Instant limiteAudit = Instant.parse("2026-01-28T12:00:00Z");

        when(refreshTokenRepository.deleteByExpiraEmBefore(limiteRefresh)).thenReturn(4L);
        when(auditLogRepository.deleteByTimestampUtcBefore(limiteAudit)).thenReturn(9L);

        RetencaoAcessoService.ResultadoRetencao resultado = service.limparAntigos(referencia);

        assertThat(resultado.refreshTokensRemovidos()).isEqualTo(4L);
        assertThat(resultado.auditLogsRemovidos()).isEqualTo(9L);
        verify(refreshTokenRepository).deleteByExpiraEmBefore(limiteRefresh);
        verify(auditLogRepository).deleteByTimestampUtcBefore(limiteAudit);
    }
}
