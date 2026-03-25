package com.dashboard.api.controller;

import com.dashboard.api.dto.LoginRequestDTO;
import com.dashboard.api.dto.LoginResponseDTO;
import com.dashboard.api.dto.SessaoUsuarioDTO;
import com.dashboard.api.dto.acesso.AlterarSenhaRequestDTO;
import com.dashboard.api.exception.RespostaErroPadrao;
import com.dashboard.api.security.RateLimitService;
import com.dashboard.api.service.acesso.AutenticacaoService;
import com.dashboard.api.service.acesso.CredencialInvalidaException;
import com.dashboard.api.service.acesso.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private static final Logger log = LoggerFactory.getLogger(AutenticacaoController.class);

    private final AutenticacaoService autenticacaoService;
    private final RateLimitService rateLimitService;
    private final RefreshTokenService refreshTokenService;
    private final String refreshCookieName;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;
    private final long refreshTokenExpiracaoDias;

    public AutenticacaoController(
            AutenticacaoService autenticacaoService,
            RateLimitService rateLimitService,
            RefreshTokenService refreshTokenService,
            @Value("${auth.refresh-cookie-name:dashboard_refresh_token}") String refreshCookieName,
            @Value("${auth.refresh-cookie-secure:false}") boolean refreshCookieSecure,
            @Value("${auth.refresh-cookie-same-site:Lax}") String refreshCookieSameSite,
            @Value("${auth.refresh-token-expiracao-dias:30}") long refreshTokenExpiracaoDias
    ) {
        this.autenticacaoService = autenticacaoService;
        this.rateLimitService = rateLimitService;
        this.refreshTokenService = refreshTokenService;
        this.refreshCookieName = refreshCookieName;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
        this.refreshTokenExpiracaoDias = refreshTokenExpiracaoDias;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        RateLimitService.RateLimitDecision decisao = rateLimitService.avaliarTentativaLogin(ip, request.email());
        if (!decisao.permitido()) {
            return respostaLimiteExcedido(decisao);
        }

        try {
            LoginResponseDTO resposta = autenticacaoService.autenticar(request.email(), request.senha());
            var usuario = autenticacaoService.carregarUsuarioAtivoPorEmail(request.email());
            var refresh = refreshTokenService.emitir(usuario, ip, httpRequest.getHeader("User-Agent"));

            rateLimitService.limparTentativasLogin(ip, request.email());
            log.info("Login realizado com sucesso para usuario={} papel={} setor={}",
                    resposta.usuario().email(),
                    resposta.usuario().papel(),
                    resposta.usuario().setor().nome());

            return ResponseEntity.ok()
                    .header("Set-Cookie", criarRefreshCookie(refresh.tokenPlano()))
                    .body(resposta);
        } catch (CredencialInvalidaException ex) {
            RateLimitService.RateLimitDecision falha = rateLimitService.consumirTentativaLogin(ip, request.email());
            log.warn("Falha de autenticacao para usuario={}", request.email());
            if (!falha.permitido()) {
                return respostaLimiteExcedido(falha);
            }

            RespostaErroPadrao erro = new RespostaErroPadrao(
                    LocalDateTime.now(),
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized",
                    "Usuario ou senha invalidos."
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(erro);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<SessaoUsuarioDTO> me(Authentication authentication) {
        return ResponseEntity.ok(autenticacaoService.buscarSessaoAtual(authentication.getName()));
    }

    @PostMapping("/alterar-senha")
    public ResponseEntity<?> alterarSenha(Authentication authentication, @Valid @RequestBody AlterarSenhaRequestDTO request) {
        try {
            autenticacaoService.alterarSenha(authentication.getName(), request.senhaAtual(), request.novaSenha());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException ex) {
            RespostaErroPadrao erro = new RespostaErroPadrao(
                    LocalDateTime.now(),
                    HttpStatus.BAD_REQUEST.value(),
                    "Bad Request",
                    ex.getMessage()
            );
            return ResponseEntity.badRequest().body(erro);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        try {
            RefreshTokenService.RefreshTokenRotacionado rotacao = refreshTokenService.rotacionar(
                    extrairRefreshToken(request),
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );

            LoginResponseDTO resposta = autenticacaoService.gerarSessaoParaUsuario(rotacao.usuario());
            return ResponseEntity.ok()
                    .header("Set-Cookie", criarRefreshCookie(rotacao.tokenPlano()))
                    .body(resposta);
        } catch (CredencialInvalidaException ex) {
            RespostaErroPadrao erro = new RespostaErroPadrao(
                    LocalDateTime.now(),
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized",
                    "Sessao expirada."
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("Set-Cookie", limparRefreshCookie())
                    .body(erro);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, Authentication authentication) {
        refreshTokenService.revogar(extrairRefreshToken(request));
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            log.info("Logout realizado para usuario={}", authentication.getName());
        }

        return ResponseEntity.noContent()
                .header("Set-Cookie", limparRefreshCookie())
                .build();
    }

    private ResponseEntity<RespostaErroPadrao> respostaLimiteExcedido(RateLimitService.RateLimitDecision decisao) {
        RespostaErroPadrao erro = new RespostaErroPadrao(
                LocalDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                "Muitas tentativas de autenticação. Aguarde alguns minutos e tente novamente."
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(decisao.retryAfterSeconds()))
                .body(erro);
    }

    private String criarRefreshCookie(String tokenPlano) {
        String cookieName = Objects.requireNonNull(refreshCookieName, "auth.refresh-cookie-name é obrigatório.");
        String sameSite = Objects.requireNonNull(refreshCookieSameSite, "auth.refresh-cookie-same-site é obrigatório.");

        return ResponseCookie.from(cookieName, Objects.requireNonNull(tokenPlano, "refresh token é obrigatório."))
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(sameSite)
                .path("/api/auth")
                .maxAge(refreshTokenExpiracaoDias * 24 * 60 * 60)
                .build()
                .toString();
    }

    private String limparRefreshCookie() {
        String cookieName = Objects.requireNonNull(refreshCookieName, "auth.refresh-cookie-name é obrigatório.");
        String sameSite = Objects.requireNonNull(refreshCookieSameSite, "auth.refresh-cookie-same-site é obrigatório.");

        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(sameSite)
                .path("/api/auth")
                .maxAge(0)
                .build()
                .toString();
    }

    private String extrairRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (var cookie : request.getCookies()) {
            if (refreshCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
