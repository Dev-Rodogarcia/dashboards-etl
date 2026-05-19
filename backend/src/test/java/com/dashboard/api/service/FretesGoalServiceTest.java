package com.dashboard.api.service;

import com.dashboard.api.dto.fretes.FretesGoalBranchSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.AbstractDataSource;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FretesGoalServiceTest {

    private FakeNamedParameterJdbcTemplate jdbcTemplate;
    private FretesGoalService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new FakeNamedParameterJdbcTemplate();
        service = new FretesGoalService(jdbcTemplate, null);
    }

    @Test
    void buscarResumoUsaMetaGlobalMensalSemRatearPelosDiasNemMultiplicarPelasFiliais() {
        metas(new MetaRow(null, 2026, 5, "700000.00", 6000000));

        var resumo = service.buscarResumo(
                LocalDate.of(2026, 4, 19),
                LocalDate.of(2026, 5, 19),
                List.of(realizado("SPO", "1000.00", 10), realizado("REC", "2000.00", 20)),
                List.of()
        );

        assertThat(resumo.metaFaturamento()).isEqualByComparingTo("700000.00");
        assertThat(resumo.metaFretes()).isEqualTo(6000000);
        assertThat(resumo.realizadoFaturamento()).isEqualByComparingTo("3000.00");
        assertThat(resumo.realizadoFretes()).isEqualTo(30);
        assertThat(resumo.branches())
                .extracting(FretesGoalBranchSummaryDTO::metaFretes)
                .containsOnly(0);
    }

    @Test
    void buscarResumoSomaMetasEspecificasQuandoNaoExisteMetaGlobal() {
        metas(
                new MetaRow("REC", 2026, 5, "1000.00", 10),
                new MetaRow("SPO", 2026, 5, "2000.00", 20)
        );

        var resumo = service.buscarResumo(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                List.of(realizado("SPO", "3000.00", 30), realizado("REC", "1500.00", 15)),
                List.of()
        );

        assertThat(resumo.metaFaturamento()).isEqualByComparingTo("3000.00");
        assertThat(resumo.metaFretes()).isEqualTo(30);
        assertThat(resumo.branches()).extracting(FretesGoalBranchSummaryDTO::branchId)
                .containsExactly("REC", "SPO");
        assertThat(resumo.branches()).extracting(FretesGoalBranchSummaryDTO::metaFaturamento)
                .containsExactly(new BigDecimal("1000.00"), new BigDecimal("2000.00"));
    }

    @Test
    void buscarResumoComFiltroDeFilialNaoUsaMetaGlobalComoMetaDaFilial() {
        metas(new MetaRow(null, 2026, 5, "700000.00", 6000000));

        var resumo = service.buscarResumo(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                List.of(realizado("SPO", "3000.00", 30)),
                List.of("SPO")
        );

        assertThat(resumo.metaFaturamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.metaFretes()).isZero();
        assertThat(resumo.branches()).singleElement().satisfies(branch -> {
            assertThat(branch.branchId()).isEqualTo("SPO");
            assertThat(branch.metaFaturamento()).isEqualByComparingTo("0.00");
            assertThat(branch.metaFretes()).isZero();
        });
    }

    @Test
    void buscarResumoComFiltroDeFilialUsaMetaEspecificaDaFilial() {
        metas(
                new MetaRow(null, 2026, 5, "700000.00", 6000000),
                new MetaRow("SPO", 2026, 5, "1000.00", 10)
        );

        var resumo = service.buscarResumo(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                List.of(realizado("SPO", "3000.00", 30)),
                List.of("SPO")
        );

        assertThat(resumo.metaFaturamento()).isEqualByComparingTo("1000.00");
        assertThat(resumo.metaFretes()).isEqualTo(10);
        assertThat(resumo.branches()).singleElement().satisfies(branch -> {
            assertThat(branch.branchId()).isEqualTo("SPO");
            assertThat(branch.metaFaturamento()).isEqualByComparingTo("1000.00");
            assertThat(branch.metaFretes()).isEqualTo(10);
        });
    }

    private FretesGoalService.FretesBranchRealizado realizado(String branchId, String faturamento, int fretes) {
        return new FretesGoalService.FretesBranchRealizado(branchId, new BigDecimal(faturamento), fretes);
    }

    private void metas(MetaRow... rows) {
        jdbcTemplate.setRows(Arrays.asList(rows));
    }

    private static ResultSet resultSet(MetaRow row) {
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getString" -> "branch_id".equals(args[0]) ? row.branchId() : null;
                    case "getInt" -> switch (String.valueOf(args[0])) {
                        case "ano" -> row.ano();
                        case "mes" -> row.mes();
                        case "meta_fretes" -> row.metaFretes();
                        default -> 0;
                    };
                    case "getBigDecimal" -> "meta_faturamento".equals(args[0]) ? new BigDecimal(row.metaFaturamento()) : BigDecimal.ZERO;
                    case "wasNull" -> false;
                    case "close" -> null;
                    case "isClosed" -> false;
                    case "toString" -> "MetaRowResultSet";
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class FakeNamedParameterJdbcTemplate extends NamedParameterJdbcTemplate {

        private final FakeJdbcTemplate jdbcTemplate;
        private List<MetaRow> rows = List.of();

        FakeNamedParameterJdbcTemplate() {
            this(new FakeJdbcTemplate());
        }

        private FakeNamedParameterJdbcTemplate(FakeJdbcTemplate jdbcTemplate) {
            super(jdbcTemplate);
            this.jdbcTemplate = jdbcTemplate;
        }

        void setRows(List<MetaRow> rows) {
            this.rows = rows;
        }

        @Override
        public JdbcTemplate getJdbcTemplate() {
            return jdbcTemplate;
        }

        @Override
        public <T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper) {
            List<T> mappedRows = new ArrayList<>();
            for (int index = 0; index < rows.size(); index++) {
                try {
                    mappedRows.add(rowMapper.mapRow(resultSet(rows.get(index)), index));
                } catch (SQLException ex) {
                    throw new IllegalStateException(ex);
                }
            }
            return mappedRows;
        }
    }

    private static final class FakeJdbcTemplate extends JdbcTemplate {

        FakeJdbcTemplate() {
            super(new ThrowingDataSource());
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType) {
            return requiredType.cast(1);
        }
    }

    private static final class ThrowingDataSource extends AbstractDataSource {

        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException("DataSource fake usado apenas para testes de unidade.");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            throw new SQLException("DataSource fake usado apenas para testes de unidade.");
        }
    }

    private record MetaRow(String branchId, int ano, int mes, String metaFaturamento, int metaFretes) {
    }
}
