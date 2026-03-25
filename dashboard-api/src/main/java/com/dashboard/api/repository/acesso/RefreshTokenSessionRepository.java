package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.RefreshTokenSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, Long> {
    Optional<RefreshTokenSession> findByTokenHash(String tokenHash);
    List<RefreshTokenSession> findAllByUsuarioIdAndRevogadoEmIsNull(Long usuarioId);
}
