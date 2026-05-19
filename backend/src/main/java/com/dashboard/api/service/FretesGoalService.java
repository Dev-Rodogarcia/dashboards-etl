package com.dashboard.api.service;

import com.dashboard.api.dto.fretes.FretesGoalBranchSummaryDTO;
import com.dashboard.api.dto.fretes.FretesGoalConfigDTO;
import com.dashboard.api.dto.fretes.FretesGoalConfigRequestDTO;
import com.dashboard.api.dto.fretes.FretesGoalSummaryDTO;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class FretesGoalService {

    public static final String GLOBAL_BRANCH_ID = "GLOBAL";
    private static final Logger log = LoggerFactory.getLogger(FretesGoalService.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UsuarioRepository usuarioRepository;

    public FretesGoalService(NamedParameterJdbcTemplate jdbcTemplate, UsuarioRepository usuarioRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public FretesGoalSummaryDTO buscarResumo(
            LocalDate dataInicio,
            LocalDate dataFim,
            Collection<FretesBranchRealizado> realizados,
            Collection<String> filiaisSelecionadas
    ) {
        Set<String> filiaisFiltradas = new LinkedHashSet<>();
        filiaisSelecionadas.stream()
                .map(this::normalizarBranchId)
                .filter(Objects::nonNull)
                .forEach(filiaisFiltradas::add);

        Set<String> branchIds = new LinkedHashSet<>(filiaisFiltradas);
        realizados.stream()
                .map(FretesBranchRealizado::branchId)
                .map(this::normalizarBranchId)
                .filter(Objects::nonNull)
                .forEach(branchIds::add);

        Map<String, FretesBranchRealizado> realizadoPorFilial = new LinkedHashMap<>();
        for (FretesBranchRealizado realizado : realizados) {
            String branchId = normalizarBranchId(realizado.branchId());
            if (branchId != null) {
                realizadoPorFilial.put(branchId, realizado);
            }
        }

        GoalPeriodResult metasPeriodo = tabelaMetasExiste()
                ? calcularMetasPeriodo(dataInicio, dataFim, branchIds, !filiaisFiltradas.isEmpty())
                : GoalPeriodResult.zero(branchIds);
        branchIds.addAll(metasPeriodo.branchIds());

        Map<String, GoalTotals> metasPorFilial = metasPeriodo.metasPorFilial();
        List<FretesGoalBranchSummaryDTO> branches = branchIds.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .map(branchId -> {
                    GoalTotals meta = metasPorFilial.getOrDefault(branchId, GoalTotals.zero());
                    FretesBranchRealizado realizado = realizadoPorFilial.getOrDefault(
                            branchId,
                            new FretesBranchRealizado(branchId, BigDecimal.ZERO, 0)
                    );
                    return toBranchSummary(branchId, meta, realizado);
                })
                .toList();

        BigDecimal metaFaturamento = metasPeriodo.aggregate().metaFaturamento()
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal realizadoFaturamento = branches.stream()
                .map(FretesGoalBranchSummaryDTO::realizadoFaturamento)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        int metaFretes = metasPeriodo.aggregate().metaFretes();
        int realizadoFretes = branches.stream().mapToInt(FretesGoalBranchSummaryDTO::realizadoFretes).sum();

        return new FretesGoalSummaryDTO(
                dataInicio.format(DATE_FMT),
                dataFim.format(DATE_FMT),
                metaFaturamento,
                realizadoFaturamento,
                percentual(realizadoFaturamento, metaFaturamento),
                metaFretes,
                realizadoFretes,
                percentual(realizadoFretes, metaFretes),
                branches
        );
    }

    public List<FretesGoalConfigDTO> buscarConfiguracoes(int ano, int mes) {
        validarAnoMes(ano, mes);

        try {
            if (!tabelaMetasExiste()) {
                return metaConfiguracaoFallback(ano, mes, "Meta não cadastrada");
            }

            String sql = """
                    SELECT g.branch_id, g.ano, g.mes, g.meta_faturamento, g.meta_fretes,
                           g.updated_at, u.nome AS updated_by_name
                    FROM acesso.fretes_goals g
                    LEFT JOIN acesso.usuarios u ON u.id = g.updated_by_user_id
                    WHERE g.ano = :ano AND g.mes = :mes
                    ORDER BY CASE WHEN g.branch_id IS NULL THEN 0 ELSE 1 END, g.branch_id
                    """;

            List<FretesGoalConfigDTO> rows = jdbcTemplate.query(sql, new MapSqlParameterSource()
                    .addValue("ano", ano)
                    .addValue("mes", mes), this::mapConfig);

            return rows.isEmpty()
                    ? metaConfiguracaoFallback(ano, mes, "Meta não cadastrada")
                    : rows;
        } catch (DataAccessException ex) {
            log.warn("Metas de fretes indisponíveis para {}/{}. Retornando fallback sem meta configurada. Motivo: {}", mes, ano, ex.getMessage());
            return metaConfiguracaoFallback(ano, mes, "Metas indisponíveis (API offline)");
        }
    }

    @Transactional
    public FretesGoalConfigDTO salvarConfiguracao(FretesGoalConfigRequestDTO request, String usuarioEmail) {
        garantirTabela();
        Objects.requireNonNull(request, "request é obrigatório.");
        validarAnoMes(request.ano(), request.mes());
        BigDecimal metaFaturamento = normalizarMetaFaturamento(request.metaFaturamento());
        int metaFretes = normalizarMetaFretes(request.metaFretes());
        String branchId = normalizarBranchId(request.branchId());
        UsuarioEntity usuario = usuarioAutenticado(usuarioEmail);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("branchId", branchId)
                .addValue("ano", request.ano())
                .addValue("mes", request.mes())
                .addValue("metaFaturamento", metaFaturamento)
                .addValue("metaFretes", metaFretes)
                .addValue("updatedByUserId", usuario.getId());

        jdbcTemplate.update("""
                MERGE acesso.fretes_goals AS target
                USING (SELECT :branchId AS branch_id, :ano AS ano, :mes AS mes) AS source
                   ON ((target.branch_id = source.branch_id) OR (target.branch_id IS NULL AND source.branch_id IS NULL))
                  AND target.ano = source.ano
                  AND target.mes = source.mes
                WHEN MATCHED THEN
                    UPDATE SET meta_faturamento = :metaFaturamento,
                               meta_fretes = :metaFretes,
                               updated_by_user_id = :updatedByUserId,
                               updated_at = SYSUTCDATETIME()
                WHEN NOT MATCHED THEN
                    INSERT (branch_id, ano, mes, meta_faturamento, meta_fretes, updated_by_user_id)
                    VALUES (:branchId, :ano, :mes, :metaFaturamento, :metaFretes, :updatedByUserId);
                """, params);

        return buscarConfiguracao(branchId, request.ano(), request.mes())
                .orElseThrow(() -> new IllegalStateException("Meta salva, mas não localizada para retorno."));
    }

    @Transactional
    public void removerConfiguracao(String branchId, int ano, int mes) {
        garantirTabela();
        validarAnoMes(ano, mes);
        String normalizedBranchId = normalizarBranchId(branchId);
        jdbcTemplate.update("""
                DELETE FROM acesso.fretes_goals
                WHERE ano = :ano
                  AND mes = :mes
                  AND ((branch_id = :branchId) OR (branch_id IS NULL AND :branchId IS NULL))
                """, new MapSqlParameterSource()
                .addValue("branchId", normalizedBranchId)
                .addValue("ano", ano)
                .addValue("mes", mes));
    }

    @Transactional
    public void garantirTabela() {
        jdbcTemplate.getJdbcTemplate().execute("""
                IF OBJECT_ID(N'acesso.fretes_goals', N'U') IS NULL
                BEGIN
                    CREATE TABLE acesso.fretes_goals (
                        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_fretes_goals PRIMARY KEY,
                        branch_id NVARCHAR(120) NULL,
                        ano SMALLINT NOT NULL,
                        mes TINYINT NOT NULL,
                        meta_faturamento DECIMAL(18,2) NOT NULL CONSTRAINT DF_fretes_goals_meta_faturamento DEFAULT 0,
                        meta_fretes INT NOT NULL CONSTRAINT DF_fretes_goals_meta_fretes DEFAULT 0,
                        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_fretes_goals_created_at DEFAULT SYSUTCDATETIME(),
                        updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_fretes_goals_updated_at DEFAULT SYSUTCDATETIME(),
                        updated_by_user_id BIGINT NULL,
                        CONSTRAINT CK_fretes_goals_mes CHECK (mes BETWEEN 1 AND 12),
                        CONSTRAINT CK_fretes_goals_meta_faturamento CHECK (meta_faturamento >= 0),
                        CONSTRAINT CK_fretes_goals_meta_fretes CHECK (meta_fretes >= 0)
                    );
                END
                """);
        jdbcTemplate.getJdbcTemplate().execute("""
                IF NOT EXISTS (
                    SELECT 1 FROM sys.indexes
                    WHERE name = N'UX_fretes_goals_branch_period'
                      AND object_id = OBJECT_ID(N'acesso.fretes_goals', N'U')
                )
                BEGIN
                    CREATE UNIQUE INDEX UX_fretes_goals_branch_period
                    ON acesso.fretes_goals (branch_id, ano, mes)
                    WHERE branch_id IS NOT NULL;
                END
                """);
        jdbcTemplate.getJdbcTemplate().execute("""
                IF NOT EXISTS (
                    SELECT 1 FROM sys.indexes
                    WHERE name = N'UX_fretes_goals_global_period'
                      AND object_id = OBJECT_ID(N'acesso.fretes_goals', N'U')
                )
                BEGIN
                    CREATE UNIQUE INDEX UX_fretes_goals_global_period
                    ON acesso.fretes_goals (ano, mes)
                    WHERE branch_id IS NULL;
                END
                """);
    }

    private GoalPeriodResult calcularMetasPeriodo(LocalDate dataInicio, LocalDate dataFim, Set<String> branchIdsBase, boolean filtroFilialAtivo) {
        List<YearMonthSlice> slices = monthSlices(dataInicio, dataFim);
        Set<Integer> anos = slices.stream().map(YearMonthSlice::ano).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<Integer> meses = slices.stream().map(YearMonthSlice::mes).distinct().toList();

        String sql = """
                SELECT branch_id, ano, mes, meta_faturamento, meta_fretes
                FROM acesso.fretes_goals
                WHERE ano IN (:anos)
                  AND mes IN (:meses)
                """;

        Map<GoalKey, GoalTotals> goals = jdbcTemplate.query(sql, new MapSqlParameterSource()
                        .addValue("anos", anos)
                        .addValue("meses", meses),
                (rs, rowNum) -> Map.entry(
                        new GoalKey(rs.getString("branch_id"), rs.getInt("ano"), rs.getInt("mes")),
                        new GoalTotals(rs.getBigDecimal("meta_faturamento"), rs.getInt("meta_fretes"))
                ))
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));

        Set<String> branchIds = new LinkedHashSet<>(branchIdsBase);
        Set<String> filiaisComMetaEspecifica = goals.keySet().stream()
                .map(GoalKey::branchId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!filtroFilialAtivo) {
            branchIds.addAll(filiaisComMetaEspecifica);
        }

        Map<String, GoalTotals> result = new LinkedHashMap<>();
        branchIds.forEach(branchId -> result.put(branchId, GoalTotals.zero()));

        for (String branchId : branchIds) {
            GoalTotals total = GoalTotals.zero();
            for (YearMonthSlice slice : slices) {
                GoalTotals monthly = goals.getOrDefault(new GoalKey(branchId, slice.ano(), slice.mes()), GoalTotals.zero());
                total = total.add(monthly);
            }
            result.put(branchId, total);
        }

        GoalTotals aggregate = GoalTotals.zero();
        for (YearMonthSlice slice : slices) {
            GoalTotals global = goals.get(new GoalKey(null, slice.ano(), slice.mes()));
            if (!filtroFilialAtivo && global != null) {
                aggregate = aggregate.add(global);
                continue;
            }

            GoalTotals subtotalFiliais = GoalTotals.zero();
            for (String branchId : branchIds) {
                subtotalFiliais = subtotalFiliais.add(goals
                        .getOrDefault(new GoalKey(branchId, slice.ano(), slice.mes()), GoalTotals.zero()));
            }
            aggregate = aggregate.add(subtotalFiliais);
        }

        return new GoalPeriodResult(result, aggregate, branchIds);
    }

    private boolean tabelaMetasExiste() {
        Integer exists = jdbcTemplate.getJdbcTemplate().queryForObject("""
                SELECT CASE WHEN OBJECT_ID(N'acesso.fretes_goals', N'U') IS NULL THEN 0 ELSE 1 END
                """, Integer.class);
        return exists != null && exists == 1;
    }

    private List<YearMonthSlice> monthSlices(LocalDate dataInicio, LocalDate dataFim) {
        List<YearMonthSlice> result = new ArrayList<>();
        LocalDate cursor = dataInicio.withDayOfMonth(1);
        LocalDate fimMes = dataFim.withDayOfMonth(1);

        while (!cursor.isAfter(fimMes)) {
            result.add(new YearMonthSlice(cursor.getYear(), cursor.getMonthValue()));
            cursor = cursor.plusMonths(1);
        }

        return result;
    }

    private FretesGoalBranchSummaryDTO toBranchSummary(String branchId, GoalTotals meta, FretesBranchRealizado realizado) {
        BigDecimal metaFaturamento = meta.metaFaturamento().setScale(2, RoundingMode.HALF_UP);
        BigDecimal realizadoFaturamento = realizado.realizadoFaturamento().setScale(2, RoundingMode.HALF_UP);
        int metaFretes = meta.metaFretes();
        int realizadoFretes = realizado.realizadoFretes();
        return new FretesGoalBranchSummaryDTO(
                branchId,
                metaFaturamento,
                realizadoFaturamento,
                percentual(realizadoFaturamento, metaFaturamento),
                metaFretes,
                realizadoFretes,
                percentual(realizadoFretes, metaFretes)
        );
    }

    private Optional<FretesGoalConfigDTO> buscarConfiguracao(String branchId, int ano, int mes) {
        String sql = """
                SELECT g.branch_id, g.ano, g.mes, g.meta_faturamento, g.meta_fretes,
                       g.updated_at, u.nome AS updated_by_name
                FROM acesso.fretes_goals g
                LEFT JOIN acesso.usuarios u ON u.id = g.updated_by_user_id
                WHERE g.ano = :ano
                  AND g.mes = :mes
                  AND ((g.branch_id = :branchId) OR (g.branch_id IS NULL AND :branchId IS NULL))
                """;
        List<FretesGoalConfigDTO> rows = jdbcTemplate.query(sql, new MapSqlParameterSource()
                .addValue("branchId", branchId)
                .addValue("ano", ano)
                .addValue("mes", mes), this::mapConfig);
        return rows.stream().findFirst();
    }

    private FretesGoalConfigDTO mapConfig(ResultSet rs, int rowNum) throws SQLException {
        String branchId = rs.getString("branch_id");
        java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new FretesGoalConfigDTO(
                branchId == null ? GLOBAL_BRANCH_ID : branchId,
                rs.getInt("ano"),
                rs.getInt("mes"),
                rs.getBigDecimal("meta_faturamento").setScale(2, RoundingMode.HALF_UP),
                rs.getInt("meta_fretes"),
                updatedAt != null ? updatedAt.toLocalDateTime().format(DATE_TIME_FMT) : null,
                rs.getString("updated_by_name"),
                true,
                null
        );
    }

    private List<FretesGoalConfigDTO> metaConfiguracaoFallback(int ano, int mes, String mensagem) {
        return List.of(new FretesGoalConfigDTO(
                GLOBAL_BRANCH_ID,
                ano,
                mes,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                0,
                null,
                null,
                false,
                mensagem
        ));
    }

    private String normalizarBranchId(String branchId) {
        String normalized = branchId == null || branchId.isBlank() ? GLOBAL_BRANCH_ID : branchId.trim();
        if (GLOBAL_BRANCH_ID.equalsIgnoreCase(normalized)) {
            return null;
        }
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("Filial da meta excede 120 caracteres.");
        }
        return normalized;
    }

    private void validarAnoMes(int ano, int mes) {
        if (ano < 2000 || ano > 2100) {
            throw new IllegalArgumentException("Ano da meta deve estar entre 2000 e 2100.");
        }
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mês da meta deve estar entre 1 e 12.");
        }
    }

    private BigDecimal normalizarMetaFaturamento(BigDecimal value) {
        BigDecimal normalized = Optional.ofNullable(value).orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Meta de faturamento não pode ser negativa.");
        }
        return normalized;
    }

    private int normalizarMetaFretes(Integer value) {
        int normalized = Optional.ofNullable(value).orElse(0);
        if (normalized < 0) {
            throw new IllegalArgumentException("Meta de fretes não pode ser negativa.");
        }
        return normalized;
    }

    private double percentual(BigDecimal realizado, BigDecimal meta) {
        if (meta == null || meta.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.doubleValue();
        }
        return realizado
                .multiply(BigDecimal.valueOf(100))
                .divide(meta, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double percentual(int realizado, int meta) {
        if (meta <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(realizado)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(meta), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private UsuarioEntity usuarioAutenticado(String usuarioEmail) {
        return usuarioRepository.findByEmailIgnoreCase(Objects.toString(usuarioEmail, ""))
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
    }

    public record FretesBranchRealizado(
            String branchId,
            BigDecimal realizadoFaturamento,
            int realizadoFretes
    ) {
    }

    private record GoalKey(String branchId, int ano, int mes) {
    }

    private record YearMonthSlice(int ano, int mes) {
    }

    private record GoalPeriodResult(Map<String, GoalTotals> metasPorFilial, GoalTotals aggregate, Set<String> branchIds) {
        static GoalPeriodResult zero(Set<String> branchIds) {
            Map<String, GoalTotals> result = new LinkedHashMap<>();
            branchIds.forEach(branchId -> result.put(branchId, GoalTotals.zero()));
            return new GoalPeriodResult(result, GoalTotals.zero(), new LinkedHashSet<>(branchIds));
        }
    }

    private record GoalTotals(BigDecimal metaFaturamento, int metaFretes) {
        static GoalTotals zero() {
            return new GoalTotals(BigDecimal.ZERO, 0);
        }

        GoalTotals add(GoalTotals other) {
            return new GoalTotals(
                    metaFaturamento.add(other.metaFaturamento),
                    metaFretes + other.metaFretes
            );
        }

    }
}
