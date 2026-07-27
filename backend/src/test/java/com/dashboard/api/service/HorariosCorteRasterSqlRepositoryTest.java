package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.repository.HorariosCorteRasterSqlRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
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
    void queryDeveAplicarCorteDe22HorasSomenteNaRotaCwbNhb() throws ReflectiveOperationException {
        String sql = sql();

        assertThat(sql).contains("(N'SAO JOSE DOS PINHAIS/PR - RODOGARCIA FILIAL CWB', N'NOVO HAMBURGO/RS - RODOGARCIA FILIAL NHB', CAST(N'22:00:00' AS TIME(0)))");
        assertThat(sql).doesNotContain("(N'SAO JOSE DOS PINHAIS/PR - RODOGARCIA FILIAL CWB', N'NOVO HAMBURGO/RS - RODOGARCIA FILIAL NHB', CAST(N'18:00:00' AS TIME(0)))");
        assertThat(sql).contains("(N'NOVO HAMBURGO/RS - RODOGARCIA FILIAL NHB', N'SAO JOSE DOS PINHAIS/PR - RODOGARCIA FILIAL CWB', CAST(N'04:00:00' AS TIME(0)))");
    }

    @Test
    void queryDeveAplicarCorteDe22HorasNaRotaRjrSpo() throws ReflectiveOperationException {
        String sql = sql();

        assertThat(sql).contains("(N'DUQUE DE CAXIAS/RJ - RODOGARCIA FILIAL RJR', N'OSASCO/SP - RODOGARCIA FILIAL SPO', CAST(N'22:00:00' AS TIME(0)))");
        assertThat(sql).doesNotContain("(N'DUQUE DE CAXIAS/RJ - RODOGARCIA FILIAL RJR', N'OSASCO/SP - RODOGARCIA FILIAL SPO', CAST(N'05:00:00' AS TIME(0)))");
        assertThat(sql).contains("(N'OSASCO/SP - RODOGARCIA FILIAL SPO', N'DUQUE DE CAXIAS/RJ - RODOGARCIA FILIAL RJR', CAST(N'23:30:00' AS TIME(0)))");
    }

    @Test
    void queryDeveForcarNoPrazoQuandoSmTemJustificativa() throws ReflectiveOperationException {
        String sql = sql();
        String sqlSerie = sqlSerie();

        assertThat(sql).contains("LEFT JOIN dbo.viagem_justificativas vj");
        assertThat(sql).contains("ON rc.cod_solicitacao = vj.cod_solicitacao");
        assertThat(sql).contains("AND vj.ativo = 1");
        assertThat(sql).contains("vj.cod_solicitacao AS cod_solicitacao_justificada");
        assertThat(sql).contains("vj.justificativa AS justificativa");
        assertThat(sql).contains("justificativa AS justificativa");
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
    void tabelaPaginadaDeveAplicarFiltrosAnaliticosAntesDoCountEOffset() throws ReflectiveOperationException {
        FiltroConsultaDTO filtro = new FiltroConsultaDTO(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 12),
                Map.of(
                        "tabelaBusca", List.of("Raster"),
                        "tabelaColuna.filial", List.of("CWB"),
                        "tabelaColuna.linhaOuOperacao", List.of("CWB"),
                        "tabelaColuna.saiuNoHorario", List.of("NO PRAZO")
                )
        );

        Object countQuery = tabelaQuery(sqlCount(), filtro, "");
        String sqlCount = querySql(countQuery);
        MapSqlParameterSource countParams = queryParams(countQuery);

        assertThat(sqlCount)
                .contains("SELECT COUNT_BIG(1) FROM calculado")
                .contains("filial_key LIKE :filtroTabelaColuna_filial")
                .contains("filtroTabelaColuna_linhaOuOperacao")
                .contains("IN (:filtroTabelaColuna_saiuNoHorario)")
                .doesNotContain("OFFSET :offset");
        assertThat(countParams.getValue("filtroTabelaBusca")).isEqualTo("%raster%");
        assertThat(countParams.getValue("filtroTabelaColuna_filial")).isEqualTo("%cwb%");
        assertThat(countParams.getValue("filtroTabelaColuna_linhaOuOperacao")).isEqualTo("%cwb%");
        assertThat(countParams.getValue("filtroTabelaColuna_saiuNoHorario")).isEqualTo(List.of("no prazo"));

        Object pageQuery = tabelaQuery(sql(), filtro, sqlOrderPaged());
        assertThat(querySql(pageQuery))
                .contains("filial_key LIKE :filtroTabelaColuna_filial")
                .contains("ORDER BY data DESC, importado_em DESC, filial, linha_ou_operacao")
                .contains("OFFSET :offset ROWS FETCH NEXT :limite ROWS ONLY");
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
        LocalDateTime horarioCorte = LocalDateTime.of(2026, 6, 5, 22, 0);
        LocalDateTime saidaDentroTolerancia = LocalDateTime.of(2026, 6, 5, 22, 10);
        LocalDateTime saidaForaTolerancia = LocalDateTime.of(2026, 6, 5, 22, 11);
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

    private String sqlCount() throws ReflectiveOperationException {
        return constanteSql("SQL_COUNT");
    }

    private String sqlOrderPaged() throws ReflectiveOperationException {
        return constanteSql("SQL_ORDER_PAGED");
    }

    private String constanteSql(String nome) throws ReflectiveOperationException {
        Field field = HorariosCorteRasterSqlRepository.class.getDeclaredField(nome);
        field.setAccessible(true);
        return (String) field.get(null);
    }

    private Object tabelaQuery(String sqlBase, FiltroConsultaDTO filtro, String sufixo) throws ReflectiveOperationException {
        HorariosCorteRasterSqlRepository repository = new HorariosCorteRasterSqlRepository(new JdbcTemplate());
        Method method = HorariosCorteRasterSqlRepository.class.getDeclaredMethod(
                "tabelaQuery",
                String.class,
                LocalDate.class,
                LocalDate.class,
                EscopoFilialService.EscopoFilial.class,
                List.class,
                FiltroConsultaDTO.class,
                String.class
        );
        method.setAccessible(true);
        return method.invoke(
                repository,
                sqlBase,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 12),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                List.of("CWB"),
                filtro,
                sufixo
        );
    }

    private String querySql(Object query) throws ReflectiveOperationException {
        Method method = query.getClass().getDeclaredMethod("sql");
        method.setAccessible(true);
        return (String) method.invoke(query);
    }

    private MapSqlParameterSource queryParams(Object query) throws ReflectiveOperationException {
        Method method = query.getClass().getDeclaredMethod("params");
        method.setAccessible(true);
        return (MapSqlParameterSource) method.invoke(query);
    }

    private int toleranciaHorarioCorteMinutos() throws ReflectiveOperationException {
        Field field = HorariosCorteRasterSqlRepository.class.getDeclaredField("TOLERANCIA_HORARIO_CORTE_MINUTOS");
        field.setAccessible(true);
        return (int) field.get(null);
    }
}
