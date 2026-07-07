package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.manifestos.ManifestosPerformanceDTO;
import com.dashboard.api.repository.ManifestosPerformanceSqlRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.PeriodoOffsetDateTimeHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.RowMapper;
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
        repository.buscarCustosDiarios(filtroPadrao());

        assertThat(dto.kpis().totalManifestos()).isEqualTo(12);
        assertThat(dto.kpis().custoPorKg()).isEqualByComparingTo("0.10");
        assertThat(dto.kpis().custoPorKm()).isEqualByComparingTo("4.00");
        assertThat(dto.kpis().receitaPorKg()).isEqualByComparingTo("0.19");
        assertThat(dto.kpis().receitaPorKm()).isEqualByComparingTo("7.50");

        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql)
                        .contains("COUNT_BIG(1) AS total_manifestos")
                        .contains("SUM(CASE WHEN status_norm = N'Pendente'")
                        .contains("NULLIF(SUM(peso_taxado), 0)")
                        .contains("AS receita_por_kg")
                        .contains("NULLIF(SUM(km_total), 0)"));
        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql)
                        .contains("NULLIF(SUM(receita_total), 0)")
                        .contains("NULLIF(SUM(capacidade_veiculo), 0)")
                        .contains("NULLIF(SUM(servicos_total), 0)")
                        .contains("GROUP BY GROUPING SETS ((classificacao_bucket), ())")
                        .doesNotContain("UNION ALL"));
        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql)
                        .contains("tipo_contrato")
                        .contains("COALESCE(SUM(custo_total), 0) AS custo_total")
                        .contains("GROUP BY tipo_contrato")
                        .doesNotContain("tipo_motorista AS tipo"));
        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql)
                        .contains("[Tipo de contrato]")
                        .contains("AS tipo_contrato")
                        .contains("[Tipo de contrato key]")
                        .contains("AS tipo_contrato_key"));
        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql).contains("COUNT(DISTINCT sequence_code) AS quantidade"));
        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql)
                        .contains("COALESCE(SUM(custo_total), 0) AS custo_real")
                        .contains("GROUP BY data_criacao_date"));
        assertThat(jdbcTemplate.sqls)
                .allSatisfy(sql -> assertThat(sql)
                        .doesNotContain("LOWER(filial) IN")
                        .doesNotContain("TRY_CONVERT(DECIMAL(18, 3), [Capacidade Lotação Kg])"));
        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql)
                        .contains("COALESCE([Receita Total Transportada], 0) AS receita_total")
                        .contains("FROM dbo.vw_fato_manifestos_dash")
                        .doesNotContain("COALESCE([Fretes/Total], 0) AS receita_total"));
        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql)
                        .contains("sys.dm_exec_describe_first_result_set")
                        .contains("N'SELECT TOP (0) * FROM dbo.vw_fato_manifestos_dash'"));
    }

    @Test
    void buscarPerformanceFiltraPorClassificacaoNoSql() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        ManifestosPerformanceSqlRepository repository = new ManifestosPerformanceSqlRepository(
                jdbcTemplate,
                new ValidadorPeriodoService(),
                escopoTotal(),
                PeriodoOffsetDateTimeHelper.padrao()
        );
        FiltroConsultaDTO filtro = new FiltroConsultaDTO(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 24),
                Map.of("classificacoes", List.of("Transferência"))
        );

        repository.buscarPerformance(filtro, "dia", null, null);

        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql)
                        .contains("[Classificação] COLLATE Latin1_General_CI_AI IN (:classificacoes)"));
        assertThat(jdbcTemplate.parametros)
                .filteredOn(params -> params.hasValue("classificacoes"))
                .anySatisfy(params -> assertThat(params.getValue("classificacoes"))
                        .isEqualTo(List.of("transferência")));
    }

    @Test
    void buscarPerformanceFiltraFilialPelaChaveCurtaCanonicaNoSql() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        ManifestosPerformanceSqlRepository repository = new ManifestosPerformanceSqlRepository(
                jdbcTemplate,
                new ValidadorPeriodoService(),
                escopoTotal(),
                PeriodoOffsetDateTimeHelper.padrao()
        );
        FiltroConsultaDTO filtro = new FiltroConsultaDTO(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 24),
                Map.of("filiais", List.of("AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA"))
        );

        repository.buscarPerformance(filtro, "dia", null, null);

        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql)
                        .contains("AS filial_key")
                        .contains("filial_key COLLATE Latin1_General_CI_AI IN (:filiais)")
                        .doesNotContain("filial COLLATE Latin1_General_CI_AI IN (:filiais)"));
        assertThat(jdbcTemplate.parametros)
                .filteredOn(params -> params.hasValue("filiais"))
                .anySatisfy(params -> assertThat(params.getValue("filiais"))
                        .isEqualTo(List.of("agu")));
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
                        .contains("data_criacao >= :inicioTemporal")
                        .contains("data_criacao < :fimTemporal")
                        .doesNotContain("YEAR(data_criacao) =")
                        .doesNotContain("MONTH(data_criacao) =")
                        .contains("GROUP BY data_criacao_date"));
        assertThat(jdbcTemplate.parametros)
                .filteredOn(params -> params.hasValue("inicioTemporal") && params.hasValue("fimTemporal"))
                .anySatisfy(params -> {
                    assertThat(params.getValue("inicioTemporal"))
                            .isEqualTo(OffsetDateTime.parse("2026-05-01T00:00:00-03:00"));
                    assertThat(params.getValue("fimTemporal"))
                            .isEqualTo(OffsetDateTime.parse("2026-06-01T00:00:00-03:00"));
                });
    }

    @Test
    void buscarPerformanceFiltraPorNumeroManifestoNoSql() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        ManifestosPerformanceSqlRepository repository = new ManifestosPerformanceSqlRepository(
                jdbcTemplate,
                new ValidadorPeriodoService(),
                escopoTotal(),
                PeriodoOffsetDateTimeHelper.padrao()
        );
        FiltroConsultaDTO filtro = new FiltroConsultaDTO(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 24),
                Map.of("numeroManifesto", List.of("62848"))
        );

        repository.buscarPerformance(filtro, "dia", null, null);

        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql).contains("AND sequence_code = :numeroManifesto"));
        assertThat(jdbcTemplate.sqls)
                .allSatisfy(sql -> assertThat(sql).doesNotContain(":numeroManifestoGROUP"));
        assertThat(jdbcTemplate.parametros)
                .filteredOn(params -> params.hasValue("numeroManifesto"))
                .anySatisfy(params -> assertThat(params.getValue("numeroManifesto")).isEqualTo(62848L));
    }

    @Test
    void buscarPerformanceFiltraTipoContratoPelaKeyPublicadaNaView() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        ManifestosPerformanceSqlRepository repository = new ManifestosPerformanceSqlRepository(
                jdbcTemplate,
                new ValidadorPeriodoService(),
                escopoTotal(),
                PeriodoOffsetDateTimeHelper.padrao()
        );
        FiltroConsultaDTO filtro = new FiltroConsultaDTO(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 24),
                Map.of("tiposContrato", List.of("Frota + PX"))
        );

        repository.buscarPerformance(filtro, "dia", null, null);

        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql)
                        .contains("tipo_contrato_key COLLATE Latin1_General_CI_AI IN (:tiposContrato)")
                        .doesNotContain("tipo_contrato COLLATE Latin1_General_CI_AI IN (:tiposContrato)"));
        assertThat(jdbcTemplate.parametros)
                .filteredOn(params -> params.hasValue("tiposContrato"))
                .anySatisfy(params -> assertThat(params.getValue("tiposContrato"))
                        .isEqualTo(List.of("frota + px")));
    }

    @Test
    void buscarPerformanceIgnoraNumeroManifestoInvalido() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        ManifestosPerformanceSqlRepository repository = new ManifestosPerformanceSqlRepository(
                jdbcTemplate,
                new ValidadorPeriodoService(),
                escopoTotal(),
                PeriodoOffsetDateTimeHelper.padrao()
        );
        FiltroConsultaDTO filtro = new FiltroConsultaDTO(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 24),
                Map.of("numeroManifesto", List.of("62848A"))
        );

        repository.buscarPerformance(filtro, "dia", null, null);

        assertThat(jdbcTemplate.sqls)
                .allSatisfy(sql -> assertThat(sql)
                        .doesNotContain("AND numero = :numeroManifesto")
                        .doesNotContain("AND sequence_code = :numeroManifesto")
                        .doesNotContain("AND 1 = 0"));
        assertThat(jdbcTemplate.parametros)
                .allSatisfy(params -> assertThat(params.hasValue("numeroManifesto")).isFalse());
    }

    @Test
    void buscarPerformanceConsomeSynonymLocalDeManifestosSemRefreshView() {
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
                "Receita Total Transportada",
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
        assertThat(jdbcTemplate.sqls)
                .anySatisfy(sql -> assertThat(sql).contains("FROM dbo.vw_fato_manifestos_dash"));
        assertThat(jdbcTemplate.sqls)
                .noneSatisfy(sql -> assertThat(sql).contains("ETL_SISTEMA"));
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
            parametros.add(paramSource);
            return Map.ofEntries(
                    Map.entry("updated_at", "2026-05-24T10:00:00"),
                    Map.entry("total_manifestos", 12L),
                    Map.entry("em_transito", 3L),
                    Map.entry("pendentes", 2L),
                    Map.entry("encerrados", 7L),
                    Map.entry("km_total", new BigDecimal("100.00")),
                    Map.entry("custo_total", new BigDecimal("400.00")),
                    Map.entry("custo_por_kg", new BigDecimal("0.10")),
                    Map.entry("custo_por_km", new BigDecimal("4.00")),
                    Map.entry("receita_por_kg", new BigDecimal("0.19")),
                    Map.entry("receita_por_km", new BigDecimal("7.50"))
            );
        }

        @Override
        public <T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper) {
            sqls.add(sql);
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
                "Tipo de contrato",
                "Tipo de contrato key",
                "Proprietário/Documento",
                "KM Total",
                "Custo total",
                "Fretes/Total",
                "Receita Total Transportada",
                "Total peso taxado",
                "Capacidade Lotação Kg",
                "Itens/Finalizados",
                "Itens/Total",
                "Data de extracao"
        );
    }
}
