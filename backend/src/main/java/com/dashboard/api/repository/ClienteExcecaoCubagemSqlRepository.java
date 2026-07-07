package com.dashboard.api.repository;

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
                        data_atualizacao = SYSDATETIMEOFFSET()
                WHEN NOT MATCHED THEN
                    INSERT (cliente_cnpj, razao_social, nome_fantasia, cidade_uf, atualizado_por)
                    VALUES (source.cliente_cnpj, source.razao_social, source.nome_fantasia, source.cidade_uf, source.atualizado_por);
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

    public record ClienteExcecaoCubagemRegistro(
            String clienteCnpj,
            String razaoSocial,
            String nomeFantasia,
            String cidadeUf
    ) {
    }
}
