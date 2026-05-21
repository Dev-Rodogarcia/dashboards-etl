package com.dashboard.api.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class FretesMigrationSqlTest {
    private static final Pattern MOJIBAKE_PATTERN = Pattern.compile(
            "\\x{00C3}[\\x{0080}-\\x{00BF}\\x{0192}\\x{201A}\\x{00A2}]|"
                    + "\\x{00C2}[\\x{0080}-\\x{00BF}]|\\x{FFFD}"
    );

    @Test
    void migrationV018DeveAtualizarMetadataDoWrapperFretesSemAlterarEstruturaDaEtl() throws IOException {
        String sql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V018__atualizar_metadata_view_fretes_etl.sql"));

        assertThat(sql).contains("CREATE OR ALTER VIEW dbo.vw_fretes_powerbi");
        assertThat(sql).contains("FROM [ETL_SISTEMA].dbo.vw_fretes_powerbi");
        assertThat(sql).contains("EXEC sys.sp_refreshview N'dbo.vw_fretes_powerbi'");
        assertThat(sql).contains("data_referencia_faturamento");
        assertThat(sql).contains("is_elegivel_faturamento");
        assertThat(sql).contains("N'CT-e Emissão'");
        assertThat(sql).contains("N'Classificação'");
        assertThat(sql).contains("N'Nº Minuta'");
        assertThat(sql).doesNotContain("ALTER TABLE");
        assertThat(sql).doesNotContain("dbo.fretes ADD");
        assertThat(MOJIBAKE_PATTERN.matcher(sql).find()).isFalse();
    }

    @Test
    void migrationV018DoBackendDeveFicarIgualAoCatalogoDatabase() throws IOException {
        String backendSql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V018__atualizar_metadata_view_fretes_etl.sql"));
        String catalogoSql = lerSql(Path.of("..", "databases", "DASHBOARDS", "migrations",
                "V018__atualizar_metadata_view_fretes_etl.sql"));

        assertThat(backendSql).isEqualTo(catalogoSql);
    }

    private String lerSql(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
