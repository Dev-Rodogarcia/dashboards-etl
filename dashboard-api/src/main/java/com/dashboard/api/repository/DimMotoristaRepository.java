package com.dashboard.api.repository;

import com.dashboard.api.model.DimMotoristaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DimMotoristaRepository extends JpaRepository<DimMotoristaEntity, String> {
}
