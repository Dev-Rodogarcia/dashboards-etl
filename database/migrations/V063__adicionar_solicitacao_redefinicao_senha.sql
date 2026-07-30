IF COL_LENGTH(N'acesso.usuarios', N'password_reset_requested_at') IS NULL
BEGIN
    ALTER TABLE acesso.usuarios
        ADD password_reset_requested_at DATETIME2 NULL;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'acesso.usuarios', N'U')
      AND name = N'IX_usuarios_password_reset_requested_at'
)
BEGIN
    CREATE INDEX IX_usuarios_password_reset_requested_at
        ON acesso.usuarios (password_reset_requested_at)
        WHERE password_reset_requested_at IS NOT NULL;
END;
