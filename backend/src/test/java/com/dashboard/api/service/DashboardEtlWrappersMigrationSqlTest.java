package com.dashboard.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DashboardEtlWrappersMigrationSqlTest {
    private static final Pattern MOJIBAKE_PATTERN = Pattern.compile(
            "\\x{00C3}[\\x{0080}-\\x{00BF}\\x{0192}\\x{201A}\\x{00A2}]|"
                    + "\\x{00C2}[\\x{0080}-\\x{00BF}]|\\x{FFFD}"
    );

    @Test
    void migrationV020NaoDeveCriarWrappersDeViewsDaEtl() throws IOException {
        String sql = lerMigration("V020__sincronizar_wrappers_views_etl_dashboard.sql");

        assertThat(sql).contains("No-op intencional");
        assertThat(sql).contains("nao cria wrappers locais");
        assertThat(sql).doesNotContain("ETL_SISTEMA");
        assertThat(sql).doesNotContain("CREATE OR ALTER VIEW");
        assertThat(sql).doesNotContain("sp_refreshview");
        assertThat(sql).doesNotContain("ALTER TABLE");
        assertThat(sql).doesNotContain("DROP VIEW");
        assertThat(MOJIBAKE_PATTERN.matcher(sql).find()).isFalse();
    }

    @Test
    void migrationV020DeveExistirNoCatalogoDatabaseUnificado() throws IOException {
        String sql = lerMigration("V020__sincronizar_wrappers_views_etl_dashboard.sql");

        assertThat(sql).isNotBlank();
    }

    @Test
    void migrationV027DeveSubstituirWrappersPorSynonyms() throws IOException {
        String sql = lerMigration("V027__substituir_wrappers_etl_por_synonyms.sql");

        assertThat(sql).contains("DROP VIEW dbo.vw_manifestos_powerbi");
        assertThat(sql).contains("DROP SYNONYM dbo.vw_manifestos_powerbi");
        assertThat(sql).contains("CREATE SYNONYM dbo.vw_manifestos_powerbi FOR ETL_SISTEMA.dbo.vw_manifestos_powerbi");
        assertThat(sql).contains("CREATE SYNONYM dbo.fato_fretes_faturamento FOR ETL_SISTEMA.dbo.fato_fretes_faturamento");
        assertThat(sql).doesNotContain("CREATE OR ALTER VIEW");
        assertThat(sql).doesNotContain("SELECT * FROM ETL_SISTEMA");
        assertThat(MOJIBAKE_PATTERN.matcher(sql).find()).isFalse();
    }

    @Test
    void migrationV029DevePublicarSynonymDaFatoManifestos() throws IOException {
        String sql = lerMigration("V029__sincronizar_fato_manifestos_etl.sql");

        assertThat(sql).contains("DROP VIEW dbo.vw_fato_manifestos_dash");
        assertThat(sql).contains("DROP SYNONYM dbo.vw_fato_manifestos_dash");
        assertThat(sql).contains("CREATE SYNONYM dbo.vw_fato_manifestos_dash FOR ETL_SISTEMA.dbo.vw_fato_manifestos_dash");
        assertThat(sql).doesNotContain("CREATE OR ALTER VIEW");
        assertThat(sql).doesNotContain("SELECT * FROM ETL_SISTEMA");
        assertThat(MOJIBAKE_PATTERN.matcher(sql).find()).isFalse();
    }

    private String lerMigration(String arquivo) throws IOException {
        return lerSql(Path.of("..", "database", "migrations", arquivo));
    }

    private String lerSql(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
