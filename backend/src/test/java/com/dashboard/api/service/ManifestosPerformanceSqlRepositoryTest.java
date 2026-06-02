package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.manifestos.ManifestosPerformanceDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ManifestosPerformanceSqlRepositoryTest {

    @Test
    void buscarPerformanceUsaAgregacoesSqlComDefesaContraDivisaoPorZero() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        ManifestosPerformanceSqlRepository repository = new ManifestosPerformanceSqlRepository(
                jdbcTemplate,
                new ValidadorPeriodoService(),
                escopoTotal(),
                PeriodoOffsetDateTimeHelper.padrao()
        );

        ManifestosPerformanceDTO dto = repository.buscarPerformance(
                filtroPadrao(),
                "ano",
                null,
                null
        );

        assertThat(dto.kpis().totalManifestos()).isEqualTo(12);
        assertThat(dto.kpis().custoPorKm()).isEqualByComparingTo("4.00");
        assertThat(dto.kpis().receitaPorKm()).isEqualByComparingTo("7.50");

        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql)
                        .contains("COUNT_BIG(1) AS total_manifestos")
                        .contains("SUM(CASE WHEN status_norm = N'Pendente'")
                        .contains("NULLIF(SUM(km_total), 0)"));
        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql)
                        .contains("NULLIF(SUM(receita_total), 0)")
                        .contains("classificacao_bucket"));
        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql).contains("tipo_motorista AS tipo"));
        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql).contains("[Tipo Motorista]"));
        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql).contains("COUNT(DISTINCT numero) AS quantidade"));
    }

    @Test
    void buscarPerformanceAplicaDrillTemporalPorAnoEMesNoSql() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        ManifestosPerformanceSqlRepository repository = new ManifestosPerformanceSqlRepository(
                jdbcTemplate,
                new ValidadorPeriodoService(),
                escopoTotal(),
                PeriodoOffsetDateTimeHelper.padrao()
        );

        repository.buscarPerformance(filtroPadrao(), "dia", 2026, 5);

        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql)
                        .contains("YEAR(data_criacao_date) = :anoTemporal")
                        .contains("MONTH(data_criacao_date) = :mesTemporal")
                        .contains("GROUP BY data_criacao_date"));
        assertThat(jdbcTemplate.parametros)
                .filteredOn(params -> params.hasValue("anoTemporal") && params.hasValue("mesTemporal"))
                .anySatisfy(params -> {
                    assertThat(params.getValue("anoTemporal")).isEqualTo(2026);
                    assertThat(params.getValue("mesTemporal")).isEqualTo(5);
                });
    }

    @Test
    void buscarPerformanceNaoAtualizaMetadataQuandoWrapperManifestosEstaDesatualizado() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        jdbcTemplate.colunasManifestos = List.of(
                "Número",
                "Data criação",
                "Status",
                "Classificação",
                "Filial",
                "Motorista",
                "Veículo/Placa",
                "Tipo Veículo",
                "KM Total",
                "Custo total",
                "Fretes/Total",
                "Total peso taxado",
                "Capacidade Lotação Kg",
                "Itens/Finalizados",
                "Itens/Total",
                "Data de extracao"
        );
        ManifestosPerformanceSqlRepository repository = new ManifestosPerformanceSqlRepository(
                jdbcTemplate,
                new ValidadorPeriodoService(),
                escopoTotal(),
                PeriodoOffsetDateTimeHelper.padrao()
        );

        repository.buscarPerformance(filtroPadrao(), "dia", null, null);
        repository.buscarPerformance(filtroPadrao(), "dia", null, null);

        assertThat(jdbcTemplate.sqls)
                .noneSatisfy(sql -> assertThat(sql).contains("sp_refreshview"));
        assertThat(jdbcTemplate.consultasColunas).isEqualTo(2);
    }

    private static FiltroConsultaDTO filtroPadrao() {
        return new FiltroConsultaDTO(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 24),
                Map.of(
                        "filiais", List.of("SPO"),
                        "status", List.of("Encerrado")
                )
        );
    }

    private static EscopoFilialService escopoTotal() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }

    private static final class CapturandoNamedParameterJdbcTemplate extends NamedParameterJdbcTemplate {
        private final List<String> sqls = new ArrayList<>();
        private final List<SqlParameterSource> parametros = new ArrayList<>();
        private int consultasColunas;
        private List<String> colunasManifestos = colunasManifestosValidas();
        private SqlParameterSource ultimoParametro;

        private CapturandoNamedParameterJdbcTemplate() {
            super(new JdbcTemplate());
        }

        @Override
        public <T> List<T> queryForList(String sql, SqlParameterSource paramSource, Class<T> elementType) {
            sqls.add(sql);
            parametros.add(paramSource);
            consultasColunas++;
            return colunasManifestos.stream()
                    .map(elementType::cast)
                    .toList();
        }

        @Override
        public Map<String, Object> queryForMap(String sql, SqlParameterSource paramSource) {
            sqls.add(sql);
            ultimoParametro = paramSource;
            parametros.add(paramSource);
            return Map.of(
                    "updated_at", "2026-05-24T10:00:00",
                    "total_manifestos", 12L,
                    "em_transito", 3L,
                    "pendentes", 2L,
                    "encerrados", 7L,
                    "km_total", new BigDecimal("100.00"),
                    "custo_total", new BigDecimal("400.00"),
                    "custo_por_km", new BigDecimal("4.00"),
                    "receita_por_km", new BigDecimal("7.50")
            );
        }

        @Override
        public <T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper) {
            sqls.add(sql);
            ultimoParametro = paramSource;
            parametros.add(paramSource);
            return List.of();
        }
    }

    private static List<String> colunasManifestosValidas() {
        return List.of(
                "Número",
                "Data criação",
                "Status",
                "Classificação",
                "Filial",
                "Motorista",
                "Veículo/Placa",
                "Tipo Veículo",
                "Tipo Motorista",
                "Proprietário/Documento",
                "KM Total",
                "Custo total",
                "Fretes/Total",
                "Total peso taxado",
                "Capacidade Lotação Kg",
                "Itens/Finalizados",
                "Itens/Total",
                "Data de extracao"
        );
    }
}
