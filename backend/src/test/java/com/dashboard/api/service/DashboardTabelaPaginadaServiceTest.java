package com.dashboard.api.service;

import com.dashboard.api.builder.DashboardExportSqlBuilder;
import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.PeriodoOffsetDateTimeHelper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import static org.assertj.core.api.Assertions.assertThat;

class DashboardTabelaPaginadaServiceTest {

    @Test
    void buscarPrimeiraPaginaColetasDeveDeduplicarComRowNumberEPagearNoSqlServer() {
        CapturandoJdbcTemplate jdbcTemplate = new CapturandoJdbcTemplate(List.of(linha("ID", "coleta-1")));
        DashboardTabelaPaginadaService service = service(jdbcTemplate);

        service.buscarPrimeiraPaginaColetas(filtroPadrao(), 150);

        assertThat(jdbcTemplate.sqls()).hasSize(2);
        assertThat(jdbcTemplate.sqls().get(0))
                .contains("ROW_NUMBER() OVER (PARTITION BY [ID]")
                .contains("WHERE [__rn] = 1")
                .contains("[Solicitacao] >= :dataInicio AND [Solicitacao] < :dataFimExclusivo");
        assertThat(jdbcTemplate.sqls().get(1))
                .contains("ROW_NUMBER() OVER (PARTITION BY [ID]")
                .contains("WHERE [__rn] = 1")
                .contains("OFFSET :offsetTabela ROWS FETCH NEXT :tamanhoTabela ROWS ONLY");
        assertThat(jdbcTemplate.params().get(1).getValue("offsetTabela")).isEqualTo(0L);
        assertThat(jdbcTemplate.params().get(1).getValue("tamanhoTabela")).isEqualTo(150);
    }

    @Test
    void buscarPrimeiraPaginaTrackingDeveUsarCountEOffsetFetchNativos() {
        CapturandoJdbcTemplate jdbcTemplate = new CapturandoJdbcTemplate(List.of(linha("N° Minuta", 123L)));
        DashboardTabelaPaginadaService service = service(jdbcTemplate);

        service.buscarPrimeiraPaginaTracking(filtroPadrao(), 200);

        assertThat(jdbcTemplate.sqls()).hasSize(2);
        assertThat(jdbcTemplate.sqls().get(0))
                .contains("SELECT COUNT(1)")
                .contains("FROM [vw_localizacao_cargas_powerbi] base_raw")
                .contains("[Data do frete] >= :inicioOffset AND [Data do frete] < :fimOffset");
        assertThat(jdbcTemplate.sqls().get(1))
                .contains("ORDER BY [Data do frete] DESC, [N° Minuta] DESC")
                .contains("OFFSET :offsetTabela ROWS FETCH NEXT :tamanhoTabela ROWS ONLY");
        assertThat(jdbcTemplate.params().get(1).getValue("tamanhoTabela")).isEqualTo(200);
    }

    @Test
    void buscarPrimeiraPaginaContasAPagarDeveUsarCountEOffsetFetchNativos() {
        CapturandoJdbcTemplate jdbcTemplate = new CapturandoJdbcTemplate(List.of(linha("Lançamento a Pagar/N°", 10L)));
        DashboardTabelaPaginadaService service = service(jdbcTemplate);

        service.buscarPrimeiraPaginaContasAPagar(filtroPadrao(), 125);

        assertThat(jdbcTemplate.sqls()).hasSize(2);
        assertThat(jdbcTemplate.sqls().get(0))
                .contains("SELECT COUNT(1) FROM [vw_contas_a_pagar_powerbi] base")
                .contains("[Emissão] >= :dataInicio AND [Emissão] < :dataFimExclusivo");
        assertThat(jdbcTemplate.sqls().get(1))
                .contains("ORDER BY [Emissão] DESC, [Lançamento a Pagar/N°] DESC")
                .contains("OFFSET :offsetTabela ROWS FETCH NEXT :tamanhoTabela ROWS ONLY");
        assertThat(jdbcTemplate.params().get(1).getValue("tamanhoTabela")).isEqualTo(125);
    }

    private static DashboardTabelaPaginadaService service(NamedParameterJdbcTemplate jdbcTemplate) {
        return new DashboardTabelaPaginadaService(
                jdbcTemplate,
                new DashboardExportSqlBuilder(PeriodoOffsetDateTimeHelper.padrao()),
                new ValidadorPeriodoService(),
                escopoSemRestricao()
        );
    }

    private static FiltroConsultaDTO filtroPadrao() {
        return new FiltroConsultaDTO(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), Map.of());
    }

    private static Map<String, Object> linha(String coluna, Object valor) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(coluna, valor);
        return row;
    }

    private static EscopoFilialService escopoSemRestricao() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }

    private static final class CapturandoJdbcTemplate extends NamedParameterJdbcTemplate {

        private final List<Map<String, Object>> linhas;
        private final List<String> sqls = new ArrayList<>();
        private final List<SqlParameterSource> params = new ArrayList<>();

        private CapturandoJdbcTemplate(List<Map<String, Object>> linhas) {
            super(new JdbcTemplate());
            this.linhas = linhas;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T queryForObject(String sql, SqlParameterSource paramSource, Class<T> requiredType) {
            sqls.add(sql);
            params.add(paramSource);
            if (requiredType == Long.class) {
                return (T) Long.valueOf(linhas.size());
            }
            return null;
        }

        @Override
        public List<Map<String, Object>> queryForList(String sql, SqlParameterSource paramSource) {
            sqls.add(sql);
            params.add(paramSource);
            return linhas;
        }

        private List<String> sqls() {
            return sqls;
        }

        private List<SqlParameterSource> params() {
            return params;
        }
    }
}
