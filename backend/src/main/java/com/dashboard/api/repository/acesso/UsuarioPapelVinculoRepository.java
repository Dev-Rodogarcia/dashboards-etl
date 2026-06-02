package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.UsuarioPapelVinculo;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioPapelVinculoRepository extends JpaRepository<UsuarioPapelVinculo, Long> {
    List<UsuarioPapelVinculo> findAllByUsuarioId(Long usuarioId);
    void deleteAllByUsuarioId(Long usuarioId);

    @Query("""
            SELECT v.usuario.id AS usuarioId,
                   p.nome AS papelNome,
                   p.nivel AS nivel
            FROM UsuarioPapelVinculo v
            JOIN v.papel p
            WHERE v.usuario.id IN :usuarioIds
            ORDER BY v.usuario.id ASC, p.nivel DESC
            """)
    List<UsuarioPapelProjection> findPapeisPorUsuarios(@Param("usuarioIds") Collection<Long> usuarioIds);

    interface UsuarioPapelProjection {
        Long getUsuarioId();
        String getPapelNome();
        Integer getNivel();
    }
}
