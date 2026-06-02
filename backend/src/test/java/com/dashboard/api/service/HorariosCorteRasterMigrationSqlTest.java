package com.dashboard.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HorariosCorteRasterMigrationSqlTest {
    private static final Pattern MOJIBAKE_PATTERN = Pattern.compile(
            "\\x{00C3}[\\x{0080}-\\x{00BF}\\x{0192}\\x{201A}\\x{00A2}]|"
                    + "\\x{00C2}[\\x{0080}-\\x{00BF}]|\\x{FFFD}"
    );

    @Test
    void migrationV013NaoDeveConsumirRasterDaEtl() throws IOException {
        String sql = lerMigration("V013__horarios_corte_consumir_raster_etl.sql");

        assertThat(sql).contains("No-op intencional");
        assertThat(sql).contains("nao cria views baseadas em tabelas Raster do ETL");
        assertThat(sql).doesNotContain("ETL_SISTEMA");
        assertThat(sql).doesNotContain("CREATE OR ALTER VIEW");
        assertThat(sql).doesNotContain("THROW");
        assertThat(MOJIBAKE_PATTERN.matcher(sql).find()).isFalse();
    }

    @Test
    void migrationV013DevePreservarFronteiraSemMojibake() throws IOException {
        String sql = lerMigration("V013__horarios_corte_consumir_raster_etl.sql");

        assertThat(sql).contains("contratos publicados sob ownership do Dashboard");
        assertThat(sql).doesNotContain("raster_viagens");
        assertThat(sql).doesNotContain("raster_viagem_paradas");
        assertThat(MOJIBAKE_PATTERN.matcher(sql).find()).isFalse();
    }

    @Test
    void migrationV013NaoDeveConterRegrasOperacionaisRaster() throws IOException {
        String sql = lerMigration("V013__horarios_corte_consumir_raster_etl.sql");

        assertThat(sql).doesNotContain("data_hora_prev_ini");
        assertThat(sql).doesNotContain("data_hora_real_ini_at <= corte_at");
        assertThat(sql).doesNotContain("data_corte_base_at");
    }

    @Test
    void migrationV024DeveAncorarCorteNaDataOperacaoSemViradaPorInicio() throws IOException {
        String sql = lerMigration("V024__corrigir_sla_horario_corte.sql");

        assertThat(sql).contains("DATEADD(SECOND, DATEDIFF(SECOND, CAST(''00:00:00'' AS TIME(0)), hc.corte), CAST(hc.data_operacao AS DATETIME2(0)))");
        assertThat(sql).contains("WHEN sm_gerada_at <= corte_at THEN CAST(1 AS BIT)");
        assertThat(sql).doesNotContain("hc.corte < hc.inicio");
        assertThat(MOJIBAKE_PATTERN.matcher(sql).find()).isFalse();
    }

    @Test
    void migrationV013DeveExistirNoCatalogoDatabaseUnificado() throws IOException {
        String sql = lerMigration("V013__horarios_corte_consumir_raster_etl.sql");

        assertThat(sql).isNotBlank();
    }

    @Test
    void migrationV024DeveExistirNoCatalogoDatabaseUnificado() throws IOException {
        String sql = lerMigration("V024__corrigir_sla_horario_corte.sql");

        assertThat(sql).isNotBlank();
    }

    private String lerMigration(String arquivo) throws IOException {
        return lerSql(Path.of("..", "database", "migrations", arquivo));
    }

    private String lerSql(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
