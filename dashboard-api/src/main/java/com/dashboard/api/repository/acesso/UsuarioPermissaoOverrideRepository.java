package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.UsuarioPermissaoOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioPermissaoOverrideRepository extends JpaRepository<UsuarioPermissaoOverride, Long> {
    List<UsuarioPermissaoOverride> findAllByUsuarioId(Long usuarioId);
    void deleteAllByUsuarioId(Long usuarioId);
}
