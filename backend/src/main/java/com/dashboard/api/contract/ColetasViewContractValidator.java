package com.dashboard.api.contract;

import java.util.Locale;
import java.util.Set;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;

@Component
public class ColetasViewContractValidator {

    private static final Set<String> TIPOS_DATA_NATIVOS = Set.of(
            "date",
            "datetime",
            "datetime2",
            "smalldatetime",
            "datetimeoffset"
    );

    private final JdbcOperations jdbcTemplate;
    private volatile boolean validado;

    public ColetasViewContractValidator(JdbcOperations jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void validarSolicitacaoNativa() {
        if (validado) {
            return;
        }

        String tipo = jdbcTemplate.query("""
                SELECT system_type_name
                FROM sys.dm_exec_describe_first_result_set(
                    N'SELECT TOP (0) [Solicitacao] FROM dbo.vw_coletas_powerbi',
                    NULL,
                    0
                )
                WHERE error_number IS NULL
                  AND name = N'Solicitacao'
                """, rs -> rs.next() ? rs.getString(1) : null);

        if (tipo == null || tipo.isBlank()) {
            throw new IllegalStateException("Contrato invalido: coluna vw_coletas_powerbi.[Solicitacao] nao encontrada.");
        }

        String normalizado = tipo.trim().toLowerCase(Locale.ROOT).replaceFirst("\\(.*\\)$", "");
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
