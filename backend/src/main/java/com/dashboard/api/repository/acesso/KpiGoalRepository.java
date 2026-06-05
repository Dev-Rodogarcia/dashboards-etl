package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.KpiGoalEntity;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KpiGoalRepository extends JpaRepository<KpiGoalEntity, Long> {
    List<KpiGoalEntity> findAllByBranchIdInAndCompetencia(Collection<String> branchIds, LocalDate competencia);
    List<KpiGoalEntity> findAllByBranchIdAndCompetencia(String branchId, LocalDate competencia);
    Optional<KpiGoalEntity> findByBranchIdAndIndicatorKeyAndCompetencia(String branchId, String indicatorKey, LocalDate competencia);

    @Query("SELECT g FROM KpiGoalEntity g WHERE g.branchId IS NULL AND g.competencia = :competencia")
    List<KpiGoalEntity> findGlobalGoalsByCompetencia(@Param("competencia") LocalDate competencia);

    @Query("""
            SELECT g FROM KpiGoalEntity g
            WHERE g.branchId IS NULL
              AND g.indicatorKey = :indicatorKey
              AND g.competencia = :competencia
            """)
    Optional<KpiGoalEntity> findGlobalGoalByIndicatorKeyAndCompetencia(
            @Param("indicatorKey") String indicatorKey,
            @Param("competencia") LocalDate competencia
    );

    @Query("SELECT g FROM KpiGoalEntity g WHERE g.branchId IS NOT NULL AND g.competencia = :competencia")
    List<KpiGoalEntity> findAllBranchOverridesByCompetencia(@Param("competencia") LocalDate competencia);

    @Query("""
            SELECT g FROM KpiGoalEntity g
            WHERE g.branchId IS NOT NULL
              AND g.indicatorKey = :indicatorKey
              AND g.competencia = :competencia
            """)
    List<KpiGoalEntity> findAllBranchOverridesByIndicatorKeyAndCompetencia(
            @Param("indicatorKey") String indicatorKey,
            @Param("competencia") LocalDate competencia
    );
}
