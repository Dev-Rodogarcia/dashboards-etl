package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.HomeComunicadoCurtidaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HomeComunicadoCurtidaRepository extends JpaRepository<HomeComunicadoCurtidaEntity, Long> {

    Optional<HomeComunicadoCurtidaEntity> findByComunicadoIdAndUsuarioId(Long comunicadoId, Long usuarioId);

    @Query(value = """
            SELECT c.comunicado_id AS comunicadoId,
                   CAST(COUNT_BIG(1) AS bigint) AS totalCurtidas,
                   STRING_AGG(CONVERT(nvarchar(max), u.nome), N'|') WITHIN GROUP (ORDER BY u.nome) AS curtidoPor,
                   CAST(MAX(CASE WHEN c.usuario_id = :usuarioId THEN 1 ELSE 0 END) AS bit) AS curtidoPeloUsuarioAtual
            FROM acesso.home_comunicado_curtidas c
            JOIN acesso.usuarios u ON u.id = c.usuario_id
            WHERE c.ativo = 1
              AND c.comunicado_id IN :comunicadoIds
            GROUP BY c.comunicado_id
            """, nativeQuery = true)
    List<ResumoCurtidasProjection> resumirAtivasPorComunicado(
            @Param("comunicadoIds") List<Long> comunicadoIds,
            @Param("usuarioId") Long usuarioId
    );

    interface ResumoCurtidasProjection {
        Long getComunicadoId();
        Long getTotalCurtidas();
        String getCurtidoPor();
        Boolean getCurtidoPeloUsuarioAtual();
    }
}
