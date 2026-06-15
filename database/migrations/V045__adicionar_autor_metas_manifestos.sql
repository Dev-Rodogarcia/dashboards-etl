IF COL_LENGTH(N'acesso.manifestos_cost_goals', N'updated_by_user_id') IS NULL
BEGIN
    ALTER TABLE acesso.manifestos_cost_goals
        ADD updated_by_user_id BIGINT NULL;
END
GO
