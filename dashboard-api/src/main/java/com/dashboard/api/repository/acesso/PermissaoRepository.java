package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.PermissaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissaoRepository extends JpaRepository<PermissaoEntity, Long> {
    Optional<PermissaoEntity> findByChave(String chave);
    Optional<PermissaoEntity> findByChaveLegado(String chaveLegado);
    List<PermissaoEntity> findAllByAtivoTrue();
}
