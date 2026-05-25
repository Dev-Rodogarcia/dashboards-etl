package com.dashboard.api.service.acesso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class HomeComunicadosSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HomeComunicadosSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public HomeComunicadosSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        exigir(schemaExiste("acesso"), "Schema 'acesso' não encontrado. Execute as migrations Flyway antes de iniciar a API.");
        exigir(tabelaExiste("acesso.home_comunicados"), "Tabela 'acesso.home_comunicados' não encontrada. Execute a migration V012.");
        exigir(colunaExiste("acesso.home_comunicados", "id"), "Coluna 'acesso.home_comunicados.id' não encontrada.");
        exigir(colunaExiste("acesso.home_comunicados", "titulo"), "Coluna 'acesso.home_comunicados.titulo' não encontrada.");
        exigir(colunaExiste("acesso.home_comunicados", "corpo"), "Coluna 'acesso.home_comunicados.corpo' não encontrada.");
        exigir(colunaExiste("acesso.home_comunicados", "tag"), "Coluna 'acesso.home_comunicados.tag' não encontrada.");
        exigir(colunaExiste("acesso.home_comunicados", "publico_alvo"), "Coluna 'acesso.home_comunicados.publico_alvo' não encontrada.");
        exigir(colunaExiste("acesso.home_comunicados", "publicado_em"), "Coluna 'acesso.home_comunicados.publicado_em' não encontrada.");
        exigir(colunaExiste("acesso.home_comunicados", "ativo"), "Coluna 'acesso.home_comunicados.ativo' não encontrada.");
        exigir(colunaExiste("acesso.home_comunicados", "criado_por"), "Coluna 'acesso.home_comunicados.criado_por' não encontrada.");
        exigir(colunaExiste("acesso.home_comunicados", "criado_em"), "Coluna 'acesso.home_comunicados.criado_em' não encontrada.");
        exigir(colunaExiste("acesso.home_comunicados", "atualizado_por"), "Coluna 'acesso.home_comunicados.atualizado_por' não encontrada.");
        exigir(colunaExiste("acesso.home_comunicados", "atualizado_em"), "Coluna 'acesso.home_comunicados.atualizado_em' não encontrada.");
        exigir(constraintExiste("acesso.home_comunicados", "CK_home_comunicados_tag"), "Constraint 'CK_home_comunicados_tag' não encontrada.");
        exigir(indiceExiste("acesso.home_comunicados", "IX_home_comunicados_ativo_publicado"), "Índice 'IX_home_comunicados_ativo_publicado' não encontrado.");
        log.info("Schema de comunicados da Home validado.");
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
