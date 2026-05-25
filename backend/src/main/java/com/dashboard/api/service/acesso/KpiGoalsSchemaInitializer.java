package com.dashboard.api.service.acesso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(6)
public class KpiGoalsSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KpiGoalsSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public KpiGoalsSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        garantirSchema();
    }

    public void garantirSchema() {
        exigir(schemaExiste("acesso"), "Schema 'acesso' não encontrado. Execute as migrations Flyway antes de iniciar a API.");
        exigir(tabelaExiste("acesso.usuarios"), "Tabela 'acesso.usuarios' não encontrada. Execute as migrations Flyway antes de iniciar a API.");

        validarTabelaMetas();
        validarTabelaHistorico();
        log.info("Schema de metas KPI validado.");
    }

    private void validarTabelaMetas() {
        exigir(tabelaExiste("acesso.kpi_goals"), "Tabela 'acesso.kpi_goals' não encontrada. Execute as migrations V014/V015.");
        exigir(colunaExiste("acesso.kpi_goals", "id"), "Coluna 'acesso.kpi_goals.id' não encontrada.");
        exigir(colunaExiste("acesso.kpi_goals", "branch_id"), "Coluna 'acesso.kpi_goals.branch_id' não encontrada.");
        exigir(colunaPermiteNull("acesso.kpi_goals", "branch_id"), "Coluna 'acesso.kpi_goals.branch_id' deve aceitar NULL para metas globais.");
        exigir(colunaExiste("acesso.kpi_goals", "indicator_key"), "Coluna 'acesso.kpi_goals.indicator_key' não encontrada.");
        exigir(colunaExiste("acesso.kpi_goals", "goal_value"), "Coluna 'acesso.kpi_goals.goal_value' não encontrada.");
        exigir(colunaExiste("acesso.kpi_goals", "created_at"), "Coluna 'acesso.kpi_goals.created_at' não encontrada.");
        exigir(colunaExiste("acesso.kpi_goals", "updated_at"), "Coluna 'acesso.kpi_goals.updated_at' não encontrada.");
        exigir(colunaExiste("acesso.kpi_goals", "updated_by_user_id"), "Coluna 'acesso.kpi_goals.updated_by_user_id' não encontrada.");
        exigir(uniqueConstraintExiste("acesso.kpi_goals", "UQ_kpi_goals_branch_indicator"), "Constraint 'UQ_kpi_goals_branch_indicator' não encontrada.");
        exigir(checkConstraintExiste("acesso.kpi_goals", "CK_kpi_goals_indicator_key"), "Constraint 'CK_kpi_goals_indicator_key' não encontrada.");
        exigir(checkConstraintExiste("acesso.kpi_goals", "CK_kpi_goals_goal_value"), "Constraint 'CK_kpi_goals_goal_value' não encontrada.");
        exigir(indiceExiste("acesso.kpi_goals", "IX_kpi_goals_branch_id"), "Índice 'IX_kpi_goals_branch_id' não encontrado.");
    }

    private void validarTabelaHistorico() {
        exigir(tabelaExiste("acesso.kpi_goals_history"), "Tabela 'acesso.kpi_goals_history' não encontrada. Execute as migrations V014/V015.");
        exigir(colunaExiste("acesso.kpi_goals_history", "id"), "Coluna 'acesso.kpi_goals_history.id' não encontrada.");
        exigir(colunaExiste("acesso.kpi_goals_history", "branch_id"), "Coluna 'acesso.kpi_goals_history.branch_id' não encontrada.");
        exigir(colunaExiste("acesso.kpi_goals_history", "indicator_key"), "Coluna 'acesso.kpi_goals_history.indicator_key' não encontrada.");
        exigir(colunaExiste("acesso.kpi_goals_history", "old_value"), "Coluna 'acesso.kpi_goals_history.old_value' não encontrada.");
        exigir(colunaExiste("acesso.kpi_goals_history", "new_value"), "Coluna 'acesso.kpi_goals_history.new_value' não encontrada.");
        exigir(colunaExiste("acesso.kpi_goals_history", "updated_by_user_id"), "Coluna 'acesso.kpi_goals_history.updated_by_user_id' não encontrada.");
        exigir(colunaExiste("acesso.kpi_goals_history", "updated_at"), "Coluna 'acesso.kpi_goals_history.updated_at' não encontrada.");
        exigir(colunaExiste("acesso.kpi_goals_history", "action"), "Coluna 'acesso.kpi_goals_history.action' não encontrada.");
        exigir(checkConstraintExiste("acesso.kpi_goals_history", "CK_kpi_goals_history_indicator_key"), "Constraint 'CK_kpi_goals_history_indicator_key' não encontrada.");
        exigir(checkConstraintExiste("acesso.kpi_goals_history", "CK_kpi_goals_history_action"), "Constraint 'CK_kpi_goals_history_action' não encontrada.");
        exigir(indiceExiste("acesso.kpi_goals_history", "IX_kpi_goals_history_branch_updated_at"), "Índice 'IX_kpi_goals_history_branch_updated_at' não encontrado.");
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

    private boolean colunaPermiteNull(String nomeCompletoTabela, String nomeColuna) {
        Integer permiteNull = jdbcTemplate.queryForObject(
                "SELECT COLUMNPROPERTY(OBJECT_ID(?, 'U'), ?, 'AllowsNull')",
                Integer.class,
                nomeCompletoTabela,
                nomeColuna
        );
        return permiteNull != null && permiteNull == 1;
    }

    private boolean uniqueConstraintExiste(String nomeCompletoTabela, String nomeConstraint) {
        Integer total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM sys.key_constraints
                WHERE name = ?
                  AND parent_object_id = OBJECT_ID(?, 'U')
                """,
                Integer.class,
                nomeConstraint,
                nomeCompletoTabela
        );
        return total != null && total > 0;
    }

    private boolean checkConstraintExiste(String nomeCompletoTabela, String nomeConstraint) {
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
