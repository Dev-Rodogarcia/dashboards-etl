package com.dashboard.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProdDbContractValidationScriptTest {

    @Test
    void preflightProducaoDeveAceitarViewsESynonymsNoContratoDashboard() throws IOException {
        String script = Files.readString(
                Path.of("..", "scripts", "validate-prod-db-contract.ps1"),
                StandardCharsets.UTF_8
        );

        assertThat(script).contains("o.type IN (N'V', N'SN')");
        assertThat(script).contains("FROM sys.dm_exec_describe_first_result_set(");
        assertThat(script).contains("N'SELECT TOP (0) * FROM dbo.vw_localizacao_cargas_powerbi'");
        assertThat(script).contains("ProdDbContractValidator.java");
        assertThat(script).doesNotContain("SQLCMD.EXE");
        assertThat(script).doesNotContain("OBJECT_ID(N'dbo.vw_localizacao_cargas_powerbi', N'V')");
        assertThat(script).doesNotContain("FROM sys.columns");
    }
}
