package com.dashboard.api.service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SchemaGovernanceMigrationSqlTest {

    private static final Pattern DDL_PATTERN = Pattern.compile(
            "\\b(CREATE\\s+TABLE|ALTER\\s+TABLE|CREATE\\s+(?:UNIQUE\\s+)?INDEX|DROP\\s+INDEX|DROP\\s+CONSTRAINT)\\b",
            Pattern.CASE_INSENSITIVE
    );

    @Test
    void migrationV023CorrigeDriftDeConfiguracoesSegurancaEIndicesFiltrados() throws IOException {
        String sql = lerMigration("V023__corrigir_drift_schema_acesso.sql");

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
    void migrationV023DeveExistirNoCatalogoDatabaseUnificado() throws IOException {
        String sql = lerMigration("V023__corrigir_drift_schema_acesso.sql");

        assertThat(sql).isNotBlank();
    }

    @Test
    void inicializadoresDeSchemaDevemPermanecerForaDoRuntime() {
        List<Path> arquivos = List.of(
                Path.of("src", "main", "java", "com", "dashboard", "api", "config", "acesso", "KpiGoalsSchemaInitializer.java"),
                Path.of("src", "main", "java", "com", "dashboard", "api", "config", "acesso", "RefreshTokenSchemaInitializer.java"),
                Path.of("src", "main", "java", "com", "dashboard", "api", "config", "acesso", "HomeComunicadosSchemaInitializer.java"),
                Path.of("src", "main", "java", "com", "dashboard", "api", "config", "acesso", "EscopoFiliaisUsuarioSchemaInitializer.java"),
                Path.of("src", "main", "java", "com", "dashboard", "api", "service", "acesso", "KpiGoalsSchemaInitializer.java"),
                Path.of("src", "main", "java", "com", "dashboard", "api", "service", "acesso", "RefreshTokenSchemaInitializer.java"),
                Path.of("src", "main", "java", "com", "dashboard", "api", "service", "acesso", "HomeComunicadosSchemaInitializer.java"),
                Path.of("src", "main", "java", "com", "dashboard", "api", "service", "acesso", "EscopoFiliaisUsuarioSchemaInitializer.java")
        );

        for (Path arquivo : arquivos) {
            assertThat(Files.exists(arquivo))
                    .as("Inicializador de schema em runtime: " + arquivo)
                    .isFalse();
        }
    }

    @Test
    void codigoJavaNaoDeveExecutarDdlEstruturalNoRuntime() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java");
        try (var arquivos = Files.walk(sourceRoot)) {
            List<Path> fontes = arquivos
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path arquivo : fontes) {
                String source = lerSql(arquivo);
                assertThat(DDL_PATTERN.matcher(source).find())
                        .as("DDL runtime em " + arquivo)
                        .isFalse();
            }
        }
    }

    private String lerSql(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private String lerMigration(String arquivo) throws IOException {
        return lerSql(Path.of("..", "database", "migrations", arquivo));
    }
}
