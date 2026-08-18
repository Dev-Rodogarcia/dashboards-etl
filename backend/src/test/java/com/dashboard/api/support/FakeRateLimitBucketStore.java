package com.dashboard.api.support;

import com.dashboard.api.security.RateLimitBucketStore;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class FakeRateLimitBucketStore implements RateLimitBucketStore {

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public synchronized RateLimitBucket consumir(String chave, int janelaSegundos, Instant agora) {
        RateLimitBucket atual = buckets.get(chave);
        if (atual == null || !atual.expiraEm().isAfter(agora)) {
            RateLimitBucket novo = new RateLimitBucket(1, agora.plusSeconds(janelaSegundos));
            buckets.put(chave, novo);
            return novo;
        }
        RateLimitBucket atualizado = new RateLimitBucket(atual.totalNaJanela() + 1, atual.expiraEm());
        buckets.put(chave, atualizado);
        return atualizado;
    }

    @Override
    public Optional<RateLimitBucket> consultar(String chave, Instant agora) {
        return Optional.ofNullable(buckets.get(chave)).filter(bucket -> bucket.expiraEm().isAfter(agora));
    }

    @Override
    public void expirar(String chave, Instant agora) {
        buckets.put(chave, new RateLimitBucket(0, agora));
    }
}
