package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.HomeComunicadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HomeComunicadoRepository extends JpaRepository<HomeComunicadoEntity, Long> {

    @Query("""
            select comunicado
            from HomeComunicadoEntity comunicado
            where comunicado.ativo = true
            order by
                case when comunicado.tag = 'FIXADO' then 0 else 1 end,
                comunicado.publicadoEm desc,
                comunicado.id desc
            """)
    List<HomeComunicadoEntity> listarAtivosOrdenados();
}
