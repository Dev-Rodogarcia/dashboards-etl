package com.dashboard.api.repository;

import com.dashboard.api.model.VisaoContasAPagarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface VisaoContasAPagarRepository extends JpaRepository<VisaoContasAPagarEntity, Long>,
        JpaSpecificationExecutor<VisaoContasAPagarEntity> {

    List<VisaoContasAPagarEntity> findByEmissaoBetween(LocalDate inicio, LocalDate fim);
}
