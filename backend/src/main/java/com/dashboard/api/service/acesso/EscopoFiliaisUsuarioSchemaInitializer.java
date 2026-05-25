package com.dashboard.api.service.acesso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        exigir(schemaExiste("acesso"), "Schema 'acesso' não encontrado. Execute as migrations Flyway antes de iniciar a API.");
        exigir(tabelaExiste("acesso.usuarios"), "Tabela 'acesso.usuarios' não encontrada. Execute as migrations Flyway antes de iniciar a API.");
        exigir(colunaExiste("acesso.usuarios", "escopo_filiais_tipo"), "Coluna 'acesso.usuarios.escopo_filiais_tipo' não encontrada. Execute a migration V011.");
        exigir(constraintExiste("acesso.usuarios", "CK_usuarios_escopo_filiais_tipo"), "Constraint 'CK_usuarios_escopo_filiais_tipo' não encontrada.");
        exigir(tabelaExiste("acesso.usuario_filiais_permitidas"), "Tabela 'acesso.usuario_filiais_permitidas' não encontrada. Execute a migration V011.");
        exigir(colunaExiste("acesso.usuario_filiais_permitidas", "usuario_id"), "Coluna 'acesso.usuario_filiais_permitidas.usuario_id' não encontrada.");
        exigir(colunaExiste("acesso.usuario_filiais_permitidas", "filial_nome"), "Coluna 'acesso.usuario_filiais_permitidas.filial_nome' não encontrada.");
        log.info("Schema de escopo de filiais por usuário validado.");
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

    private void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new IllegalStateException(mensagem);
        }
    }
}
