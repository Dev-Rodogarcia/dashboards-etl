package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.repository.IndicadoresGestaoAVistaSqlRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.PeriodoOffsetDateTimeHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.RowMapper;
import static org.assertj.core.api.Assertions.assertThat;

class IndicadoresGestaoAVistaSqlRepositoryTest {

    @Test
    void performanceECubagemUsamFatoGestaoVistaComFiltroParticionado() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        IndicadoresGestaoAVistaSqlRepository repository = new IndicadoresGestaoAVistaSqlRepository(
                jdbcTemplate,
                PeriodoOffsetDateTimeHelper.padrao()
        );

        FiltroConsultaDTO filtro = new FiltroConsultaDTO(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 29),
                Map.of("filiais", List.of("SPO"))
        );
        EscopoFilialService.EscopoFilial escopo = EscopoFilialService.EscopoFilial.comAcessoTotal();

        repository.buscarPerformanceEntregaSerie(filtro, escopo);
        repository.buscarCubagemSerie(filtro, escopo, Set.of("43996693000127"));
        repository.buscarIndenizacaoSerie(filtro, escopo);
        repository.buscarUtilizacaoColetoresSerie(filtro, escopo);

        String performanceSql = jdbcTemplate.sqls.get(0);
        String cubagemSql = jdbcTemplate.sqls.get(1);
        String demaisSql = String.join("\n", jdbcTemplate.sqls.subList(2, jdbcTemplate.sqls.size()));

        assertThat(performanceSql)
                .contains("FROM [ETL_SISTEMA].dbo.fato_gestao_vista_fretes")
                .contains("WHERE indicador_codigo = 'PE'")
                .contains("AND data_referencia >= :dataInicio")
                .contains("AND data_referencia < :dataFimExclusivo")
                .contains("SUM(CAST(is_no_prazo AS BIGINT))")
                .contains("SUM(CAST(is_fora_prazo AS BIGINT))")
                .doesNotContain("vw_fretes_powerbi")
                .doesNotContain("fretes_deduplicados")
                .doesNotContain("ROW_NUMBER()")
                .doesNotContain("[Previsão de Entrega]");

        assertThat(cubagemSql)
                .contains("FROM [ETL_SISTEMA].dbo.fato_gestao_vista_fretes")
                .contains("WHERE indicador_codigo = 'CB'")
                .contains("AND data_referencia >= :dataInicio")
                .contains("AND data_referencia < :dataFimExclusivo")
                .contains("AND is_pagador_excluido_cubagem = 0")
                .contains("SUM(CAST(is_cubado AS BIGINT))")
                .doesNotContain("vw_fretes_powerbi")
                .doesNotContain("fretes_deduplicados")
                .doesNotContain("ROW_NUMBER()")
                .doesNotContain("[Data frete]")
                .doesNotContain("docsExcluidos");

        assertThat(demaisSql)
                .contains("WHERE [Data abertura] >= :dataInicio")
                .contains("AND [Data abertura] < :dataFimExclusivo")
                .contains("WHERE [Data criação] >= :inicioOffset")
                .contains("AND [Data criação] < :fimOffset")
                .contains("WHERE [Data/Hora início] >= :inicioOffset")
                .contains("AND [Data/Hora início] < :fimOffset");

        assertThat(String.join("\n", jdbcTemplate.sqls))
                .doesNotContain("WHERE TRY_CONVERT")
                .doesNotContain("AND TRY_CONVERT")
                .doesNotContain("WHERE YEAR(")
                .doesNotContain("AND YEAR(")
                .doesNotContain("WHERE MONTH(")
                .doesNotContain("AND MONTH(");

        assertThat(demaisSql)
                .contains("faturamento_mensal AS")
                .contains("SUM(ABS(valor_a_pagar_cliente))")
                .contains("COUNT_BIG(1) AS manifestos_emitidos")
                .contains("COUNT_BIG(1) AS manifestos_bipados");
    }

    private static final class CapturandoNamedParameterJdbcTemplate extends NamedParameterJdbcTemplate {
        private final List<String> sqls = new ArrayList<>();

        private CapturandoNamedParameterJdbcTemplate() {
            super(new JdbcTemplate());
        }

        @Override
        public Map<String, Object> queryForMap(String sql, SqlParameterSource paramSource) {
            sqls.add(sql);
            return Map.ofEntries(
                    Map.entry("updated_at", "2026-06-01T10:00:00"),
                    Map.entry("total_entregas", 0L),
                    Map.entry("entregas_no_prazo", 0L),
                    Map.entry("entregas_fora_do_prazo", 0L),
                    Map.entry("total_fretes", 0L),
                    Map.entry("fretes_cubados", 0L),
                    Map.entry("fretes_com_peso_real", 0L),
                    Map.entry("total_sinistros", 0L),
                    Map.entry("valor_indenizado_original", BigDecimal.ZERO),
                    Map.entry("valor_indenizado_abs", BigDecimal.ZERO),
                    Map.entry("faturamento_base", BigDecimal.ZERO),
                    Map.entry("manifestos_bipados", 0L),
                    Map.entry("manifestos_emitidos", 0L),
                    Map.entry("manifestos_descarregamento", 0L),
                    Map.entry("manifestos_incompletos", 0L)
            );
        }

        @Override
        public <T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper) {
            sqls.add(sql);
            return List.of();
        }

        @Override
        public <T> T queryForObject(String sql, SqlParameterSource paramSource, Class<T> requiredType) {
            sqls.add(sql);
            if (Long.class.equals(requiredType)) {
                return requiredType.cast(0L);
            }
            return null;
        }
    }
}
