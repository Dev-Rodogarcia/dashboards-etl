IF DB_NAME() LIKE N'%DEV%'
BEGIN
    IF SCHEMA_ID(N'acesso') IS NOT NULL
       AND OBJECT_ID(N'acesso.kpi_goals', N'U') IS NOT NULL
    BEGIN
        DECLARE @kpi_goals_seed TABLE (
            indicator_key VARCHAR(60) NOT NULL PRIMARY KEY,
            goal_value DECIMAL(9, 3) NOT NULL
        )

        INSERT INTO @kpi_goals_seed (indicator_key, goal_value)
        VALUES
            ('delivery_performance', 95.000),
            ('collector_usage', 80.000),
            ('cargo_cubage', 80.000),
            ('cargo_indemnity', 0.200),
            ('cutoff_time', 90.000)

        INSERT INTO acesso.kpi_goals (
            branch_id,
            indicator_key,
            goal_value,
            created_at,
            updated_at,
            updated_by_user_id
        )
        SELECT
            NULL,
            seed.indicator_key,
            seed.goal_value,
            SYSUTCDATETIME(),
            SYSUTCDATETIME(),
            NULL
        FROM @kpi_goals_seed seed
        WHERE NOT EXISTS (
            SELECT 1
            FROM acesso.kpi_goals existing
            WHERE existing.branch_id IS NULL
              AND existing.indicator_key = seed.indicator_key
        )
    END

    IF SCHEMA_ID(N'acesso') IS NOT NULL
       AND OBJECT_ID(N'acesso.fretes_goals', N'U') IS NOT NULL
    BEGIN
        DECLARE @ano_inicio SMALLINT = CONVERT(SMALLINT, YEAR(GETDATE()) - 1)
        DECLARE @ano_fim SMALLINT = CONVERT(SMALLINT, YEAR(GETDATE()) + 1)
        DECLARE @ano SMALLINT = @ano_inicio
        DECLARE @mes TINYINT
        DECLARE @meta_faturamento DECIMAL(18, 2) = 1500000.00

        WHILE @ano <= @ano_fim
        BEGIN
            SET @mes = 1

            WHILE @mes <= 12
            BEGIN
                IF NOT EXISTS (
                    SELECT 1
                    FROM acesso.fretes_goals existing
                    WHERE existing.branch_id IS NULL
                      AND existing.ano = @ano
                      AND existing.mes = @mes
                )
                BEGIN
                    INSERT INTO acesso.fretes_goals (
                        branch_id,
                        ano,
                        mes,
                        meta_faturamento,
                        meta_fretes,
                        created_at,
                        updated_at,
                        updated_by_user_id
                    )
                    VALUES (
                        NULL,
                        @ano,
                        @mes,
                        @meta_faturamento,
                        0,
                        SYSUTCDATETIME(),
                        SYSUTCDATETIME(),
                        NULL
                    )
                END

                SET @mes = CONVERT(TINYINT, @mes + 1)
            END

            SET @ano = CONVERT(SMALLINT, @ano + 1)
        END
    END
END
@@
