package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.ManifestosCostGoalEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManifestosCostGoalRepository extends JpaRepository<ManifestosCostGoalEntity, Long> {

    Optional<ManifestosCostGoalEntity> findByBranchIdAndYearMonth(String branchId, LocalDate yearMonth);

    @Query("""
            SELECT g
            FROM ManifestosCostGoalEntity g
            WHERE g.branchId IS NULL
              AND g.yearMonth = :yearMonth
            """)
    Optional<ManifestosCostGoalEntity> findGlobalByYearMonth(@Param("yearMonth") LocalDate yearMonth);

    @Query(value = """
            WITH metas_por_competencia AS (
                SELECT
                    year_month,
                    CASE
                        WHEN MAX(CASE WHEN branch_id IS NULL THEN 1 ELSE 0 END) = 1
                        THEN MAX(CASE WHEN branch_id IS NULL THEN cost_goal END)
                        ELSE SUM(CASE WHEN branch_id IS NOT NULL THEN cost_goal ELSE 0 END)
                    END AS cost_goal,
                    CASE
                        WHEN MAX(CASE WHEN branch_id IS NULL THEN 1 ELSE 0 END) = 1
                        THEN CAST(1 AS BIGINT)
                        ELSE COUNT_BIG(CASE WHEN branch_id IS NOT NULL THEN 1 END)
                    END AS configured_goals
                FROM acesso.manifestos_cost_goals
                WHERE year_month >= :inicioCompetencia
                  AND year_month < :fimCompetencia
                GROUP BY year_month
            )
            SELECT
                COALESCE(SUM(cost_goal), 0) AS costGoal,
                COALESCE(SUM(configured_goals), 0) AS configuredGoals
            FROM metas_por_competencia
            """, nativeQuery = true)
    GoalAggregateProjection aggregateGlobalOrBranches(
            @Param("inicioCompetencia") LocalDate inicioCompetencia,
            @Param("fimCompetencia") LocalDate fimCompetencia
    );

    @Query(value = """
            SELECT
                COALESCE(SUM(cost_goal), 0) AS costGoal,
                COUNT_BIG(1) AS configuredGoals
            FROM acesso.manifestos_cost_goals
            WHERE year_month >= :inicioCompetencia
              AND year_month < :fimCompetencia
              AND branch_id COLLATE Latin1_General_CI_AI IN (:branchIds)
            """, nativeQuery = true)
    GoalAggregateProjection aggregateByBranches(
            @Param("inicioCompetencia") LocalDate inicioCompetencia,
            @Param("fimCompetencia") LocalDate fimCompetencia,
            @Param("branchIds") Collection<String> branchIds
    );

    interface GoalAggregateProjection {
        BigDecimal getCostGoal();

        long getConfiguredGoals();
    }
}
