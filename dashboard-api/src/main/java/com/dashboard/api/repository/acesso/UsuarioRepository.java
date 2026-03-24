package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByLogin(String login);
    Optional<UsuarioEntity> findByEmail(String email);

    @Query("SELECT u FROM UsuarioEntity u WHERE LOWER(u.login) = LOWER(:loginOuEmail) OR LOWER(u.email) = LOWER(:loginOuEmail)")
    Optional<UsuarioEntity> findByLoginOrEmail(String loginOuEmail);

    boolean existsByLoginIgnoreCase(String login);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByLoginIgnoreCaseAndIdNot(String login, Long id);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countBySetorId(Long setorId);

    @Query("SELECT COUNT(u) FROM UsuarioEntity u JOIN UsuarioPapelVinculo v ON v.usuario = u JOIN PapelEntity p ON v.papel = p WHERE p.nome = 'admin_plataforma' AND u.ativo = true")
    long countAdminsAtivos();
}
