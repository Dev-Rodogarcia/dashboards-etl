package com.dashboard.api.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaGovernanceMigrationSqlTest {

    private static final Pattern DDL_PATTERN = Pattern.compile(
            "\\b(CREATE\\s+TABLE|ALTER\\s+TABLE|CREATE\\s+(?:UNIQUE\\s+)?INDEX|DROP\\s+INDEX|DROP\\s+CONSTRAINT)\\b",
            Pattern.CASE_INSENSITIVE
    );

    @Test
    void migrationV023CorrigeDriftDeConfiguracoesSegurancaEIndicesFiltrados() throws IOException {
        String sql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V023__corrigir_drift_schema_acesso.sql"));

        assertThat(sql).contains("CREATE TABLE acesso.configuracoes_seguranca");
        assertThat(sql).contains("CREATE UNIQUE INDEX UX_usuarios_chave_legado_not_null");
        assertThat(sql).contains("WHERE chave_legado IS NOT NULL");
        assertThat(sql).contains("CREATE UNIQUE INDEX UX_fretes_goals_branch_period");
        assertThat(sql).contains("ON acesso.fretes_goals (branch_id, ano, mes)");
        assertThat(sql).contains("WHERE branch_id IS NOT NULL");
        assertThat(sql).contains("CREATE UNIQUE INDEX UX_fretes_goals_global_period");
        assertThat(sql).contains("ON acesso.fretes_goals (ano, mes)");
        assertThat(sql).contains("WHERE branch_id IS NULL");
    }

    @Test
    void migrationV023DoBackendDeveFicarIgualAoCatalogoDatabase() throws IOException {
        String backendSql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V023__corrigir_drift_schema_acesso.sql"));
        String catalogoSql = lerSql(Path.of("..", "databases", "DASHBOARDS", "migrations",
                "V023__corrigir_drift_schema_acesso.sql"));

        assertThat(backendSql).isEqualTo(catalogoSql);
    }

    @Test
    void validadoresDeSchemaNaoDevemExecutarDdlNoRuntime() throws IOException {
        List<Path> arquivos = List.of(
                Path.of("src", "main", "java", "com", "dashboard", "api", "service", "acesso", "KpiGoalsSchemaInitializer.java"),
                Path.of("src", "main", "java", "com", "dashboard", "api", "service", "acesso", "RefreshTokenSchemaInitializer.java"),
                Path.of("src", "main", "java", "com", "dashboard", "api", "service", "acesso", "HomeComunicadosSchemaInitializer.java"),
                Path.of("src", "main", "java", "com", "dashboard", "api", "service", "acesso", "EscopoFiliaisUsuarioSchemaInitializer.java"),
                Path.of("src", "main", "java", "com", "dashboard", "api", "service", "FretesGoalService.java")
        );

        for (Path arquivo : arquivos) {
            String source = lerSql(arquivo);
            assertThat(DDL_PATTERN.matcher(source).find())
                    .as("DDL runtime em " + arquivo)
                    .isFalse();
        }
    }

    private String lerSql(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
