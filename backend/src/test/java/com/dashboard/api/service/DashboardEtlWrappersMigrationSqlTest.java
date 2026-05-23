package com.dashboard.api.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardEtlWrappersMigrationSqlTest {
    private static final Pattern MOJIBAKE_PATTERN = Pattern.compile(
            "\\x{00C3}[\\x{0080}-\\x{00BF}\\x{0192}\\x{201A}\\x{00A2}]|"
                    + "\\x{00C2}[\\x{0080}-\\x{00BF}]|\\x{FFFD}"
    );

    @Test
    void migrationV020DeveCriarWrappersDasViewsPublicadasPelaEtl() throws IOException {
        String sql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V020__sincronizar_wrappers_views_etl_dashboard.sql"));

        List<String> wrappers = List.of(
                "vw_bi_monitoramento",
                "vw_coletas_powerbi",
                "vw_contas_a_pagar_powerbi",
                "vw_cotacoes_powerbi",
                "vw_dim_clientes",
                "vw_dim_filiais",
                "vw_dim_motoristas",
                "vw_dim_planocontas",
                "vw_dim_usuarios",
                "vw_dim_veiculos",
                "vw_faturas_graphql_powerbi",
                "vw_inventario_powerbi",
                "vw_manifestos_powerbi",
                "vw_sinistros_powerbi"
        );

        assertThat(sql).contains("DB_ID(N'ETL_SISTEMA')");
        assertThat(sql).contains("CREATE OR ALTER VIEW dbo.");
        assertThat(sql).contains("FROM [ETL_SISTEMA].dbo.");
        assertThat(sql).contains("EXEC sys.sp_refreshview");
        assertThat(sql).contains("ETL_SISTEMA.dbo.vw_coletas_powerbi.[Solicitacao]");
        assertThat(sql).contains("tipo de data nativo");
        assertThat(sql).contains("N'date'");
        assertThat(sql).contains("N'datetime2'");
        assertThat(sql).contains("nao use conversao dinamica no Dashboard");
        assertThat(sql).contains(wrappers);
        assertThat(sql).doesNotContain("ALTER TABLE");
        assertThat(sql).doesNotContain("DROP VIEW");
        assertThat(MOJIBAKE_PATTERN.matcher(sql).find()).isFalse();
    }

    @Test
    void migrationV020DoBackendDeveFicarIgualAoCatalogoDatabase() throws IOException {
        String backendSql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V020__sincronizar_wrappers_views_etl_dashboard.sql"));
        String catalogoSql = lerSql(Path.of("..", "databases", "DASHBOARDS", "migrations",
                "V020__sincronizar_wrappers_views_etl_dashboard.sql"));

        assertThat(backendSql).isEqualTo(catalogoSql);
    }

    private String lerSql(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
