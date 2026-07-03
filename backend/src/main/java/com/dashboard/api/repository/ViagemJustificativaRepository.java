package com.dashboard.api.repository;

import com.dashboard.api.model.ViagemJustificativa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ViagemJustificativaRepository extends JpaRepository<ViagemJustificativa, Long> {
    Optional<ViagemJustificativa> findByCodSolicitacao(Long codSolicitacao);

    @Modifying
    @Query("delete from ViagemJustificativa v where v.codSolicitacao = :codSolicitacao")
    int deleteByCodSolicitacao(@Param("codSolicitacao") Long codSolicitacao);
}
