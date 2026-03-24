package com.dashboard.api.security;

import com.dashboard.api.service.acesso.AcaoAudit;
import com.dashboard.api.service.acesso.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class FiltroRateLimitApi extends OncePerRequestFilter {

    private static final List<String> PREFIXOS_LIMITADOS = List.of(
            "/api/painel/coletas",
            "/api/painel/contas-a-pagar",
            "/api/painel/cotacoes",
            "/api/dimensoes",
            "/api/painel/etl-saude",
            "/api/painel/executivo",
            "/api/painel/faturas",
            "/api/painel/faturas-por-cliente",
            "/api/painel/fretes",
            "/api/painel/manifestos",
            "/api/painel/tracking"
    );

    private final RateLimitService rateLimitService;
    private final AuditService auditService;

    public FiltroRateLimitApi(RateLimitService rateLimitService, AuditService auditService) {
        this.rateLimitService = rateLimitService;
        this.auditService = auditService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return PREFIXOS_LIMITADOS.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String principal = principalAtual();
        RateLimitService.RateLimitDecision decisao = rateLimitService.consumirChamadaApi(request.getRemoteAddr() + ":" + principal);

        if (!decisao.permitido()) {
            auditService.registrarSync(
                    AcaoAudit.RATE_LIMIT_EXCEDIDO,
                    null,
                    principal,
                    request.getRequestURI(),
                    "{\"janelaRequests\":" + decisao.totalNaJanela() + "}"
            );
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(decisao.retryAfterSeconds()));
            response.getWriter().write(
                    "{\"status\":429,\"erro\":\"Too Many Requests\",\"mensagem\":\"Limite temporário de requisições excedido.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String principalAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "anon";
        }
        return authentication.getName();
    }
}
