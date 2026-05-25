package com.dashboard.api.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingMigrationSqlTest {
    private static final Pattern MOJIBAKE_PATTERN = Pattern.compile(
            "\\x{00C3}[\\x{0080}-\\x{00BF}\\x{0192}\\x{201A}\\x{00A2}]|"
                    + "\\x{00C2}[\\x{0080}-\\x{00BF}]|\\x{FFFD}"
    );

    @Test
    void migrationV019NaoDeveCriarWrapperDeLocalizacaoDaEtl() throws IOException {
        String sql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V019__sincronizar_view_localizacao_cargas_etl.sql"));

        assertThat(sql).contains("No-op intencional");
        assertThat(sql).contains("nao cria nem sincroniza wrappers");
        assertThat(sql).doesNotContain("ETL_SISTEMA");
        assertThat(sql).doesNotContain("CREATE OR ALTER VIEW");
        assertThat(sql).doesNotContain("sp_refreshview");
        assertThat(sql).doesNotContain("ALTER TABLE");
        assertThat(MOJIBAKE_PATTERN.matcher(sql).find()).isFalse();
    }

    @Test
    void migrationV019DoBackendDeveFicarIgualAoCatalogoDatabase() throws IOException {
        String backendSql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V019__sincronizar_view_localizacao_cargas_etl.sql"));
        String catalogoSql = lerSql(Path.of("..", "databases", "DASHBOARDS", "migrations",
                "V019__sincronizar_view_localizacao_cargas_etl.sql"));

        assertThat(backendSql).isEqualTo(catalogoSql);
    }

    private String lerSql(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
