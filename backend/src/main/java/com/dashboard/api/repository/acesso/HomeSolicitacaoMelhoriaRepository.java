package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.HomeSolicitacaoMelhoriaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HomeSolicitacaoMelhoriaRepository extends JpaRepository<HomeSolicitacaoMelhoriaEntity, Long> {

    @Query("""
            select solicitacao
            from HomeSolicitacaoMelhoriaEntity solicitacao
            where solicitacao.ativo = true
            order by
                case when solicitacao.status = 'ABERTA' then 0 else 1 end,
                solicitacao.criadoEm desc,
                solicitacao.id desc
            """)
    List<HomeSolicitacaoMelhoriaEntity> listarAtivasOrdenadas();

    @Query("""
            select solicitacao
            from HomeSolicitacaoMelhoriaEntity solicitacao
            where solicitacao.ativo = true
              and solicitacao.solicitanteEmail = :solicitanteEmail
            order by
                case when solicitacao.status = 'ABERTA' then 0 else 1 end,
                solicitacao.criadoEm desc,
                solicitacao.id desc
            """)
    List<HomeSolicitacaoMelhoriaEntity> listarAtivasDoSolicitanteOrdenadas(
            @Param("solicitanteEmail") String solicitanteEmail
    );
}
