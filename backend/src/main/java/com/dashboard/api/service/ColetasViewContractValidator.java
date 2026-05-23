package com.dashboard.api.service;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
class ColetasViewContractValidator {

    private static final Set<String> TIPOS_DATA_NATIVOS = Set.of(
            "date",
            "datetime",
            "datetime2",
            "smalldatetime",
            "datetimeoffset"
    );

    private final JdbcOperations jdbcTemplate;
    private volatile boolean validado;

    ColetasViewContractValidator(JdbcOperations jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void validarSolicitacaoNativa() {
        if (validado) {
            return;
        }

        String tipo = jdbcTemplate.query("""
                SELECT TYPE_NAME(c.user_type_id)
                FROM sys.columns c
                WHERE c.object_id = OBJECT_ID(N'dbo.vw_coletas_powerbi')
                  AND c.name = N'Solicitacao'
                """, rs -> rs.next() ? rs.getString(1) : null);

        if (tipo == null || tipo.isBlank()) {
            throw new IllegalStateException("Contrato invalido: coluna vw_coletas_powerbi.[Solicitacao] nao encontrada.");
        }

        String normalizado = tipo.trim().toLowerCase(Locale.ROOT);
        if (!TIPOS_DATA_NATIVOS.contains(normalizado)) {
            throw new IllegalStateException(
                    "Contrato invalido: vw_coletas_powerbi.[Solicitacao] deve ser tipo de data nativo no ETL, mas veio como "
                            + tipo + ". Corrija dbo.coletas.request_date/view no repositorio etl-extracao-dados; "
                            + "o Dashboard nao aplicara conversao dinamica em tempo de consulta."
            );
        }

        validado = true;
    }
}
