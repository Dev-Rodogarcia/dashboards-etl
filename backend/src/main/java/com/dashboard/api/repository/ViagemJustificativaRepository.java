package com.dashboard.api.repository;

import com.dashboard.api.model.ViagemJustificativa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ViagemJustificativaRepository extends JpaRepository<ViagemJustificativa, Long> {
    Optional<ViagemJustificativa> findByCodSolicitacao(Long codSolicitacao);

    @Query(value = """
            SELECT TOP (1)
                id,
                cod_solicitacao,
                justificativa,
                criado_em,
                criado_por,
                ativo
            FROM dbo.viagem_justificativas
            WHERE cod_solicitacao = :codSolicitacao
            ORDER BY id DESC
            """, nativeQuery = true)
    Optional<ViagemJustificativa> findAnyByCodSolicitacao(@Param("codSolicitacao") Long codSolicitacao);
}
