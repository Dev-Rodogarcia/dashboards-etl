package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.SetorEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SetorRepository extends JpaRepository<SetorEntity, Long> {
    Optional<SetorEntity> findByChave(String chave);
    boolean existsByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
    List<SetorEntity> findAllByAtivoTrue();

    @Query("""
            SELECT s.id AS setorId,
                   filial AS filialNome
            FROM SetorEntity s
            JOIN s.filiaisPermitidas filial
            WHERE s.id IN :setorIds
            ORDER BY s.id ASC, filial ASC
            """)
    List<SetorFilialProjection> findFiliaisPermitidasPorSetores(@Param("setorIds") Collection<Long> setorIds);

    interface SetorFilialProjection {
        Long getSetorId();
        String getFilialNome();
    }
}
