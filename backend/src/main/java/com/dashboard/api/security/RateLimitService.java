package com.dashboard.api.security;

import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final RateLimitBucketStore bucketStore;
    private final int loginMaxAttempts;
    private final int loginWindowSeconds;
    private final int apiMaxRequests;
    private final int apiWindowSeconds;
    private final int exportMaxRequests;
    private final int exportWindowSeconds;
    private final int passwordResetMaxRequests;
    private final int passwordResetWindowSeconds;

    @Autowired
    public RateLimitService(
            RateLimitBucketStore bucketStore,
            @Value("${security.rate-limit.login.max-attempts:10}") int loginMaxAttempts,
            @Value("${security.rate-limit.login.window-seconds:900}") int loginWindowSeconds,
            @Value("${security.rate-limit.api.max-requests:120}") int apiMaxRequests,
            @Value("${security.rate-limit.api.window-seconds:60}") int apiWindowSeconds,
            @Value("${security.rate-limit.export.max-requests:12}") int exportMaxRequests,
            @Value("${security.rate-limit.export.window-seconds:60}") int exportWindowSeconds,
            @Value("${security.rate-limit.password-reset.max-requests:5}") int passwordResetMaxRequests,
            @Value("${security.rate-limit.password-reset.window-seconds:3600}") int passwordResetWindowSeconds
    ) {
        this.bucketStore = bucketStore;
        this.loginMaxAttempts = loginMaxAttempts;
        this.loginWindowSeconds = loginWindowSeconds;
        this.apiMaxRequests = apiMaxRequests;
        this.apiWindowSeconds = apiWindowSeconds;
        this.exportMaxRequests = exportMaxRequests;
        this.exportWindowSeconds = exportWindowSeconds;
        this.passwordResetMaxRequests = passwordResetMaxRequests;
        this.passwordResetWindowSeconds = passwordResetWindowSeconds;
    }

    public RateLimitService(
            RateLimitBucketStore bucketStore,
            int loginMaxAttempts,
            int loginWindowSeconds,
            int apiMaxRequests,
            int apiWindowSeconds,
            int exportMaxRequests,
            int exportWindowSeconds
    ) {
        this(
                bucketStore,
                loginMaxAttempts,
                loginWindowSeconds,
                apiMaxRequests,
                apiWindowSeconds,
                exportMaxRequests,
                exportWindowSeconds,
                5,
                3600
        );
    }

    public RateLimitDecision consumirTentativaLogin(String ip, String loginOuEmail) {
        return consumir("login:" + normalizar(ip) + ":" + normalizar(loginOuEmail), loginMaxAttempts, loginWindowSeconds);
    }

    public RateLimitDecision avaliarTentativaLogin(String ip, String loginOuEmail) {
        return consultar("login:" + normalizar(ip) + ":" + normalizar(loginOuEmail), loginMaxAttempts);
    }

    public void limparTentativasLogin(String ip, String loginOuEmail) {
        bucketStore.expirar("login:" + normalizar(ip) + ":" + normalizar(loginOuEmail), Instant.now());
    }

    public RateLimitDecision consumirChamadaApi(String identificador) {
        return consumir("api:" + normalizar(identificador), apiMaxRequests, apiWindowSeconds);
    }

    public RateLimitDecision consumirExportacao(String identificador) {
        return consumir("export:" + normalizar(identificador), exportMaxRequests, exportWindowSeconds);
    }

    public RateLimitDecision consumirSolicitacaoRedefinicaoSenha(String ip) {
        return consumir("password-reset:" + normalizar(ip), passwordResetMaxRequests, passwordResetWindowSeconds);
    }

    private RateLimitDecision consumir(String chave, int limite, int janelaSegundos) {
        Instant agora = Instant.now();
        RateLimitBucketStore.RateLimitBucket bucket = bucketStore.consumir(chave, janelaSegundos, agora);
        int total = bucket.totalNaJanela();
        long retryAfterSeconds = Math.max(1L, (bucket.expiraEm().toEpochMilli() - agora.toEpochMilli() + 999L) / 1000L);
        return new RateLimitDecision(total <= limite, retryAfterSeconds, total);
    }

    private RateLimitDecision consultar(String chave, int limite) {
        Instant agora = Instant.now();
        RateLimitBucketStore.RateLimitBucket bucket = bucketStore.consultar(chave, agora).orElse(null);
        if (bucket == null) {
            return new RateLimitDecision(true, 1L, 0);
        }

        int total = bucket.totalNaJanela();
        long retryAfterSeconds = Math.max(1L, (bucket.expiraEm().toEpochMilli() - agora.toEpochMilli() + 999L) / 1000L);
        return new RateLimitDecision(total < limite, retryAfterSeconds, total);
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "anon";
        }
        return valor.trim().toLowerCase(Locale.ROOT);
    }

    public record RateLimitDecision(boolean permitido, long retryAfterSeconds, int totalNaJanela) {
    }
}
