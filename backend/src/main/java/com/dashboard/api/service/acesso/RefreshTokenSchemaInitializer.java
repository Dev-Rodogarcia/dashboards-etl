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
        exigir(schemaExiste("acesso"), "Schema 'acesso' não encontrado. Execute as migrations Flyway antes de iniciar a API.");
        exigir(tabelaExiste("acesso.usuarios"), "Tabela 'acesso.usuarios' não encontrada. Execute as migrations Flyway antes de iniciar a API.");
        exigir(tabelaExiste("acesso.refresh_tokens"), "Tabela 'acesso.refresh_tokens' não encontrada. Execute a migration V005.");
        exigir(colunaExiste("acesso.refresh_tokens", "id"), "Coluna 'acesso.refresh_tokens.id' não encontrada.");
        exigir(colunaExiste("acesso.refresh_tokens", "usuario_id"), "Coluna 'acesso.refresh_tokens.usuario_id' não encontrada.");
        exigir(colunaExiste("acesso.refresh_tokens", "token_hash"), "Coluna 'acesso.refresh_tokens.token_hash' não encontrada.");
        exigir(colunaExiste("acesso.refresh_tokens", "expira_em"), "Coluna 'acesso.refresh_tokens.expira_em' não encontrada.");
        exigir(colunaExiste("acesso.refresh_tokens", "revogado_em"), "Coluna 'acesso.refresh_tokens.revogado_em' não encontrada.");
        exigir(colunaExiste("acesso.refresh_tokens", "substituido_por_hash"), "Coluna 'acesso.refresh_tokens.substituido_por_hash' não encontrada.");
        exigir(colunaExiste("acesso.refresh_tokens", "criado_em"), "Coluna 'acesso.refresh_tokens.criado_em' não encontrada.");
        exigir(colunaExiste("acesso.refresh_tokens", "criado_ip"), "Coluna 'acesso.refresh_tokens.criado_ip' não encontrada.");
        exigir(colunaExiste("acesso.refresh_tokens", "user_agent"), "Coluna 'acesso.refresh_tokens.user_agent' não encontrada.");
        exigir(indiceExiste("acesso.refresh_tokens", "IX_refresh_tokens_usuario"), "Índice 'IX_refresh_tokens_usuario' não encontrado.");
        log.info("Schema de refresh tokens validado.");
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

    private boolean colunaExiste(String nomeCompletoTabela, String nomeColuna) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) WHERE COL_LENGTH(?, ?) IS NOT NULL",
                Integer.class,
                nomeCompletoTabela,
                nomeColuna
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

    private void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new IllegalStateException(mensagem);
        }
    }
}
