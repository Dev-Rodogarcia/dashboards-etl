package com.dashboard.api.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class HorariosCorteRasterSqlRepositoryTest {

    @Test
    void queryDeveAncorarHorarioCorteNaDataBaseDaSm() throws ReflectiveOperationException {
        String sql = sql();

        assertThat(sql).contains("CAST(COALESCE(v.data_hora_prev_ini, v.data_hora_real_ini) AS DATETIME2(0)) AS data_base_sm_at");
        assertThat(sql).contains("WHERE COALESCE(v.data_hora_prev_ini, v.data_hora_real_ini) >= CAST(? AS DATE)");
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
}
