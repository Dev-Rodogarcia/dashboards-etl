package com.dashboard.api.repository;

import com.dashboard.api.model.VisaoManifestosEntity;
import com.dashboard.api.model.VisaoManifestosId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.List;

public interface VisaoManifestosRepository extends JpaRepository<VisaoManifestosEntity, VisaoManifestosId>,
        JpaSpecificationExecutor<VisaoManifestosEntity> {

    List<VisaoManifestosEntity> findByDataCriacaoGreaterThanEqualAndDataCriacaoLessThan(
            OffsetDateTime inicioInclusivo,
            OffsetDateTime fimExclusivo
    );
}
