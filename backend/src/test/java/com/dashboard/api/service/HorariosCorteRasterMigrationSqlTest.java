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
        assertThat(sql).contains("FROM [ETL_SISTEMA].dbo.raster_viagens");
        assertThat(sql).contains("FROM [ETL_SISTEMA].dbo.raster_viagem_paradas");
        assertThat(sql).contains("FROM ETL_SISTEMA.INFORMATION_SCHEMA.COLUMNS c");
        assertThat(sql).contains("THROW 51303");
        assertThat(sql).contains("hc_apoio AS");
        assertThat(sql).contains("AS [Saiu no Horário]");
        assertThat(sql).contains("AS [Atraso Minutos]");
        assertThat(sql).contains("N''Raster API - SQL Server'' AS [Nome do Arquivo]");
        assertThat(sql).contains("data_extracao_at AS [Data de extracao]");
    }

    @Test
    void migrationV013DeveConsumirCamposOperacionaisDaViewRasterSemMojibake() throws IOException {
        String sql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V013__horarios_corte_consumir_raster_etl.sql"));

        assertThat(sql).contains("v.rota_descricao");
        assertThat(sql).contains("v.data_hora_real_ini");
        assertThat(sql).contains("v.data_hora_real_fim");
        assertThat(sql).contains("v.data_hora_prev_fim");
        assertThat(sql).contains("v.data_hora_identificou_fim_viagem");
        assertThat(sql).contains("N''AGUDOS/SP - RODOGARCIA FILIAL AGU''");
        assertThat(sql).contains("N''OSASCO/SP - RODOGARCIA FILIAL SPO''");
        assertThat(sql).contains("origem_sm AS [Origem SM]");
        assertThat(sql).contains("destino_sm AS [Destino SM]");
        assertThat(sql).contains("origem_destino AS [Origem Destino]");
        assertThat(sql).contains("AS [Horario Corte SM]");
        assertThat(sql).contains("AS [Previsao Chegada Destino]");
        assertThat(sql).contains("AS [Transit Time]");
        assertThat(MOJIBAKE_PATTERN.matcher(sql).find()).isFalse();
    }

    @Test
    void migrationV013NaoDeveUsarPrevisaoDeInicioComoHorarioDeCorte() throws IOException {
        String sql = lerSql(Path.of("src", "main", "resources", "db", "migration",
                "V013__horarios_corte_consumir_raster_etl.sql"));

        assertThat(sql).doesNotContain("data_hora_prev_ini");
        assertThat(sql).contains("data_hora_real_ini_at <= corte_at");
        assertThat(sql).contains("CAST(COALESCE(v.data_hora_real_fim, v.data_hora_prev_fim, v.data_hora_identificou_fim_viagem) AS DATETIME2(0)) AS data_corte_base_at");
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
