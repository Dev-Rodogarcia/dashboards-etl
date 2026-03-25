package com.dashboard.api.service.acesso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public RefreshTokenSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!schemaExiste("acesso")) {
            log.warn("Schema 'acesso' não encontrado. Bootstrap de refresh token não executado.");
            return;
        }

        if (!tabelaExiste("acesso.usuarios")) {
            log.warn("Tabela 'acesso.usuarios' não encontrada. Bootstrap de refresh token não executado.");
            return;
        }

        if (!tabelaExiste("acesso.refresh_tokens")) {
            jdbcTemplate.execute("""
                CREATE TABLE acesso.refresh_tokens (
                    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
                    usuario_id           BIGINT        NOT NULL REFERENCES acesso.usuarios(id),
                    token_hash           VARCHAR(128)  NOT NULL UNIQUE,
                    expira_em            DATETIME2     NOT NULL,
                    revogado_em          DATETIME2     NULL,
                    substituido_por_hash VARCHAR(128)  NULL,
                    criado_em            DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
                    criado_ip            VARCHAR(45)   NULL,
                    user_agent           VARCHAR(500)  NULL
                )
                """);
            log.info("Tabela 'acesso.refresh_tokens' criada automaticamente.");
        }

        if (!indiceExiste("acesso.refresh_tokens", "IX_refresh_tokens_usuario")) {
            jdbcTemplate.execute("""
                CREATE INDEX IX_refresh_tokens_usuario
                    ON acesso.refresh_tokens (usuario_id, revogado_em, expira_em DESC)
                """);
            log.info("Índice 'IX_refresh_tokens_usuario' criado automaticamente.");
        }
    }

    private boolean schemaExiste(String nomeSchema) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys.schemas WHERE name = ?",
                Integer.class,
                nomeSchema
        );
        return total != null && total > 0;
    }

    private boolean tabelaExiste(String nomeCompletoTabela) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) WHERE OBJECT_ID(?, 'U') IS NOT NULL",
                Integer.class,
                nomeCompletoTabela
        );
        return total != null && total > 0;
    }

    private boolean indiceExiste(String nomeCompletoTabela, String nomeIndice) {
        Integer total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM sys.indexes
                WHERE name = ?
                  AND object_id = OBJECT_ID(?, 'U')
                """,
                Integer.class,
                nomeIndice,
                nomeCompletoTabela
        );
        return total != null && total > 0;
    }
}
