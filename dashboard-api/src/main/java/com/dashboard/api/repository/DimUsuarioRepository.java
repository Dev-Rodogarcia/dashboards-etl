package com.dashboard.api.repository;

import com.dashboard.api.model.DimUsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DimUsuarioRepository extends JpaRepository<DimUsuarioEntity, String> {
}
