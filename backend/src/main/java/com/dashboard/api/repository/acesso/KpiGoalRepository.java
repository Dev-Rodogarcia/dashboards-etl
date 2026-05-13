package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.KpiGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface KpiGoalRepository extends JpaRepository<KpiGoalEntity, Long> {
    List<KpiGoalEntity> findAllByBranchIdIn(Collection<String> branchIds);
    List<KpiGoalEntity> findAllByBranchId(String branchId);
    Optional<KpiGoalEntity> findByBranchIdAndIndicatorKey(String branchId, String indicatorKey);

    @Query("SELECT g FROM KpiGoalEntity g WHERE g.branchId IS NULL")
    List<KpiGoalEntity> findGlobalGoals();

    @Query("SELECT g FROM KpiGoalEntity g WHERE g.branchId IS NULL AND g.indicatorKey = :indicatorKey")
    Optional<KpiGoalEntity> findGlobalGoalByIndicatorKey(@Param("indicatorKey") String indicatorKey);

    @Query("SELECT g FROM KpiGoalEntity g WHERE g.branchId IS NOT NULL")
    List<KpiGoalEntity> findAllBranchOverrides();

    @Query("SELECT g FROM KpiGoalEntity g WHERE g.branchId IS NOT NULL AND g.indicatorKey = :indicatorKey")
    List<KpiGoalEntity> findAllBranchOverridesByIndicatorKey(@Param("indicatorKey") String indicatorKey);
}
