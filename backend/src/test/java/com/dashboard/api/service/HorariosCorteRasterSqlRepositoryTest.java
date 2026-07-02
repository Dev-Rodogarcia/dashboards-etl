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
        assertThat(sql).contains("WHEN data_hora_real_ini_at <= DATEADD(MINUTE, :toleranciaHorarioCorteMinutos, corte_at) THEN CAST(1 AS BIT)");
        assertThat(sql).contains("WHEN data_hora_real_ini_at <= DATEADD(MINUTE, :toleranciaHorarioCorteMinutos, corte_at) THEN 0");
        assertThat(sql).contains("ELSE DATEDIFF(MINUTE, DATEADD(MINUTE, :toleranciaHorarioCorteMinutos, corte_at), data_hora_real_ini_at)");
    }

    @Test
    void queryDeveForcarNoPrazoQuandoSmTemJustificativa() throws ReflectiveOperationException {
        String sql = sql();
        String sqlSerie = sqlSerie();

        assertThat(sql).contains("LEFT JOIN dbo.viagem_justificativas vj");
        assertThat(sql).contains("ON rc.cod_solicitacao = vj.cod_solicitacao");
        assertThat(sql).contains("vj.cod_solicitacao AS cod_solicitacao_justificada");
        assertThat(sql).contains("WHEN cod_solicitacao_justificada IS NOT NULL THEN CAST(1 AS BIT)");
        assertThat(sql).contains("WHEN cod_solicitacao_justificada IS NOT NULL THEN 0");
        assertThat(sqlSerie).contains("WHEN cod_solicitacao_justificada IS NOT NULL THEN 1");
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

    @Test
    void regraDeToleranciaDeveAceitarAteDezMinutosAposCorte() throws ReflectiveOperationException {
        int toleranciaMinutos = toleranciaHorarioCorteMinutos();
        LocalDateTime horarioCorte = LocalDateTime.of(2026, 6, 5, 20, 30);
        LocalDateTime saidaDentroTolerancia = LocalDateTime.of(2026, 6, 5, 20, 38);
        LocalDateTime saidaForaTolerancia = LocalDateTime.of(2026, 6, 5, 20, 41);
        LocalDateTime limiteComTolerancia = horarioCorte.plusMinutes(toleranciaMinutos);

        assertThat(toleranciaMinutos).isEqualTo(10);
        assertThat(saidaDentroTolerancia.compareTo(limiteComTolerancia) <= 0).isTrue();
        assertThat(saidaForaTolerancia.compareTo(limiteComTolerancia) <= 0).isFalse();
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

    private int toleranciaHorarioCorteMinutos() throws ReflectiveOperationException {
        Field field = HorariosCorteRasterSqlRepository.class.getDeclaredField("TOLERANCIA_HORARIO_CORTE_MINUTOS");
        field.setAccessible(true);
        return (int) field.get(null);
    }
}
