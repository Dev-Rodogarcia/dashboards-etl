package com.dashboard.api.service;

import com.dashboard.api.repository.HorariosCorteRasterSqlRepository;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HorariosCorteRasterSqlRepositoryTest {

    @Test
    void queryDeveFiltrarJanelaDaSmComPredicadosSargaveis() throws ReflectiveOperationException {
        String sql = sql();

        assertThat(sql).contains("CAST(COALESCE(v.data_hora_prev_ini, v.data_hora_real_ini) AS DATETIME2(0)) AS data_base_sm_at");
        assertThat(sql).contains("WHERE v.data_hora_prev_ini >= :dataInicio");
        assertThat(sql).contains("AND v.data_hora_prev_ini < :dataFimExclusivo");
        assertThat(sql).contains("WHERE v.data_hora_prev_ini IS NULL");
        assertThat(sql).contains("AND v.data_hora_real_ini >= :dataInicio");
        assertThat(sql).contains("AND v.data_hora_real_ini < :dataFimExclusivo");
        assertThat(sql).doesNotContain("WHERE COALESCE(v.data_hora_prev_ini, v.data_hora_real_ini)");
        assertThat(sql).doesNotContain("CAST(? AS DATE)");
        assertThat(sql).contains("CAST(rc.data_base_sm_at AS DATE) AS data_corte");
        assertThat(sql).contains("CAST(CAST(rc.data_base_sm_at AS DATE) AS DATETIME2(0))");
        assertThat(sql).doesNotContain("data_corte_base_at");
    }

    @Test
    void queryDeveCompararSaidaEfetivaComHorarioCorteCorrigido() throws ReflectiveOperationException {
        String sql = sql();

        assertThat(sql).contains("data_hora_real_ini_at AS saida_efetiva");
        assertThat(sql).contains("WHEN data_hora_real_ini_at IS NULL OR corte_at IS NULL THEN NULL");
        assertThat(sql).contains("WHEN data_hora_real_ini_at <= corte_at THEN CAST(1 AS BIT)");
        assertThat(sql).contains("ELSE DATEDIFF(MINUTE, corte_at, data_hora_real_ini_at)");
    }

    @Test
    void serieDeveAgruparKpiNoSqlServer() throws ReflectiveOperationException {
        String sql = sqlSerie();

        assertThat(sql).contains("COUNT_BIG(1) AS total_programado");
        assertThat(sql).contains("SUM(CASE WHEN saiu_no_horario = 1 THEN 1 ELSE 0 END)");
        assertThat(sql).contains("SUM(CASE WHEN saiu_no_horario = 0 THEN 1 ELSE 0 END)");
        assertThat(sql).contains("GROUP BY data_corte, filial");
        assertThat(sql).contains("ORDER BY data_corte, filial");
    }

    @Test
    void regraCorrigidaDeveMarcarAtrasoQuandoFimPrevistoCaiNoDiaSeguinte() {
        LocalDateTime dataBaseSm = LocalDateTime.of(2026, 5, 20, 23, 10);
        LocalDateTime saidaEfetiva = LocalDateTime.of(2026, 5, 21, 0, 10);
        LocalDateTime previsaoChegadaDiaSeguinte = LocalDateTime.of(2026, 5, 21, 5, 0);
        LocalTime cortePrevistoRota = LocalTime.of(23, 30);

        LocalDateTime horarioCorteCorrigido = dataBaseSm.toLocalDate().atTime(cortePrevistoRota);
        LocalDateTime horarioCorteAntigo = previsaoChegadaDiaSeguinte.toLocalDate().atTime(cortePrevistoRota);

        assertThat(saidaEfetiva.compareTo(horarioCorteCorrigido) <= 0).isFalse();
        assertThat(saidaEfetiva.compareTo(horarioCorteAntigo) <= 0).isTrue();
    }

    private String sql() throws ReflectiveOperationException {
        Field field = HorariosCorteRasterSqlRepository.class.getDeclaredField("SQL");
        field.setAccessible(true);
        return (String) field.get(null);
    }

    private String sqlSerie() throws ReflectiveOperationException {
        Field field = HorariosCorteRasterSqlRepository.class.getDeclaredField("SQL_SERIE");
        field.setAccessible(true);
        return (String) field.get(null);
    }
}
