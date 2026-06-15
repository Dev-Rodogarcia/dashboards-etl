package com.dashboard.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ManifestosCostGoalMigrationSqlTest {

    @Test
    void migrationV028CriaContratoDeMetasDeCustoNoDashboard() throws IOException {
        String sql = lerSql(Path.of(
                "..",
                "database",
                "migrations",
                "V028__criar_metas_custo_manifestos.sql"
        ));

        assertThat(sql)
                .contains("CREATE TABLE acesso.manifestos_cost_goals")
                .contains("branch_id NVARCHAR(120) NULL")
                .contains("year_month DATE NOT NULL")
                .contains("cost_goal DECIMAL(18,2) NOT NULL")
                .contains("UX_manifestos_cost_goals_branch_period")
                .contains("UX_manifestos_cost_goals_global_period")
                .doesNotContain("ETL_SISTEMA");
    }

    @Test
    void migrationV045AdicionaAutorDaUltimaAlteracao() throws IOException {
        String sql = lerSql(Path.of(
                "..",
                "database",
                "migrations",
                "V045__adicionar_autor_metas_manifestos.sql"
        ));

        assertThat(sql)
                .contains("ALTER TABLE acesso.manifestos_cost_goals")
                .contains("ADD updated_by_user_id BIGINT NULL")
                .doesNotContain("ETL_SISTEMA");
    }

    @Test
    void validacaoDeSchemaCobreTabelaEIndices() throws IOException {
        String sql = lerSql(Path.of(
                "..",
                "database",
                "validation",
                "002_validar_metas_custo_manifestos.sql"
        ));

        assertThat(sql)
                .contains("acesso.manifestos_cost_goals")
                .contains("updated_by_user_id")
                .contains("UX_manifestos_cost_goals_branch_period")
                .contains("UX_manifestos_cost_goals_global_period");
    }

    private String lerSql(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
