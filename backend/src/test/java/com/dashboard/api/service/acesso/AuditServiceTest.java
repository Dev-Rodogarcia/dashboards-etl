package com.dashboard.api.service.acesso;

import com.dashboard.api.model.acesso.AcaoAudit;
import com.dashboard.api.model.acesso.AuditLog;
import com.dashboard.api.repository.acesso.AuditLogRepository;
import com.dashboard.api.security.IpClienteResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditServiceTest {

    @AfterEach
    void limparRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void registrarCapturaDadosDaRequestAntesDePublicarEventoAssincrono() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        AuditService service = new AuditService(repository, new IpClienteResolver(true), publisher);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("CF-Connecting-IP", "203.0.113.10");
        request.addHeader("User-Agent", "JUnit");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        service.registrar(AcaoAudit.LOGIN, 10L, "maria@empresa.com", "auth", null);

        var captor = forClass(AuditService.AuditLogRegistradoEvent.class);
        verify(publisher).publishEvent(captureNonNull(captor));

        AuditLog auditLog = captor.getValue().auditLog();
        assertThat(auditLog.getAcao()).isEqualTo(AcaoAudit.LOGIN.name());
        assertThat(auditLog.getUsuarioId()).isEqualTo(10L);
        assertThat(auditLog.getUsuarioLogin()).isEqualTo("maria@empresa.com");
        assertThat(auditLog.getIpAddress()).isEqualTo("203.0.113.10");
        assertThat(auditLog.getUserAgent()).isEqualTo("JUnit");
    }

    @SuppressWarnings("null")
    @NonNull
    private static <T> T captureNonNull(ArgumentCaptor<T> captor) {
        return captor.capture();
    }
}
