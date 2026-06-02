package com.dashboard.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TrackingMigrationSqlTest {
    private static final Pattern MOJIBAKE_PATTERN = Pattern.compile(
            "\\x{00C3}[\\x{0080}-\\x{00BF}\\x{0192}\\x{201A}\\x{00A2}]|"
                    + "\\x{00C2}[\\x{0080}-\\x{00BF}]|\\x{FFFD}"
    );

    @Test
    void migrationV019NaoDeveCriarWrapperDeLocalizacaoDaEtl() throws IOException {
        String sql = lerMigration("V019__sincronizar_view_localizacao_cargas_etl.sql");

        assertThat(sql).contains("No-op intencional");
        assertThat(sql).contains("nao cria nem sincroniza wrappers");
        assertThat(sql).doesNotContain("ETL_SISTEMA");
        assertThat(sql).doesNotContain("CREATE OR ALTER VIEW");
        assertThat(sql).doesNotContain("sp_refreshview");
        assertThat(sql).doesNotContain("ALTER TABLE");
        assertThat(MOJIBAKE_PATTERN.matcher(sql).find()).isFalse();
    }

    @Test
    void migrationV019DeveExistirNoCatalogoDatabaseUnificado() throws IOException {
        String sql = lerMigration("V019__sincronizar_view_localizacao_cargas_etl.sql");

        assertThat(sql).isNotBlank();
    }

    private String lerMigration(String arquivo) throws IOException {
        return lerSql(Path.of("..", "database", "migrations", arquivo));
    }

    private String lerSql(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
