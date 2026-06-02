package com.dashboard.api.repository.acesso;

import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.policy.EscopoFiliaisUsuarioPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EscopoFiliaisUsuarioStore {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public EscopoFiliaisUsuarioStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = jdbcTemplate == null ? null : new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public void carregarNoUsuario(UsuarioEntity usuario) {
        EscopoUsuario escopo = carregar(usuario);
        usuario.setEscopoFiliaisTipo(escopo.tipo());
        usuario.setFiliaisPermitidasUsuario(new LinkedHashSet<>(escopo.filiais()));
    }

    public EscopoUsuario carregar(UsuarioEntity usuario) {
        if (usuario == null || usuario.getId() == null || !estruturaDisponivel()) {
            return EscopoUsuario.herdarSetor();
        }

        try {
            String tipo = jdbcTemplate.queryForObject(
                    "SELECT escopo_filiais_tipo FROM acesso.usuarios WHERE id = ?",
                    String.class,
                    usuario.getId()
            );
            String tipoNormalizado = EscopoFiliaisUsuarioPolicy.normalizarTipo(tipo);
            List<String> filiais = EscopoFiliaisUsuarioPolicy.SELECIONADAS.equals(tipoNormalizado)
                    ? jdbcTemplate.queryForList(
                            """
                            SELECT filial_nome
                            FROM acesso.usuario_filiais_permitidas
                            WHERE usuario_id = ?
                            ORDER BY filial_nome
                            """,
                            String.class,
                            usuario.getId()
                    )
                    : List.of();
            return new EscopoUsuario(tipoNormalizado, filiais);
        } catch (DataAccessException ex) {
            return EscopoUsuario.herdarSetor();
        }
    }

    public Map<Long, EscopoUsuario> carregarPorUsuarios(Collection<Long> usuarioIds) {
        List<Long> ids = usuarioIds == null ? List.of() : usuarioIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, EscopoUsuario> resultado = new LinkedHashMap<>();
        ids.forEach(id -> resultado.put(id, EscopoUsuario.herdarSetor()));
        if (ids.isEmpty() || !estruturaDisponivel() || namedParameterJdbcTemplate == null) {
            return resultado;
        }

        try {
            namedParameterJdbcTemplate.query(
                    """
                    SELECT id, escopo_filiais_tipo
                    FROM acesso.usuarios
                    WHERE id IN (:ids)
                    """,
                    Map.of("ids", ids),
                    (RowCallbackHandler) rs -> resultado.put(
                            rs.getLong("id"),
                            new EscopoUsuario(
                                    EscopoFiliaisUsuarioPolicy.normalizarTipo(rs.getString("escopo_filiais_tipo")),
                                    List.of()
                            )
                    )
            );

            Map<Long, List<String>> filiaisPorUsuario = new LinkedHashMap<>();
            namedParameterJdbcTemplate.query(
                    """
                    SELECT usuario_id, filial_nome
                    FROM acesso.usuario_filiais_permitidas
                    WHERE usuario_id IN (:ids)
                    ORDER BY usuario_id, filial_nome
                    """,
                    Map.of("ids", ids),
                    (RowCallbackHandler) rs -> filiaisPorUsuario
                            .computeIfAbsent(rs.getLong("usuario_id"), ignored -> new ArrayList<>())
                            .add(rs.getString("filial_nome"))
            );

            filiaisPorUsuario.forEach((usuarioId, filiais) -> {
                EscopoUsuario escopo = resultado.getOrDefault(usuarioId, EscopoUsuario.herdarSetor());
                if (EscopoFiliaisUsuarioPolicy.SELECIONADAS.equals(escopo.tipo())) {
                    resultado.put(usuarioId, new EscopoUsuario(escopo.tipo(), filiais));
                }
            });
            return resultado;
        } catch (DataAccessException ex) {
            return resultado;
        }
    }

    public void salvar(UsuarioEntity usuario) {
        Long usuarioId = Objects.requireNonNull(usuario.getId(), "usuario.id é obrigatório.");
        String tipo = EscopoFiliaisUsuarioPolicy.normalizarTipo(usuario.getEscopoFiliaisTipo());
        LinkedHashSet<String> filiais = EscopoFiliaisUsuarioPolicy.normalizarFiliaisSelecionadas(
                tipo,
                usuario.getFiliaisPermitidasUsuario()
        );

        if (!estruturaDisponivel()) {
            if (EscopoFiliaisUsuarioPolicy.HERDAR_SETOR.equals(tipo) && filiais.isEmpty()) {
                return;
            }
            throw new IllegalStateException("A estrutura de escopo de filiais por usuário não está aplicada no banco. Execute a migration V011.");
        }

        jdbcTemplate.update(
                "UPDATE acesso.usuarios SET escopo_filiais_tipo = ? WHERE id = ?",
                tipo,
                usuarioId
        );
        jdbcTemplate.update("DELETE FROM acesso.usuario_filiais_permitidas WHERE usuario_id = ?", usuarioId);

        for (String filial : filiais) {
            jdbcTemplate.update(
                    "INSERT INTO acesso.usuario_filiais_permitidas (usuario_id, filial_nome) VALUES (?, ?)",
                    usuarioId,
                    filial
            );
        }
    }

    public boolean estruturaDisponivel() {
        if (jdbcTemplate == null) {
            return false;
        }
        try {
            return colunaExiste("acesso.usuarios", "escopo_filiais_tipo")
                    && tabelaExiste("acesso.usuario_filiais_permitidas");
        } catch (DataAccessException ex) {
            return false;
        }
    }

    private boolean colunaExiste(String nomeCompletoTabela, String nomeColuna) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) WHERE COL_LENGTH(?, ?) IS NOT NULL",
                Integer.class,
                nomeCompletoTabela,
                nomeColuna
        );
        return total != null && total > 0;
    }

    private boolean tabelaExiste(String nomeCompletoTabela) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) WHERE OBJECT_ID(?, 'U') IS NOT NULL",
                Integer.class,
                nomeCompletoTabela
        );
        return total != null && total > 0;
    }

    public record EscopoUsuario(String tipo, List<String> filiais) {
        public static EscopoUsuario herdarSetor() {
            return new EscopoUsuario(EscopoFiliaisUsuarioPolicy.HERDAR_SETOR, List.of());
        }
    }
}
