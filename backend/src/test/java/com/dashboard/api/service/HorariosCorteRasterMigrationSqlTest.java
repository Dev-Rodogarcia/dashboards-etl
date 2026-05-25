package com.dashboard.api.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class HorariosCorteRasterMigrationSqlTest {
    private static final Pattern MOJIBAKE_PATTERN = Pattern.compile(
            "\\x{00C3}[\\x{0080}-\\x{00BF}\\x{0192}\\x{201A}\\x{00A2}]|"
                    + "\\x{00C2}[\\x{0080}-\\x{00BF}]|\\x{FFFD}"
    );

    @Test
    void migrationV013NaoDeveConsumirRasterDaEtl() throws IOException {
        String sql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V013__horarios_corte_consumir_raster_etl.sql"));

        assertThat(sql).contains("No-op intencional");
        assertThat(sql).contains("nao cria views baseadas em tabelas Raster do ETL");
        assertThat(sql).doesNotContain("ETL_SISTEMA");
        assertThat(sql).doesNotContain("CREATE OR ALTER VIEW");
        assertThat(sql).doesNotContain("THROW");
        assertThat(MOJIBAKE_PATTERN.matcher(sql).find()).isFalse();
    }

    @Test
    void migrationV013DevePreservarFronteiraSemMojibake() throws IOException {
        String sql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V013__horarios_corte_consumir_raster_etl.sql"));

        assertThat(sql).contains("contratos publicados sob ownership do Dashboard");
        assertThat(sql).doesNotContain("raster_viagens");
        assertThat(sql).doesNotContain("raster_viagem_paradas");
        assertThat(MOJIBAKE_PATTERN.matcher(sql).find()).isFalse();
    }

    @Test
    void migrationV013NaoDeveConterRegrasOperacionaisRaster() throws IOException {
        String sql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V013__horarios_corte_consumir_raster_etl.sql"));

        assertThat(sql).doesNotContain("data_hora_prev_ini");
        assertThat(sql).doesNotContain("data_hora_real_ini_at <= corte_at");
        assertThat(sql).doesNotContain("data_corte_base_at");
    }

    @Test
    void migrationV013DoBackendDeveFicarIgualAoCatalogoDatabase() throws IOException {
        String backendSql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V013__horarios_corte_consumir_raster_etl.sql"));
        String catalogoSql = lerSql(Path.of("..", "databases", "DASHBOARDS", "migrations",
                "V013__horarios_corte_consumir_raster_etl.sql"));

        assertThat(backendSql).isEqualTo(catalogoSql);
    }

    private String lerSql(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
