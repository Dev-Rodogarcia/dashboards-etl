package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.ManifestosCostGoalEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManifestosCostGoalRepository extends JpaRepository<ManifestosCostGoalEntity, Long> {

    long countByYearMonth(LocalDate yearMonth);

    @Query("""
            SELECT g
            FROM ManifestosCostGoalEntity g
            WHERE UPPER(g.branchId) = :branchId
              AND g.yearMonth = :yearMonth
              AND UPPER(g.contractTypeKey) = :contractTypeKey
              AND (
                    (:classificationKey IS NULL AND g.classificationKey IS NULL)
                    OR UPPER(g.classificationKey) = :classificationKey
              )
            """)
    Optional<ManifestosCostGoalEntity> findByBranchIdAndYearMonthAndContractTypeKeyAndClassificationKey(
            @Param("branchId") String branchId,
            @Param("yearMonth") LocalDate yearMonth,
            @Param("contractTypeKey") String contractTypeKey,
            @Param("classificationKey") String classificationKey
    );

    @Query("""
            SELECT g
            FROM ManifestosCostGoalEntity g
            LEFT JOIN FETCH g.updatedByUser
            WHERE g.yearMonth = :yearMonth
            ORDER BY CASE WHEN g.branchId IS NULL THEN 0 ELSE 1 END,
                     g.branchId,
                     g.contractType,
                     g.classificationKey
            """)
    List<ManifestosCostGoalEntity> findAllByYearMonthOrdered(@Param("yearMonth") LocalDate yearMonth);

    @Query("""
            SELECT g
            FROM ManifestosCostGoalEntity g
            LEFT JOIN FETCH g.updatedByUser
            WHERE g.yearMonth = :yearMonth
              AND g.branchId = :branchId
            ORDER BY g.contractType,
                     g.classificationKey
            """)
    List<ManifestosCostGoalEntity> findAllByBranchIdAndYearMonthOrdered(
            @Param("branchId") String branchId,
            @Param("yearMonth") LocalDate yearMonth
    );

    @Query("""
            SELECT g
            FROM ManifestosCostGoalEntity g
            LEFT JOIN FETCH g.updatedByUser
            WHERE g.yearMonth = :yearMonth
              AND g.branchId IS NULL
            ORDER BY g.contractType,
                     g.classificationKey
            """)
    List<ManifestosCostGoalEntity> findGlobalByYearMonthOrdered(@Param("yearMonth") LocalDate yearMonth);

    @Query("""
            SELECT g
            FROM ManifestosCostGoalEntity g
            WHERE g.yearMonth = :yearMonth
            ORDER BY CASE WHEN g.branchId IS NULL THEN 0 ELSE 1 END,
                     g.branchId,
                     g.contractType,
                     g.classificationKey
            """)
    List<ManifestosCostGoalEntity> findAllByYearMonthForReplication(@Param("yearMonth") LocalDate yearMonth);

    @Query("""
            SELECT g
            FROM ManifestosCostGoalEntity g
            LEFT JOIN FETCH g.updatedByUser
            WHERE g.branchId IS NULL
              AND g.yearMonth = :yearMonth
              AND UPPER(g.contractTypeKey) = :contractTypeKey
              AND (
                    (:classificationKey IS NULL AND g.classificationKey IS NULL)
                    OR UPPER(g.classificationKey) = :classificationKey
              )
            """)
    Optional<ManifestosCostGoalEntity> findGlobalByYearMonthAndContractTypeKeyAndClassificationKey(
            @Param("yearMonth") LocalDate yearMonth,
            @Param("contractTypeKey") String contractTypeKey,
            @Param("classificationKey") String classificationKey
    );

    @Query(value = """
            WITH metas_por_competencia AS (
                SELECT
                    year_month,
                    CASE
                        WHEN MAX(CASE WHEN branch_id IS NULL THEN 1 ELSE 0 END) = 1
                        THEN SUM(CASE WHEN branch_id IS NULL THEN cost_goal ELSE 0 END)
                        ELSE SUM(CASE WHEN branch_id IS NOT NULL THEN cost_goal ELSE 0 END)
                    END AS cost_goal,
                    CASE
                        WHEN MAX(CASE WHEN branch_id IS NULL THEN 1 ELSE 0 END) = 1
                        THEN COUNT_BIG(CASE WHEN branch_id IS NULL THEN 1 END)
                        ELSE COUNT_BIG(CASE WHEN branch_id IS NOT NULL THEN 1 END)
                    END AS configured_goals
                FROM acesso.manifestos_cost_goals
                WHERE year_month >= :inicioCompetencia
                  AND year_month < :fimCompetencia
                  AND (
                        :contractTypeFilterActive = 0
                        OR UPPER(LTRIM(RTRIM(contract_type_key))) IN (:contractTypeKeys)
                  )
                  AND (
                        :classificationFilterActive = 0
                        OR UPPER(LTRIM(RTRIM(classification_key))) IN (:classificationKeys)
                  )
                GROUP BY year_month
            )
            SELECT
                COALESCE(SUM(cost_goal), 0) AS costGoal,
                COALESCE(SUM(configured_goals), 0) AS configuredGoals
            FROM metas_por_competencia
            """, nativeQuery = true)
    GoalAggregateProjection aggregateGlobalOrBranches(
            @Param("inicioCompetencia") LocalDate inicioCompetencia,
            @Param("fimCompetencia") LocalDate fimCompetencia,
            @Param("contractTypeKeys") Collection<String> contractTypeKeys,
            @Param("contractTypeFilterActive") int contractTypeFilterActive,
            @Param("classificationKeys") Collection<String> classificationKeys,
            @Param("classificationFilterActive") int classificationFilterActive
    );

    @Query(value = """
            SELECT
                COALESCE(SUM(cost_goal), 0) AS costGoal,
                COUNT_BIG(1) AS configuredGoals
            FROM acesso.manifestos_cost_goals
            WHERE year_month >= :inicioCompetencia
              AND year_month < :fimCompetencia
              AND branch_id COLLATE Latin1_General_CI_AI IN (:branchIds)
              AND (
                    :contractTypeFilterActive = 0
                    OR UPPER(LTRIM(RTRIM(contract_type_key))) IN (:contractTypeKeys)
              )
              AND (
                    :classificationFilterActive = 0
                    OR UPPER(LTRIM(RTRIM(classification_key))) IN (:classificationKeys)
              )
            """, nativeQuery = true)
    GoalAggregateProjection aggregateByBranches(
            @Param("inicioCompetencia") LocalDate inicioCompetencia,
            @Param("fimCompetencia") LocalDate fimCompetencia,
            @Param("branchIds") Collection<String> branchIds,
            @Param("contractTypeKeys") Collection<String> contractTypeKeys,
            @Param("contractTypeFilterActive") int contractTypeFilterActive,
            @Param("classificationKeys") Collection<String> classificationKeys,
            @Param("classificationFilterActive") int classificationFilterActive
    );

    interface GoalAggregateProjection {
        BigDecimal getCostGoal();

        long getConfiguredGoals();
    }
}
