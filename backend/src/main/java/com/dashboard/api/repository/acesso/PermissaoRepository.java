package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.PermissaoEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PermissaoRepository extends JpaRepository<PermissaoEntity, Long> {
    Optional<PermissaoEntity> findByChave(String chave);
    Optional<PermissaoEntity> findByChaveLegado(String chaveLegado);
    List<PermissaoEntity> findAllByAtivoTrue();

    @Query("""
            SELECT p.id AS id,
                   COALESCE(p.chaveLegado, p.chave) AS chave
            FROM PermissaoEntity p
            WHERE p.ativo = true
            ORDER BY COALESCE(p.chaveLegado, p.chave)
            """)
    List<PermissaoResumoProjection> findCatalogoAtivoResumo();

    interface PermissaoResumoProjection {
        Long getId();
        String getChave();
    }
}
