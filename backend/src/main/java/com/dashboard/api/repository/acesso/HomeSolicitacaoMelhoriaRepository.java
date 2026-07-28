package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.HomeSolicitacaoMelhoriaEntity;
import java.util.List;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HomeSolicitacaoMelhoriaRepository extends JpaRepository<HomeSolicitacaoMelhoriaEntity, Long> {

    @Query("""
            select solicitacao
            from HomeSolicitacaoMelhoriaEntity solicitacao
            where solicitacao.ativo = true
            order by
                case solicitacao.status when 'ABERTA' then 0 when 'CONCLUIDA' then 1 else 2 end,
                case when solicitacao.status = 'ARQUIVADA' then solicitacao.arquivadoEm else solicitacao.criadoEm end desc,
                solicitacao.id desc
            """)
    List<HomeSolicitacaoMelhoriaEntity> listarAtivasOrdenadas();

    @Query("""
            select solicitacao
            from HomeSolicitacaoMelhoriaEntity solicitacao
            where solicitacao.ativo = true
              and solicitacao.solicitanteEmail = :solicitanteEmail
            order by
                case solicitacao.status when 'ABERTA' then 0 when 'CONCLUIDA' then 1 else 2 end,
                case when solicitacao.status = 'ARQUIVADA' then solicitacao.arquivadoEm else solicitacao.criadoEm end desc,
                solicitacao.id desc
            """)
    List<HomeSolicitacaoMelhoriaEntity> listarAtivasDoSolicitanteOrdenadas(
            @Param("solicitanteEmail") String solicitanteEmail
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update HomeSolicitacaoMelhoriaEntity solicitacao
            set solicitacao.status = 'ARQUIVADA',
                solicitacao.arquivadoEm = :arquivadoEm
            where solicitacao.ativo = true
              and solicitacao.status = 'CONCLUIDA'
              and solicitacao.concluidoEm < :limiteConclusao
            """)
    int arquivarConcluidasAntes(
            @Param("limiteConclusao") Instant limiteConclusao,
            @Param("arquivadoEm") Instant arquivadoEm
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update HomeSolicitacaoMelhoriaEntity solicitacao
            set solicitacao.ativo = false,
                solicitacao.excluidoEm = :excluidoEm
            where solicitacao.ativo = true
              and solicitacao.status = 'ARQUIVADA'
              and solicitacao.arquivadoEm < :limiteArquivamento
            """)
    int ocultarArquivadasExpiradas(
            @Param("limiteArquivamento") Instant limiteArquivamento,
            @Param("excluidoEm") Instant excluidoEm
    );
}
