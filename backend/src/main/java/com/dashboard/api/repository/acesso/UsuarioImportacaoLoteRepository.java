package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.UsuarioImportacaoLoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface UsuarioImportacaoLoteRepository extends JpaRepository<UsuarioImportacaoLoteEntity, Long> {
    Optional<UsuarioImportacaoLoteEntity> findByTokenImportacao(String tokenImportacao);
    void deleteByExpiraEmBefore(Instant instant);
}
