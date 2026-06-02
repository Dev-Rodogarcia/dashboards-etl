package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.KpiGoalHistoryEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KpiGoalHistoryRepository extends JpaRepository<KpiGoalHistoryEntity, Long> {
    Page<KpiGoalHistoryEntity> findAllByOrderByUpdatedAtDesc(Pageable pageable);
    Page<KpiGoalHistoryEntity> findByBranchIdOrderByUpdatedAtDesc(String branchId, Pageable pageable);
    Page<KpiGoalHistoryEntity> findByBranchIdIsNullOrderByUpdatedAtDesc(Pageable pageable);
    long countByBranchId(String branchId);
    long countByBranchIdIsNull();
    List<KpiGoalHistoryEntity> findByBranchIdOrderByUpdatedAtAsc(String branchId, Pageable pageable);
    List<KpiGoalHistoryEntity> findByBranchIdIsNullOrderByUpdatedAtAsc(Pageable pageable);
}
