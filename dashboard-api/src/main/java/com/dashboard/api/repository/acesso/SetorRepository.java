package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.SetorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SetorRepository extends JpaRepository<SetorEntity, Long> {
    Optional<SetorEntity> findByChave(String chave);
    boolean existsByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
    List<SetorEntity> findAllByAtivoTrue();
}
