package com.dashboard.api.service.acesso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
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
        try {
            if (!schemaExiste("acesso")) {
                log.warn("Schema 'acesso' não encontrado. Bootstrap de metas KPI não executado.");
                return;
            }

            if (!tabelaExiste("acesso.usuarios")) {
                log.warn("Tabela 'acesso.usuarios' não encontrada. Bootstrap de metas KPI não executado.");
                return;
            }

            garantirTabelaMetas();
            garantirTabelaHistorico();
        } catch (DataAccessException ex) {
            log.warn(
                    "Bootstrap de metas KPI não conseguiu aplicar DDL automaticamente. Execute as migrations V014/V015 com um usuário de banco com permissão de ALTER/CREATE. Motivo: {}",
                    ex.getMessage()
            );
        }
    }

    private void garantirTabelaMetas() {
        if (!tabelaExiste("acesso.kpi_goals")) {
            jdbcTemplate.execute("""
                CREATE TABLE acesso.kpi_goals (
                    id                 BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                    branch_id          NVARCHAR(120)        NULL,
                    indicator_key      VARCHAR(60)          NOT NULL,
                    goal_value         DECIMAL(9,3)         NOT NULL,
                    created_at         DATETIME2(0)         NOT NULL
                        CONSTRAINT DF_kpi_goals_created_at DEFAULT SYSUTCDATETIME(),
                    updated_by_user_id BIGINT               NULL REFERENCES acesso.usuarios(id),
                    updated_at         DATETIME2(0)         NOT NULL
                        CONSTRAINT DF_kpi_goals_updated_at DEFAULT SYSUTCDATETIME(),
                    CONSTRAINT UQ_kpi_goals_branch_indicator UNIQUE (branch_id, indicator_key),
                    CONSTRAINT CK_kpi_goals_indicator_key CHECK (
                        indicator_key IN (
                            'delivery_performance',
                            'collector_usage',
                            'cargo_cubage',
                            'cargo_indemnity',
                            'cutoff_time'
                        )
                    ),
                    CONSTRAINT CK_kpi_goals_goal_value CHECK (goal_value >= 0 AND goal_value <= 100)
                )
                """);
            log.info("Tabela 'acesso.kpi_goals' criada automaticamente.");
        }

        boolean precisaAjustarBranchIdNull = !colunaPermiteNull("acesso.kpi_goals", "branch_id");

        if (precisaAjustarBranchIdNull && uniqueConstraintExiste("acesso.kpi_goals", "UQ_kpi_goals_branch_indicator")) {
            jdbcTemplate.execute("""
                ALTER TABLE acesso.kpi_goals
                DROP CONSTRAINT UQ_kpi_goals_branch_indicator
                """);
        }

        if (!colunaExiste("acesso.kpi_goals", "created_at")) {
            jdbcTemplate.execute("""
                ALTER TABLE acesso.kpi_goals
                ADD created_at DATETIME2(0) NOT NULL
                    CONSTRAINT DF_kpi_goals_created_at DEFAULT SYSUTCDATETIME()
                """);
            log.info("Coluna 'acesso.kpi_goals.created_at' criada automaticamente.");
        }

        if (!colunaExiste("acesso.kpi_goals", "updated_at")) {
            jdbcTemplate.execute("""
                ALTER TABLE acesso.kpi_goals
                ADD updated_at DATETIME2(0) NOT NULL
                    CONSTRAINT DF_kpi_goals_updated_at DEFAULT SYSUTCDATETIME()
                """);
            log.info("Coluna 'acesso.kpi_goals.updated_at' criada automaticamente.");
        }

        if (!colunaExiste("acesso.kpi_goals", "updated_by_user_id")) {
            jdbcTemplate.execute("""
                ALTER TABLE acesso.kpi_goals
                ADD updated_by_user_id BIGINT NULL REFERENCES acesso.usuarios(id)
                """);
            log.info("Coluna 'acesso.kpi_goals.updated_by_user_id' criada automaticamente.");
        }

        if (precisaAjustarBranchIdNull) {
            jdbcTemplate.execute("""
                ALTER TABLE acesso.kpi_goals
                ALTER COLUMN branch_id NVARCHAR(120) NULL
                """);
            log.info("Coluna 'acesso.kpi_goals.branch_id' ajustada para aceitar GLOBAL como NULL.");
        }

        jdbcTemplate.update("""
            DELETE FROM acesso.kpi_goals
            WHERE indicator_key NOT IN (
                'delivery_performance',
                'collector_usage',
                'cargo_cubage',
                'cargo_indemnity',
                'cutoff_time'
            )
            """);
        jdbcTemplate.update("""
            UPDATE acesso.kpi_goals
            SET branch_id = NULL
            WHERE UPPER(branch_id) = 'GLOBAL'
            """);
        jdbcTemplate.update("""
            ;WITH duplicadas AS (
                SELECT
                    id,
                    ROW_NUMBER() OVER (
                        PARTITION BY COALESCE(branch_id, N'__GLOBAL__'), indicator_key
                        ORDER BY updated_at DESC, id DESC
                    ) AS rn
                FROM acesso.kpi_goals
            )
            DELETE FROM duplicadas
            WHERE rn > 1
            """);

        if (!uniqueConstraintExiste("acesso.kpi_goals", "UQ_kpi_goals_branch_indicator")) {
            jdbcTemplate.execute("""
                ALTER TABLE acesso.kpi_goals
                ADD CONSTRAINT UQ_kpi_goals_branch_indicator UNIQUE (branch_id, indicator_key)
                """);
            log.info("Constraint 'UQ_kpi_goals_branch_indicator' criada automaticamente.");
        }

        if (!checkConstraintExiste("acesso.kpi_goals", "CK_kpi_goals_indicator_key")) {
            jdbcTemplate.execute("""
                ALTER TABLE acesso.kpi_goals
                WITH CHECK ADD CONSTRAINT CK_kpi_goals_indicator_key CHECK (
                    indicator_key IN (
                        'delivery_performance',
                        'collector_usage',
                        'cargo_cubage',
                        'cargo_indemnity',
                        'cutoff_time'
                    )
                )
                """);
        }

        if (!checkConstraintExiste("acesso.kpi_goals", "CK_kpi_goals_goal_value")) {
            jdbcTemplate.execute("""
                ALTER TABLE acesso.kpi_goals
                WITH CHECK ADD CONSTRAINT CK_kpi_goals_goal_value CHECK (goal_value >= 0 AND goal_value <= 100)
                """);
        }

        if (!indiceExiste("acesso.kpi_goals", "IX_kpi_goals_branch_id")) {
            jdbcTemplate.execute("""
                CREATE INDEX IX_kpi_goals_branch_id
                ON acesso.kpi_goals (branch_id)
                """);
        }
    }

    private void garantirTabelaHistorico() {
        if (!tabelaExiste("acesso.kpi_goals_history")) {
            jdbcTemplate.execute("""
                CREATE TABLE acesso.kpi_goals_history (
                    id                 BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                    branch_id          NVARCHAR(120)        NULL,
                    indicator_key      VARCHAR(60)          NOT NULL,
                    old_value          DECIMAL(9,3)         NULL,
                    new_value          DECIMAL(9,3)         NULL,
                    updated_by_user_id BIGINT               NULL REFERENCES acesso.usuarios(id),
                    updated_at         DATETIME2(0)         NOT NULL
                        CONSTRAINT DF_kpi_goals_history_updated_at DEFAULT SYSUTCDATETIME(),
                    action             VARCHAR(40)          NOT NULL,
                    CONSTRAINT CK_kpi_goals_history_indicator_key CHECK (
                        indicator_key IN (
                            'delivery_performance',
                            'collector_usage',
                            'cargo_cubage',
                            'cargo_indemnity',
                            'cutoff_time'
                        )
                    ),
                    CONSTRAINT CK_kpi_goals_history_action CHECK (
                        action IN ('GLOBAL_UPDATE', 'BRANCH_UPDATE', 'BRANCH_OVERRIDE_REMOVED')
                    )
                )
                """);
            log.info("Tabela 'acesso.kpi_goals_history' criada automaticamente.");
        }

        if (!indiceExiste("acesso.kpi_goals_history", "IX_kpi_goals_history_branch_updated_at")) {
            jdbcTemplate.execute("""
                CREATE INDEX IX_kpi_goals_history_branch_updated_at
                ON acesso.kpi_goals_history (branch_id, updated_at DESC)
                """);
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
}
