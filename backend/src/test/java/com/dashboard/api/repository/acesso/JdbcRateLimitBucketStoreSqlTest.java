package com.dashboard.api.repository.acesso;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JdbcRateLimitBucketStoreSqlTest {

    @Test
    void migrationCriaBucketComIndiceDeExpiracao() throws Exception {
        String migration = Files.readString(Path.of("..", "database", "migrations", "V066__criar_rate_limit_compartilhado.sql"));

        assertThat(migration).contains("CREATE TABLE acesso.rate_limit_buckets");
        assertThat(migration).contains("IX_rate_limit_buckets_expira_em");
        assertThat(migration).contains("IF OBJECT_ID");
    }

    @Test
    void storeUsaBloqueioSerializavelPorChave() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java", "com", "dashboard", "api", "repository", "acesso", "JdbcRateLimitBucketStore.java"));

        assertThat(source).contains("Isolation.SERIALIZABLE");
        assertThat(source).contains("UPDLOCK, HOLDLOCK");
    }
}
