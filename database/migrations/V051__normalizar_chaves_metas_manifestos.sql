IF OBJECT_ID(N'acesso.manifestos_cost_goals', N'U') IS NOT NULL
BEGIN
    UPDATE acesso.manifestos_cost_goals
    SET
        branch_id = CASE
            WHEN branch_id IS NULL
              OR LTRIM(RTRIM(branch_id)) = N''
              OR UPPER(LTRIM(RTRIM(branch_id))) = N'GLOBAL'
            THEN NULL
            ELSE UPPER(LTRIM(RTRIM(branch_id)))
        END,
        contract_type = CASE
            WHEN contract_type IS NULL
              OR LTRIM(RTRIM(contract_type)) = N''
            THEN N'GERAL'
            ELSE UPPER(LTRIM(RTRIM(contract_type)))
        END,
        contract_type_key = CASE
            WHEN contract_type_key IS NULL
              OR LTRIM(RTRIM(contract_type_key)) = N''
            THEN N'GERAL'
            ELSE UPPER(LTRIM(RTRIM(contract_type_key)))
        END,
        classification_key = CASE
            WHEN classification_key IS NULL
              OR LTRIM(RTRIM(classification_key)) = ''
              OR UPPER(LTRIM(RTRIM(classification_key))) IN ('GERAL', 'GLOBAL')
            THEN NULL
            ELSE UPPER(LTRIM(RTRIM(classification_key)))
        END;
END
