package com.dashboard.api.repository;

import com.dashboard.api.model.DimClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DimClienteRepository extends JpaRepository<DimClienteEntity, String> {
}
