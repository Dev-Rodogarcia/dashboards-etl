package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.HomeComunicadoComentarioEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HomeComunicadoComentarioRepository extends JpaRepository<HomeComunicadoComentarioEntity, Long> {
    @Query(value = """
            SELECT c.id AS id, c.usuario_id AS usuarioId, c.corpo AS corpo, c.criado_em AS criadoEm, u.nome AS autorNome
            FROM acesso.home_comunicado_comentarios c
            JOIN acesso.usuarios u ON u.id = c.usuario_id
            WHERE c.comunicado_id = :comunicadoId AND c.ativo = 1
            ORDER BY c.criado_em ASC, c.id ASC
            """, nativeQuery = true)
    List<ComentarioProjection> listarAtivos(@Param("comunicadoId") Long comunicadoId);

    interface ComentarioProjection {
        Long getId(); Long getUsuarioId(); String getCorpo(); java.time.Instant getCriadoEm(); String getAutorNome();
    }
}
