package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.UsuarioPapelVinculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioPapelVinculoRepository extends JpaRepository<UsuarioPapelVinculo, Long> {
    List<UsuarioPapelVinculo> findAllByUsuarioId(Long usuarioId);
    void deleteAllByUsuarioId(Long usuarioId);
}
