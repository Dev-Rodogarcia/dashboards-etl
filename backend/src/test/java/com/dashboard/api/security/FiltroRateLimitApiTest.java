package com.dashboard.api.security;

import com.dashboard.api.model.acesso.AcaoAudit;
import com.dashboard.api.service.acesso.AuditService;
import com.dashboard.api.support.FakeRateLimitBucketStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.assertThat;

class FiltroRateLimitApiTest {

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void limitaEndpointsDeIndicadores() throws Exception {
        FiltroRateLimitApi filtro = filtroComLimite(1, 10);
        autenticar("gestor@empresa.com");

        MockHttpServletResponse primeiraResposta = executar(
                filtro,
                "/api/painel/indicadores-gestao-a-vista/performance-entrega/overview"
        );
        MockHttpServletResponse segundaResposta = executar(
                filtro,
                "/api/painel/indicadores-gestao-a-vista/performance-entrega/overview"
        );

        assertThat(primeiraResposta.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
        assertThat(segundaResposta.getStatus()).isEqualTo(429);
        assertThat(segundaResposta.getHeader("Retry-After")).isNotBlank();
        assertThat(segundaResposta.getContentAsString()).contains("\"timestamp\"");
        assertThat(segundaResposta.getContentAsString()).doesNotContain("\"path\"");
    }

    @Test
    void exportacaoUsaBucketProprioMaisRestrito() throws Exception {
        FiltroRateLimitApi filtro = filtroComLimite(10, 1);
        autenticar("gestor@empresa.com");

        MockHttpServletResponse primeiraResposta = executar(filtro, "/api/painel/fretes/exportacao");
        MockHttpServletResponse segundaResposta = executar(filtro, "/api/painel/fretes/exportacao");
        MockHttpServletResponse chamadaNormal = executar(filtro, "/api/painel/fretes/tabela");

        assertThat(primeiraResposta.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
        assertThat(segundaResposta.getStatus()).isEqualTo(429);
        assertThat(chamadaNormal.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    void rotasDistintasUsamBucketsDistintosParaMesmoUsuario() throws Exception {
        FiltroRateLimitApi filtro = filtroComLimite(1, 10);
        autenticar("gestor@empresa.com");

        MockHttpServletResponse fretes = executar(filtro, "/api/painel/fretes/tabela");
        MockHttpServletResponse cotacoes = executar(filtro, "/api/painel/cotacoes/overview");
        MockHttpServletResponse fretesBloqueado = executar(filtro, "/api/painel/fretes/tabela");

        assertThat(fretes.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
        assertThat(cotacoes.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
        assertThat(fretesBloqueado.getStatus()).isEqualTo(429);
    }

    @Test
    void limitaEndpointsDeMetasKpi() throws Exception {
        FiltroRateLimitApi filtro = filtroComLimite(1, 10);
        autenticar("gestor@empresa.com");

        MockHttpServletResponse primeiraResposta = executar(filtro, "/api/kpi-goals/effective");
        MockHttpServletResponse segundaResposta = executar(filtro, "/api/kpi-goals/effective");

        assertThat(primeiraResposta.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
        assertThat(segundaResposta.getStatus()).isEqualTo(429);
    }

    @Test
    void limitaEndpointsDeComunicadosDaHome() throws Exception {
        FiltroRateLimitApi filtro = filtroComLimite(1, 10);
        autenticar("gestor@empresa.com");

        MockHttpServletResponse primeiraResposta = executar(filtro, "/api/painel/home/comunicados");
        MockHttpServletResponse segundaResposta = executar(filtro, "/api/painel/home/comunicados");

        assertThat(primeiraResposta.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
        assertThat(segundaResposta.getStatus()).isEqualTo(429);
    }

    @Test
    void limitaEndpointDePerformance() throws Exception {
        FiltroRateLimitApi filtro = filtroComLimite(1, 10);
        autenticar("gestor@empresa.com");

        MockHttpServletResponse primeiraResposta = executar(filtro, "GET", "/api/painel/performance/overview");
        MockHttpServletResponse segundaResposta = executar(filtro, "GET", "/api/painel/performance/overview");

        assertThat(primeiraResposta.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
        assertThat(segundaResposta.getStatus()).isEqualTo(429);
    }

    @Test
    void optionsNaoConsomeBucketDoRateLimit() throws Exception {
        FiltroRateLimitApi filtro = filtroComLimite(1, 10);
        autenticar("gestor@empresa.com");

        for (int tentativa = 0; tentativa < 5; tentativa++) {
            MockHttpServletResponse preflight = executar(filtro, "OPTIONS", "/api/painel/performance/overview");
            assertThat(preflight.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
        }

        MockHttpServletResponse getPermitido = executar(filtro, "GET", "/api/painel/performance/overview");
        MockHttpServletResponse getBloqueado = executar(filtro, "GET", "/api/painel/performance/overview");

        assertThat(getPermitido.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
        assertThat(getBloqueado.getStatus()).isEqualTo(429);
    }

    private FiltroRateLimitApi filtroComLimite(int apiMaxRequests, int exportMaxRequests) {
        RateLimitService rateLimitService = new RateLimitService(new FakeRateLimitBucketStore(),
                10,
                900,
                apiMaxRequests,
                60,
                exportMaxRequests,
                60
        );
        return new FiltroRateLimitApi(
                rateLimitService,
                new NoopAuditService(),
                new IpClienteResolver(false),
                new ObjectMapper().findAndRegisterModules()
        );
    }

    private void autenticar(String principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    private MockHttpServletResponse executar(FiltroRateLimitApi filtro, String path) throws Exception {
        return executar(filtro, HttpMethod.GET.name(), path);
    }

    private MockHttpServletResponse executar(FiltroRateLimitApi filtro, String method, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("10.0.0.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) ->
                ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_NO_CONTENT);

        filtro.doFilter(request, response, chain);
        return response;
    }

    private static class NoopAuditService extends AuditService {
        NoopAuditService() {
            super(null, new IpClienteResolver(false));
        }

        @Override
        public void registrarSync(AcaoAudit acao, Long usuarioId, String usuarioLogin, String recurso, String detalhesJson) {
            // no-op para teste do filtro
        }
    }
}
