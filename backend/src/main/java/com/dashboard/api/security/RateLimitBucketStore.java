package com.dashboard.api.security;

import java.time.Instant;
import java.util.Optional;

public interface RateLimitBucketStore {

    RateLimitBucket consumir(String chave, int janelaSegundos, Instant agora);

    Optional<RateLimitBucket> consultar(String chave, Instant agora);

    void expirar(String chave, Instant agora);

    record RateLimitBucket(int totalNaJanela, Instant expiraEm) {
    }
}
