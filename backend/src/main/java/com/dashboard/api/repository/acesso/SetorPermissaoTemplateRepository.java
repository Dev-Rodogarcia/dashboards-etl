package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.SetorPermissaoTemplate;
import com.dashboard.api.model.acesso.SetorPermissaoTemplateId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SetorPermissaoTemplateRepository extends JpaRepository<SetorPermissaoTemplate, SetorPermissaoTemplateId> {
    List<SetorPermissaoTemplate> findAllBySetorId(Long setorId);
    void deleteAllBySetorId(Long setorId);
}
