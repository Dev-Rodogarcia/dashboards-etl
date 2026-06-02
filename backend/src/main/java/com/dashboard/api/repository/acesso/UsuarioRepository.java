package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.UsuarioEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByEmailIgnoreCase(String email);

    boolean existsByLoginIgnoreCase(String login);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByLoginIgnoreCaseAndIdNot(String login, Long id);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countBySetorId(Long setorId);

    @Query("SELECT COUNT(u) FROM UsuarioEntity u JOIN UsuarioPapelVinculo v ON v.usuario = u JOIN PapelEntity p ON v.papel = p WHERE p.nome IN :papeis AND u.ativo = true")
    long countAdminsAtivosPorPapeis(@Param("papeis") Collection<String> papeis);

    @Query("""
            SELECT u.id AS id,
                   u.nome AS nome,
                   u.email AS email,
                   u.ativo AS ativo,
                   s.id AS setorId,
                   s.nome AS setorNome,
                   u.algoritmoHash AS algoritmoHash
            FROM UsuarioEntity u
            JOIN u.setor s
            ORDER BY LOWER(u.nome)
            """)
    List<UsuarioAcessoResumoProjection> findAcessoResumo();

    interface UsuarioAcessoResumoProjection {
        Long getId();
        String getNome();
        String getEmail();
        Boolean getAtivo();
        Long getSetorId();
        String getSetorNome();
        String getAlgoritmoHash();
    }
}
