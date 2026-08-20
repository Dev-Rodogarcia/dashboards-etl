package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.ApresentacaoSequenciaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApresentacaoSequenciaRepository extends JpaRepository<ApresentacaoSequenciaEntity, Long> {
    @EntityGraph(attributePaths = "itens") List<ApresentacaoSequenciaEntity> findByUsuarioIdAndAtivoTrueOrderByAtualizadoEmDesc(Long usuarioId);
    @EntityGraph(attributePaths = "itens") Optional<ApresentacaoSequenciaEntity> findByIdAndUsuarioIdAndAtivoTrue(Long id, Long usuarioId);
}
