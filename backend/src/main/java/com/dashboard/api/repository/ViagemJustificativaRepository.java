package com.dashboard.api.repository;

import com.dashboard.api.model.ViagemJustificativa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViagemJustificativaRepository extends JpaRepository<ViagemJustificativa, Long> {
    Optional<ViagemJustificativa> findByCodSolicitacao(Long codSolicitacao);
}
