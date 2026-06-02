package com.dashboard.api.service.acesso;

import com.dashboard.api.contract.acesso.UsuarioDependenciaCleanup;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDependenciaCleanupService implements UsuarioDependenciaCleanup {

    private final JdbcTemplate jdbcTemplate;

    public UsuarioDependenciaCleanupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void limparDependencias(Long usuarioId) {
        List<ReferenciaFk> referencias = jdbcTemplate.query("""
                SELECT
                    SCHEMA_NAME(t.schema_id) AS schema_name,
                    t.name AS table_name,
                    c.name AS column_name,
                    c.is_nullable AS nullable_column,
                    fk.name AS foreign_key_name,
                    COUNT(*) OVER (PARTITION BY fk.object_id) AS column_count
                FROM sys.foreign_key_columns fkc
                INNER JOIN sys.foreign_keys fk
                    ON fk.object_id = fkc.constraint_object_id
                INNER JOIN sys.tables t
                    ON t.object_id = fkc.parent_object_id
                INNER JOIN sys.columns c
                    ON c.object_id = fkc.parent_object_id
                   AND c.column_id = fkc.parent_column_id
                WHERE fkc.referenced_object_id = OBJECT_ID(N'acesso.usuarios', N'U')
                ORDER BY c.is_nullable DESC, SCHEMA_NAME(t.schema_id), t.name, c.name
                """, (rs, rowNum) -> new ReferenciaFk(
                rs.getString("schema_name"),
                rs.getString("table_name"),
                rs.getString("column_name"),
                rs.getBoolean("nullable_column"),
                rs.getString("foreign_key_name"),
                rs.getInt("column_count")
        ));

        for (ReferenciaFk referencia : referencias) {
            if (referencia.columnCount() != 1) {
                throw new IllegalStateException(
                        "FK composta não suportada no hard delete de usuário: " + referencia.foreignKeyName()
                );
            }
            
            if (referencia.nullableColumn()) {
                String sql = Objects.requireNonNull("""
                        UPDATE %s.%s
                        SET %s = NULL
                        WHERE %s = ?
                        """.formatted(
                        quote(referencia.schemaName()),
                        quote(referencia.tableName()),
                        quote(referencia.columnName()),
                        quote(referencia.columnName())
                ));
                jdbcTemplate.update(sql, usuarioId);
            } else {
                String sql = Objects.requireNonNull("""
                        DELETE FROM %s.%s
                        WHERE %s = ?
                        """.formatted(
                        quote(referencia.schemaName()),
                        quote(referencia.tableName()),
                        quote(referencia.columnName())
                ));
                jdbcTemplate.update(sql, usuarioId);
            }
        }
    }

    private String quote(String identifier) {
        if (identifier == null || identifier.isBlank() || identifier.contains("]")) {
            throw new IllegalArgumentException("Identificador SQL inválido.");
        }
        return "[" + identifier + "]";
    }

    private record ReferenciaFk(
            String schemaName,
            String tableName,
            String columnName,
            boolean nullableColumn,
            String foreignKeyName,
            int columnCount
    ) {
    }
}
