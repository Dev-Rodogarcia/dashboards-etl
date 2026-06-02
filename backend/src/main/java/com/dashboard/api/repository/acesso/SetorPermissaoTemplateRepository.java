package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.SetorPermissaoTemplate;
import com.dashboard.api.model.acesso.SetorPermissaoTemplateId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SetorPermissaoTemplateRepository extends JpaRepository<SetorPermissaoTemplate, SetorPermissaoTemplateId> {
    List<SetorPermissaoTemplate> findAllBySetorId(Long setorId);
    void deleteAllBySetorId(Long setorId);

    @Query("""
            SELECT t.setor.id AS setorId,
                   t.permissao.id AS permissaoId
            FROM SetorPermissaoTemplate t
            WHERE t.setor.id IN :setorIds
            ORDER BY t.setor.id ASC, t.permissao.id ASC
            """)
    List<SetorPermissaoTemplateProjection> findPermissoesPorSetores(@Param("setorIds") Collection<Long> setorIds);

    interface SetorPermissaoTemplateProjection {
        Long getSetorId();
        Long getPermissaoId();
    }
}
