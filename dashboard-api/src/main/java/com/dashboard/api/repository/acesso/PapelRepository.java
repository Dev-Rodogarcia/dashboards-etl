package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.PapelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PapelRepository extends JpaRepository<PapelEntity, Long> {
    Optional<PapelEntity> findByNome(String nome);
}
