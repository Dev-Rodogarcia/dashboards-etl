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
    void migrationV019DeveUsarLocalizacaoAtualComoFallbackDaFilialAtual() throws IOException {
        String sql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V019__sincronizar_view_localizacao_cargas_etl.sql"));

        assertThat(sql).contains("CREATE OR ALTER VIEW dbo.vw_localizacao_cargas_powerbi");
        assertThat(sql).contains("FROM [ETL_SISTEMA].dbo.vw_localizacao_cargas_powerbi");
        assertThat(sql).contains("(N'Localização Atual')");
        assertThat(sql).contains("(N'Peso Taxado Decimal')");
        assertThat(sql).contains("(N'Valor NF Decimal')");
        assertThat(sql).contains("(N'Sigla Responsável Região Destino')");
        assertThat(sql).contains("(N'Status Normalizado')");
        assertThat(sql).contains("(N'Status Terminal')");
        assertThat(sql).contains("(N'Cancelado Flag')");
        assertThat(sql).contains("(N'Hash Localização')");
        assertThat(sql).contains("[Peso Taxado Decimal]");
        assertThat(sql).contains("[Valor NF Decimal]");
        assertThat(sql).contains("[Sigla Responsável Região Destino]");
        assertThat(sql).contains("[Status Normalizado]");
        assertThat(sql).contains("[Status Terminal]");
        assertThat(sql).contains("[Cancelado Flag]");
        assertThat(sql).contains("[Hash Localização]");
        assertThat(sql).contains("NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(4000), [Filial Atual]))), N'''')");
        assertThat(sql).contains("NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(4000), [Localização Atual]))), N'''')");
        assertThat(sql).contains(") AS [Filial Atual]");
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
