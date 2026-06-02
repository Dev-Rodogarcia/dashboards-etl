package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.UsuarioPermissaoOverride;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioPermissaoOverrideRepository extends JpaRepository<UsuarioPermissaoOverride, Long> {
    List<UsuarioPermissaoOverride> findAllByUsuarioId(Long usuarioId);
    void deleteAllByUsuarioId(Long usuarioId);

    @Query("""
            SELECT o.usuario.id AS usuarioId,
                   p.id AS permissaoId,
                   COALESCE(p.chaveLegado, p.chave) AS chave,
                   o.tipo AS tipo
            FROM UsuarioPermissaoOverride o
            JOIN o.permissao p
            WHERE o.usuario.id IN :usuarioIds
            ORDER BY o.usuario.id ASC, COALESCE(p.chaveLegado, p.chave) ASC
            """)
    List<UsuarioPermissaoOverrideProjection> findOverridesPorUsuarios(@Param("usuarioIds") Collection<Long> usuarioIds);

    interface UsuarioPermissaoOverrideProjection {
        Long getUsuarioId();
        Long getPermissaoId();
        String getChave();
        String getTipo();
    }
}
