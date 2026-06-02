package com.dashboard.api.repository.acesso;

import com.dashboard.api.dto.acesso.PapelDTO;
import com.dashboard.api.model.acesso.PapelEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PapelRepository extends JpaRepository<PapelEntity, Long> {
    Optional<PapelEntity> findByNome(String nome);

    @Query("""
            SELECT new com.dashboard.api.dto.acesso.PapelDTO(p.id, p.nome, p.descricao, p.nivel)
            FROM PapelEntity p
            WHERE p.ativo = true
              AND p.nome <> :papelExcluido
            ORDER BY p.nivel DESC
            """)
    List<PapelDTO> findDtosAtivosExceto(@Param("papelExcluido") String papelExcluido);

    @Query("""
            SELECT new com.dashboard.api.dto.acesso.PapelDTO(p.id, p.nome, p.descricao, p.nivel)
            FROM PapelEntity p
            WHERE p.ativo = true
              AND p.nome = :papelPermitido
              AND p.nome <> :papelExcluido
            ORDER BY p.nivel DESC
            """)
    List<PapelDTO> findDtosAtivosPorNomeExceto(
            @Param("papelPermitido") String papelPermitido,
            @Param("papelExcluido") String papelExcluido
    );
}
