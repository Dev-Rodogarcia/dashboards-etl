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
    void migrationV013DeveTrocarHorariosCorteParaViewRasterDaEtl() throws IOException {
        String sql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V013__horarios_corte_consumir_raster_etl.sql"));

        assertThat(sql).contains("CREATE OR ALTER VIEW dbo.vw_horarios_corte_powerbi");
        assertThat(sql).contains("FROM [ETL_SISTEMA].dbo.vw_raster_sm_transit_time");
        assertThat(sql).contains("FROM ETL_SISTEMA.INFORMATION_SCHEMA.COLUMNS c");
        assertThat(sql).contains("THROW 51302");
        assertThat(sql).contains("AS [Saiu no Horário]");
        assertThat(sql).contains("AS [Atraso Minutos]");
        assertThat(sql).contains("N''Raster API - getEventoFimViagem'' AS [Nome do Arquivo]");
        assertThat(sql).contains("data_extracao_at AS [Data de extracao]");
    }

    @Test
    void migrationV013DeveConsumirCamposOperacionaisDaViewRasterSemMojibake() throws IOException {
        String sql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V013__horarios_corte_consumir_raster_etl.sql"));

        assertThat(sql).contains("r.origem_sm");
        assertThat(sql).contains("r.destino_sm");
        assertThat(sql).contains("r.origem_destino");
        assertThat(sql).contains("r.origem_nome");
        assertThat(sql).contains("r.ordem_parada_label");
        assertThat(sql).contains("r.destino_nome");
        assertThat(sql).contains("r.horario_corte_texto");
        assertThat(sql).contains("r.previsao_chegada_destino");
        assertThat(sql).contains("r.transit_time_texto");
        assertThat(sql).contains("origem_sm AS [Origem SM]");
        assertThat(sql).contains("destino_sm AS [Destino SM]");
        assertThat(sql).contains("origem_destino AS [Origem Destino]");
        assertThat(sql).contains("origem AS [Origem]");
        assertThat(sql).contains("ordem AS [Ordem]");
        assertThat(sql).contains("destino AS [Destino]");
        assertThat(sql).contains("horario_corte_sm AS [Horario Corte SM]");
        assertThat(sql).contains("previsao_chegada_destino AS [Previsao Chegada Destino]");
        assertThat(sql).contains("transit_time AS [Transit Time]");
        assertThat(MOJIBAKE_PATTERN.matcher(sql).find()).isFalse();
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
