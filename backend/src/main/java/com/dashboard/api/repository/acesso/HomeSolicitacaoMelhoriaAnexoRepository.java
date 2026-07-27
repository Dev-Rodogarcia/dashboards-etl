package com.dashboard.api.repository.acesso;

import com.dashboard.api.dto.home.HomeSolicitacaoMelhoriaAnexoDTO;
import com.dashboard.api.model.acesso.HomeSolicitacaoMelhoriaAnexoEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HomeSolicitacaoMelhoriaAnexoRepository extends JpaRepository<HomeSolicitacaoMelhoriaAnexoEntity, Long> {

    @Query("""
            select new com.dashboard.api.dto.home.HomeSolicitacaoMelhoriaAnexoDTO(
                anexo.id, anexo.nomeOriginal, anexo.tipoConteudo, anexo.tamanhoBytes
            )
            from HomeSolicitacaoMelhoriaAnexoEntity anexo
            where anexo.solicitacao.id = :solicitacaoId
              and anexo.ativo = true
            order by anexo.id asc
            """)
    List<HomeSolicitacaoMelhoriaAnexoDTO> listarMetadadosAtivos(@Param("solicitacaoId") Long solicitacaoId);

    Optional<HomeSolicitacaoMelhoriaAnexoEntity> findByIdAndSolicitacaoIdAndAtivoTrue(Long id, Long solicitacaoId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update HomeSolicitacaoMelhoriaAnexoEntity anexo
            set anexo.ativo = false,
                anexo.conteudo = null,
                anexo.removidoEm = :removidoEm
            where anexo.solicitacao.id = :solicitacaoId
              and anexo.ativo = true
            """)
    int removerConteudosDaSolicitacao(
            @Param("solicitacaoId") Long solicitacaoId,
            @Param("removidoEm") Instant removidoEm
    );
}
