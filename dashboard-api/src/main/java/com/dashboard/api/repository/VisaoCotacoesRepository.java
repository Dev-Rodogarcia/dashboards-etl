package com.dashboard.api.repository;

import com.dashboard.api.model.VisaoCotacoesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.List;

public interface VisaoCotacoesRepository extends JpaRepository<VisaoCotacoesEntity, Long>,
        JpaSpecificationExecutor<VisaoCotacoesEntity> {

    List<VisaoCotacoesEntity> findByDataCotacaoBetween(OffsetDateTime inicio, OffsetDateTime fim);
}
