package com.dashboard.api.repository;

import com.dashboard.api.model.VisaoFretesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.List;

public interface VisaoFretesRepository extends JpaRepository<VisaoFretesEntity, Long>,
        JpaSpecificationExecutor<VisaoFretesEntity> {

    List<VisaoFretesEntity> findByDataFreteBetween(OffsetDateTime inicio, OffsetDateTime fim);
}
