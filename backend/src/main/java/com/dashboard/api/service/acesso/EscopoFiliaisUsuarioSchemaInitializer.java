package com.dashboard.api.service.acesso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class EscopoFiliaisUsuarioSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EscopoFiliaisUsuarioSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public EscopoFiliaisUsuarioSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!schemaExiste("acesso")) {
                log.warn("Schema 'acesso' não encontrado. Bootstrap de escopo de filiais por usuário não executado.");
                return;
            }

            if (!tabelaExiste("acesso.usuarios")) {
                log.warn("Tabela 'acesso.usuarios' não encontrada. Bootstrap de escopo de filiais por usuário não executado.");
                return;
            }

            if (!colunaExiste("acesso.usuarios", "escopo_filiais_tipo")) {
                jdbcTemplate.execute("""
                    ALTER TABLE acesso.usuarios
                    ADD escopo_filiais_tipo VARCHAR(20) NOT NULL
                        CONSTRAINT DF_usuarios_escopo_filiais_tipo DEFAULT 'HERDAR_SETOR'
                    """);
                log.info("Coluna 'acesso.usuarios.escopo_filiais_tipo' criada automaticamente.");
            }

            if (!constraintExiste("acesso.usuarios", "CK_usuarios_escopo_filiais_tipo")) {
                jdbcTemplate.execute("""
                    ALTER TABLE acesso.usuarios
                    WITH CHECK ADD CONSTRAINT CK_usuarios_escopo_filiais_tipo
                    CHECK (escopo_filiais_tipo IN ('HERDAR_SETOR', 'TODAS', 'SELECIONADAS'))
                    """);
                log.info("Constraint 'CK_usuarios_escopo_filiais_tipo' criada automaticamente.");
            }

            if (!tabelaExiste("acesso.usuario_filiais_permitidas")) {
                jdbcTemplate.execute("""
                    CREATE TABLE acesso.usuario_filiais_permitidas (
                        usuario_id  BIGINT        NOT NULL REFERENCES acesso.usuarios(id),
                        filial_nome NVARCHAR(120) NOT NULL,
                        PRIMARY KEY (usuario_id, filial_nome)
                    )
                    """);
                log.info("Tabela 'acesso.usuario_filiais_permitidas' criada automaticamente.");
            }
        } catch (DataAccessException ex) {
            log.warn("Bootstrap de escopo de filiais por usuário não conseguiu aplicar DDL automaticamente. Execute a migration V011 com um usuário de banco com permissão de ALTER/CREATE. Motivo: {}", ex.getMessage());
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

    private boolean colunaExiste(String nomeCompletoTabela, String nomeColuna) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) WHERE COL_LENGTH(?, ?) IS NOT NULL",
                Integer.class,
                nomeCompletoTabela,
                nomeColuna
        );
        return total != null && total > 0;
    }

    private boolean constraintExiste(String nomeCompletoTabela, String nomeConstraint) {
        Integer total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM sys.check_constraints
                WHERE name = ?
                  AND parent_object_id = OBJECT_ID(?, 'U')
                """,
                Integer.class,
                nomeConstraint,
                nomeCompletoTabela
        );
        return total != null && total > 0;
    }
}
