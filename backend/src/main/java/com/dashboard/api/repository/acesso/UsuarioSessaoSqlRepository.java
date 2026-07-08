package com.dashboard.api.repository.acesso;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioSessaoSqlRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UsuarioSessaoSqlRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void registrarAtividade(String usuario, String rota) {
        if (usuario == null || usuario.isBlank()) {
            return;
        }

        String sql = """
                UPDATE acesso.usuarios
                   SET ultima_atividade = SYSDATETIMEOFFSET(),
                       ultima_rota_acessada = :rota
                 WHERE email = :usuario
                    OR login = :usuario
                """;

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("usuario", usuario.trim().toLowerCase())
                .addValue("rota", rota));
    }
}
