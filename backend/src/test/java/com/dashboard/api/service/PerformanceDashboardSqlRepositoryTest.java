package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.performance.PerformanceTabelaProjection;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceDashboardSqlRepositoryTest {

    @Test
    void tabelaRetornaPageComPageableDoSpring() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        PerformanceDashboardSqlRepository repository = new PerformanceDashboardSqlRepository(
                jdbcTemplate,
                new ValidadorPeriodoService(),
                escopoTotal()
        );

        Page<PerformanceTabelaProjection> pagina = repository.buscarTabela(
                new FiltroConsultaDTO(
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 24),
                        Map.of()
                ),
                PageRequest.of(1, 50)
        );

        assertEquals(1, pagina.getNumber());
        assertEquals(50, pagina.getSize());
        assertEquals(0L, pagina.getTotalElements());
        assertTrue(jdbcTemplate.sqls.stream().anyMatch(sql -> sql.contains("COUNT_BIG(1)")));
        assertTrue(jdbcTemplate.sqls.stream().anyMatch(sql -> sql.contains("OFFSET :offsetTabela ROWS FETCH NEXT :tamanhoTabela ROWS ONLY")));
        assertEquals(50L, jdbcTemplate.ultimoParametroLong("offsetTabela"));
        assertEquals(50L, jdbcTemplate.ultimoParametroLong("tamanhoTabela"));
    }

    @Test
    void tabelaPaginadaUsaCountEOffsetFetchNoBanco() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        PerformanceDashboardSqlRepository repository = new PerformanceDashboardSqlRepository(
                jdbcTemplate,
                new ValidadorPeriodoService(),
                escopoTotal()
        );

        repository.buscarTabelaPaginada(
                new FiltroConsultaDTO(
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 24),
                        Map.of()
                ),
                2,
                50
        );

        List<String> consultasDados = jdbcTemplate.sqls.stream()
                .filter(sql -> sql.contains("FROM entregas"))
                .toList();

        assertEquals(2, consultasDados.size());
        assertTrue(consultasDados.get(0).contains("COUNT_BIG(1)"));
        assertTrue(consultasDados.get(1).contains("OFFSET :offsetTabela ROWS FETCH NEXT :tamanhoTabela ROWS ONLY"));
        assertTrue(consultasDados.get(1).contains("N'SEM_REGIAO'"));
        assertTrue(!consultasDados.get(1).contains("[Região Destino]"));
        assertEquals(50L, jdbcTemplate.ultimoParametroLong("offsetTabela"));
        assertEquals(50L, jdbcTemplate.ultimoParametroLong("tamanhoTabela"));
    }

    @Test
    void tabelaAplicaFiltrosAnaliticosAntesDaPaginacao() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        PerformanceDashboardSqlRepository repository = new PerformanceDashboardSqlRepository(
                jdbcTemplate,
                new ValidadorPeriodoService(),
                escopoTotal()
        );

        repository.buscarTabela(
                new FiltroConsultaDTO(
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 24),
                        Map.of(
                                "tabelaBusca", List.of("Campinas"),
                                "tabelaStatus", List.of("Finalizada"),
                                "tabelaColuna.numeroMinuta", List.of("123"),
                                "tabelaColuna.cidadeDestino", List.of("Destino")
                        )
                ),
                PageRequest.of(0, 20)
        );

        assertTrue(jdbcTemplate.sqls.stream().anyMatch(sql -> sql.contains("filtroTabelaBuscaTexto")));
        assertTrue(jdbcTemplate.sqls.stream().anyMatch(sql -> sql.contains("filtroTabelaStatus")));
        assertTrue(jdbcTemplate.sqls.stream().anyMatch(sql -> sql.contains("filtroTabelaColuna_numeroMinuta")));
        assertTrue(jdbcTemplate.sqls.stream().anyMatch(sql -> sql.contains("filtroTabelaColuna_cidadeDestino")));
        assertEquals(123L, jdbcTemplate.ultimoParametroLong("filtroTabelaColuna_numeroMinuta"));
        assertEquals(0L, jdbcTemplate.ultimoParametroLong("offsetTabela"));
        assertEquals(20L, jdbcTemplate.ultimoParametroLong("tamanhoTabela"));
    }

    @Test
    void tabelaAplicaFiltroDePagadoresNaCteBaseComInSargable() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        PerformanceDashboardSqlRepository repository = new PerformanceDashboardSqlRepository(
                jdbcTemplate,
                new ValidadorPeriodoService(),
                escopoTotal()
        );

        repository.buscarTabela(
                new FiltroConsultaDTO(
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 24),
                        Map.of("pagadores", List.of("Cliente A", "Cliente B"))
                ),
                PageRequest.of(0, 20)
        );

        assertTrue(jdbcTemplate.sqls.stream().anyMatch(sql -> sql.contains(
                "AND (:pagadoresVazio = 1 OR base_raw.[Pagador] IN (:pagadores))"
        )));
        assertEquals(0L, jdbcTemplate.ultimoParametroLong("pagadoresVazio"));
        assertEquals(List.of("Cliente A", "Cliente B"), jdbcTemplate.ultimoParametro("pagadores"));
    }

    @Test
    void exportacaoAplicaFiltrosDaTabelaSemPaginar() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        PerformanceDashboardSqlRepository repository = new PerformanceDashboardSqlRepository(
                jdbcTemplate,
                new ValidadorPeriodoService(),
                escopoTotal()
        );

        repository.buscarTabelaExportacao(new FiltroConsultaDTO(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 24),
                Map.of(
                        "pagadores", List.of("Cliente A"),
                        "tabelaBusca", List.of("Campinas"),
                        "tabelaStatus", List.of("Finalizada")
                )
        ));

        String sqlExportacao = jdbcTemplate.sqls.get(jdbcTemplate.sqls.size() - 1);
        assertTrue(sqlExportacao.contains("filtroTabelaBuscaTexto"));
        assertTrue(sqlExportacao.contains("filtroTabelaStatus"));
        assertTrue(sqlExportacao.contains("base_raw.[Pagador] IN (:pagadores)"));
        assertTrue(!sqlExportacao.contains("OFFSET :offsetTabela"));
        assertEquals(List.of("Cliente A"), jdbcTemplate.ultimoParametro("pagadores"));
    }

    @Test
    void serieTemporalAgrupaPorNivelEIncluiStatusEmpilhaveis() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        PerformanceDashboardSqlRepository repository = new PerformanceDashboardSqlRepository(
                jdbcTemplate,
                new ValidadorPeriodoService(),
                escopoTotal()
        );

        repository.buscarSerieTemporal(
                new FiltroConsultaDTO(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 5, 15),
                        Map.of()
                ),
                "dia",
                2026,
                5
        );

        assertTrue(jdbcTemplate.sqls.stream().anyMatch(sql -> sql
                .contains("SUM(CASE WHEN status_norm = N'Em Trânsito'")
                && sql.contains("SUM(CASE WHEN status_norm = N'Pendente'")
                && sql.contains("YEAR(data_previsao_entrega) = :anoTemporal")
                && sql.contains("MONTH(data_previsao_entrega) = :mesTemporal")
                && sql.contains("GROUP BY data_previsao_entrega")));
        assertEquals(2026L, jdbcTemplate.ultimoParametroLong("anoTemporal"));
        assertEquals(5L, jdbcTemplate.ultimoParametroLong("mesTemporal"));
    }

    @Test
    void historicoUsaPeriodoRecebidoNaCteBase() {
        CapturandoNamedParameterJdbcTemplate jdbcTemplate = new CapturandoNamedParameterJdbcTemplate();
        PerformanceDashboardSqlRepository repository = new PerformanceDashboardSqlRepository(
                jdbcTemplate,
                new ValidadorPeriodoService(),
                escopoTotal()
        );

        repository.buscarHistorico(
                new FiltroConsultaDTO(
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 5, 25),
                        Map.of()
                )
        );

        assertTrue(jdbcTemplate.sqls.stream().anyMatch(sql -> sql
                .contains("TRY_CONVERT(date, [Previsão de Entrega]) >= :dataInicio")
                && sql.contains("TRY_CONVERT(date, [Previsão de Entrega]) < :dataFim")
                && sql.contains("GROUP BY CONVERT(char(7), data_previsao_entrega, 23)")));
        assertEquals(Date.valueOf(LocalDate.of(2026, 3, 1)), jdbcTemplate.ultimoParametro("dataInicio"));
        assertEquals(Date.valueOf(LocalDate.of(2026, 5, 26)), jdbcTemplate.ultimoParametro("dataFim"));
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
        private SqlParameterSource ultimoParametro;

        private CapturandoNamedParameterJdbcTemplate() {
            super(new JdbcTemplate());
        }

        @Override
        public <T> T queryForObject(String sql, SqlParameterSource paramSource, Class<T> requiredType) {
            sqls.add(sql);
            ultimoParametro = paramSource;
            if (Long.class.equals(requiredType)) {
                return requiredType.cast(0L);
            }
            return null;
        }

        @Override
        public <T> List<T> queryForList(String sql, SqlParameterSource paramSource, Class<T> elementType) {
            sqls.add(sql);
            ultimoParametro = paramSource;
            return List.of(
                    elementType.cast("Nº Minuta"),
                    elementType.cast("Previsão de Entrega"),
                    elementType.cast("Data de Finalização"),
                    elementType.cast("Responsável pela Região de Destino"),
                    elementType.cast("Filial Emissora"),
                    elementType.cast("Kg Taxado"),
                    elementType.cast("Valor NF"),
                    elementType.cast("Status"),
                    elementType.cast("Data de extracao"),
                    elementType.cast("ID")
            );
        }

        @Override
        public <T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper) {
            sqls.add(sql);
            ultimoParametro = paramSource;
            return List.of();
        }

        private long ultimoParametroLong(String nome) {
            Object valor = ultimoParametro.getValue(nome);
            return valor instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(valor));
        }

        private Object ultimoParametro(String nome) {
            return ultimoParametro.getValue(nome);
        }
    }
}
