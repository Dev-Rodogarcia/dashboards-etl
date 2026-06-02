package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.RefreshTokenSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, Long> {
    Optional<RefreshTokenSession> findByTokenHash(String tokenHash);
    List<RefreshTokenSession> findAllByUsuarioIdAndRevogadoEmIsNull(Long usuarioId);
    long deleteByExpiraEmBefore(Instant expiraEm);
}
