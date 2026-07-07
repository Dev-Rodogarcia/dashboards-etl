package com.dashboard.api.repository;

import com.dashboard.api.dto.indicadoresgestao.ClienteExcecaoCubagemDTO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ClienteExcecaoCubagemSqlRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ClienteExcecaoCubagemSqlRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void merge(List<ClienteExcecaoCubagemRegistro> registros, String atualizadoPor) {
        if (registros == null || registros.isEmpty()) {
            return;
        }

        String sql = """
                MERGE dbo.cliente_excecao_cubagem AS target
                USING (
                    SELECT
                        :clienteCnpj AS cliente_cnpj,
                        :razaoSocial AS razao_social,
                        :nomeFantasia AS nome_fantasia,
                        :cidadeUf AS cidade_uf,
                        :atualizadoPor AS atualizado_por
                ) AS source
                   ON target.cliente_cnpj = source.cliente_cnpj
                WHEN MATCHED THEN
                    UPDATE SET
                        razao_social = source.razao_social,
                        nome_fantasia = source.nome_fantasia,
                        cidade_uf = source.cidade_uf,
                        atualizado_por = source.atualizado_por,
                        data_atualizacao = SYSDATETIMEOFFSET(),
                        ativo = 1
                WHEN NOT MATCHED THEN
                    INSERT (cliente_cnpj, razao_social, nome_fantasia, cidade_uf, atualizado_por, ativo)
                    VALUES (source.cliente_cnpj, source.razao_social, source.nome_fantasia, source.cidade_uf, source.atualizado_por, 1);
                """;

        MapSqlParameterSource[] params = registros.stream()
                .map(registro -> new MapSqlParameterSource()
                        .addValue("clienteCnpj", registro.clienteCnpj())
                        .addValue("razaoSocial", registro.razaoSocial())
                        .addValue("nomeFantasia", registro.nomeFantasia())
                        .addValue("cidadeUf", registro.cidadeUf())
                        .addValue("atualizadoPor", atualizadoPor))
                .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, params);
    }

    public List<ClienteExcecaoCubagemDTO> listarAtivos() {
        String sql = """
                SELECT
                    cliente_cnpj,
                    razao_social,
                    nome_fantasia,
                    cidade_uf,
                    atualizado_por,
                    data_atualizacao
                FROM dbo.cliente_excecao_cubagem
                WHERE ativo = 1
                ORDER BY
                    COALESCE(NULLIF(LTRIM(RTRIM(razao_social)), N''), cliente_cnpj),
                    cliente_cnpj
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource(), this::mapearCliente);
    }

    public int inativarPorCnpj(String clienteCnpj, String atualizadoPor) {
        String sql = """
                UPDATE dbo.cliente_excecao_cubagem
                   SET ativo = 0,
                       atualizado_por = :atualizadoPor,
                       data_atualizacao = SYSDATETIMEOFFSET()
                 WHERE cliente_cnpj = :clienteCnpj
                   AND ativo = 1
                """;

        return jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("clienteCnpj", clienteCnpj)
                .addValue("atualizadoPor", atualizadoPor));
    }

    private ClienteExcecaoCubagemDTO mapearCliente(ResultSet rs, int rowNum) throws SQLException {
        return new ClienteExcecaoCubagemDTO(
                rs.getString("cliente_cnpj"),
                rs.getString("razao_social"),
                rs.getString("nome_fantasia"),
                rs.getString("cidade_uf"),
                rs.getString("atualizado_por"),
                rs.getObject("data_atualizacao", OffsetDateTime.class)
        );
    }

    public record ClienteExcecaoCubagemRegistro(
            String clienteCnpj,
            String razaoSocial,
            String nomeFantasia,
            String cidadeUf
    ) {
    }
}
