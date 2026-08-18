package com.dashboard.api.repository.acesso;

import com.dashboard.api.security.RateLimitBucketStore;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcRateLimitBucketStore implements RateLimitBucketStore {

    private final JdbcTemplate jdbcTemplate;

    public JdbcRateLimitBucketStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public RateLimitBucket consumir(String chave, int janelaSegundos, Instant agora) {
        RateLimitBucket atual = buscarParaAtualizacao(chave).orElse(null);
        Instant novaExpiracao = agora.plusSeconds(janelaSegundos);

        if (atual == null) {
            jdbcTemplate.update(
                    "INSERT INTO acesso.rate_limit_buckets (chave, total_na_janela, expira_em) VALUES (?, ?, ?)",
                    chave,
                    1,
                    Timestamp.from(novaExpiracao)
            );
            return new RateLimitBucket(1, novaExpiracao);
        }
        if (!atual.expiraEm().isAfter(agora)) {
            jdbcTemplate.update(
                    "UPDATE acesso.rate_limit_buckets SET total_na_janela = ?, expira_em = ?, atualizado_em = SYSUTCDATETIME() WHERE chave = ?",
                    1,
                    Timestamp.from(novaExpiracao),
                    chave
            );
            return new RateLimitBucket(1, novaExpiracao);
        }

        jdbcTemplate.update(
                "UPDATE acesso.rate_limit_buckets SET total_na_janela = total_na_janela + 1, atualizado_em = SYSUTCDATETIME() WHERE chave = ?",
                chave
        );
        return new RateLimitBucket(atual.totalNaJanela() + 1, atual.expiraEm());
    }

    @Override
    public Optional<RateLimitBucket> consultar(String chave, Instant agora) {
        return buscar(chave).filter(bucket -> bucket.expiraEm().isAfter(agora));
    }

    @Override
    public void expirar(String chave, Instant agora) {
        jdbcTemplate.update(
                "UPDATE acesso.rate_limit_buckets SET total_na_janela = 0, expira_em = ?, atualizado_em = SYSUTCDATETIME() WHERE chave = ?",
                Timestamp.from(agora),
                chave
        );
    }

    private Optional<RateLimitBucket> buscarParaAtualizacao(String chave) {
        return jdbcTemplate.query(
                "SELECT total_na_janela, expira_em FROM acesso.rate_limit_buckets WITH (UPDLOCK, HOLDLOCK) WHERE chave = ?",
                resultSet -> resultSet.next()
                        ? Optional.of(new RateLimitBucket(resultSet.getInt("total_na_janela"), resultSet.getTimestamp("expira_em").toInstant()))
                        : Optional.empty(),
                chave
        );
    }

    private Optional<RateLimitBucket> buscar(String chave) {
        return jdbcTemplate.query(
                "SELECT total_na_janela, expira_em FROM acesso.rate_limit_buckets WHERE chave = ?",
                resultSet -> resultSet.next()
                        ? Optional.of(new RateLimitBucket(resultSet.getInt("total_na_janela"), resultSet.getTimestamp("expira_em").toInstant()))
                        : Optional.empty(),
                chave
        );
    }
}
