package com.dashboard.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RateLimitService {

    private static final long LIMPEZA_INTERVALO_OPERACOES = 512;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong operacoes = new AtomicLong();
    private final int loginMaxAttempts;
    private final int loginWindowSeconds;
    private final int apiMaxRequests;
    private final int apiWindowSeconds;

    public RateLimitService(
            @Value("${security.rate-limit.login.max-attempts:10}") int loginMaxAttempts,
            @Value("${security.rate-limit.login.window-seconds:900}") int loginWindowSeconds,
            @Value("${security.rate-limit.api.max-requests:120}") int apiMaxRequests,
            @Value("${security.rate-limit.api.window-seconds:60}") int apiWindowSeconds
    ) {
        this.loginMaxAttempts = loginMaxAttempts;
        this.loginWindowSeconds = loginWindowSeconds;
        this.apiMaxRequests = apiMaxRequests;
        this.apiWindowSeconds = apiWindowSeconds;
    }

    public RateLimitDecision consumirTentativaLogin(String ip, String loginOuEmail) {
        return consumir("login:" + normalizar(ip) + ":" + normalizar(loginOuEmail), loginMaxAttempts, loginWindowSeconds);
    }

    public RateLimitDecision avaliarTentativaLogin(String ip, String loginOuEmail) {
        return consultar("login:" + normalizar(ip) + ":" + normalizar(loginOuEmail), loginMaxAttempts);
    }

    public void limparTentativasLogin(String ip, String loginOuEmail) {
        buckets.remove("login:" + normalizar(ip) + ":" + normalizar(loginOuEmail));
    }

    public RateLimitDecision consumirChamadaApi(String identificador) {
        return consumir("api:" + normalizar(identificador), apiMaxRequests, apiWindowSeconds);
    }

    private RateLimitDecision consumir(String chave, int limite, int janelaSegundos) {
        long agora = Instant.now().toEpochMilli();
        long expiraEm = agora + (janelaSegundos * 1000L);
        if (operacoes.incrementAndGet() % LIMPEZA_INTERVALO_OPERACOES == 0) {
            limparExpirados(agora);
        }

        Bucket bucket = buckets.compute(chave, (key, atual) -> {
            if (atual == null || atual.expiraEmEpochMs() <= agora) {
                return new Bucket(expiraEm, new AtomicInteger(1));
            }
            atual.contador().incrementAndGet();
            return atual;
        });

        int total = bucket.contador().get();
        long retryAfterSeconds = Math.max(1L, (bucket.expiraEmEpochMs() - agora + 999L) / 1000L);
        return new RateLimitDecision(total <= limite, retryAfterSeconds, total);
    }

    private RateLimitDecision consultar(String chave, int limite) {
        long agora = Instant.now().toEpochMilli();
        if (operacoes.incrementAndGet() % LIMPEZA_INTERVALO_OPERACOES == 0) {
            limparExpirados(agora);
        }

        Bucket bucket = buckets.get(chave);
        if (bucket == null || bucket.expiraEmEpochMs() <= agora) {
            return new RateLimitDecision(true, 1L, 0);
        }

        int total = bucket.contador().get();
        long retryAfterSeconds = Math.max(1L, (bucket.expiraEmEpochMs() - agora + 999L) / 1000L);
        return new RateLimitDecision(total < limite, retryAfterSeconds, total);
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "anon";
        }
        return valor.trim().toLowerCase(Locale.ROOT);
    }

    private void limparExpirados(long agora) {
        buckets.entrySet().removeIf(entry -> entry.getValue().expiraEmEpochMs() <= agora);
    }

    private record Bucket(long expiraEmEpochMs, AtomicInteger contador) {
    }

    public record RateLimitDecision(boolean permitido, long retryAfterSeconds, int totalNaJanela) {
    }
}
