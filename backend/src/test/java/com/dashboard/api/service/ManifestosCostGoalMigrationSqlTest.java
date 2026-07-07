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
    void migrationV046AdicionaTipoContratoNasMetasDeManifestos() throws IOException {
        String sql = lerSql(Path.of(
                "..",
                "database",
                "migrations",
                "V046__adicionar_tipo_contrato_metas_manifestos.sql"
        ));

        assertThat(sql)
                .contains("ADD contract_type NVARCHAR(100) NULL")
                .contains("ADD contract_type_key NVARCHAR(100) NULL")
                .contains("contract_type = N'Geral'")
                .contains("ALTER COLUMN contract_type_key NVARCHAR(100) NOT NULL")
                .contains("DROP INDEX UX_manifestos_cost_goals_branch_period")
                .contains("DROP INDEX UX_manifestos_cost_goals_global_period")
                .contains("UX_manifestos_cost_goals_branch_period_contract")
                .contains("UX_manifestos_cost_goals_global_period_contract")
                .doesNotContain("ETL_SISTEMA");
    }

    @Test
    void migrationV048AdicionaClassificacaoNasMetasDeManifestos() throws IOException {
        String sql = lerSql(Path.of(
                "..",
                "database",
                "migrations",
                "V048__adicionar_classificacao_metas_manifestos.sql"
        ));

        assertThat(sql)
                .contains("ADD classification_key VARCHAR(120) NULL")
                .contains("DROP INDEX UX_manifestos_cost_goals_branch_period_contract")
                .contains("DROP INDEX UX_manifestos_cost_goals_global_period_contract")
                .contains("UX_manifestos_cost_goals_branch_period_contract_classification")
                .contains("ON acesso.manifestos_cost_goals (branch_id, year_month, contract_type_key, classification_key)")
                .contains("UX_manifestos_cost_goals_global_period_contract_classification")
                .contains("ON acesso.manifestos_cost_goals (year_month, contract_type_key, classification_key)")
                .doesNotContain("ETL_SISTEMA");
    }

    @Test
    void migrationV051NormalizaChavesDeMetasDeManifestos() throws IOException {
        String sql = lerSql(Path.of(
                "..",
                "database",
                "migrations",
                "V051__normalizar_chaves_metas_manifestos.sql"
        ));

        assertThat(sql)
                .contains("UPDATE acesso.manifestos_cost_goals")
                .contains("UPPER(LTRIM(RTRIM(branch_id)))")
                .contains("UPPER(LTRIM(RTRIM(contract_type_key)))")
                .contains("UPPER(LTRIM(RTRIM(classification_key)))")
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
                .contains("contract_type_key")
                .contains("classification_key")
                .contains("UX_manifestos_cost_goals_branch_period_contract_classification")
                .contains("UX_manifestos_cost_goals_global_period_contract_classification")
                .contains("Metas de manifestos possuem chaves fora do padrao canonico em caixa alta");
    }

    private String lerSql(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
