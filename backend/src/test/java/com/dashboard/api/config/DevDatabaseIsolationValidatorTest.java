package com.dashboard.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevDatabaseIsolationValidatorTest {

    @Test
    void devePermitirBancoDevSeparado() {
        DevDatabaseIsolationValidator validator = validator(
                "dev",
                "",
                "jdbc:sqlserver://localhost:1433;databaseName=DASHBOARDS_DEV;encrypt=true"
        );

        assertThatCode(validator::validarBancoDeDesenvolvimento).doesNotThrowAnyException();
    }

    @Test
    void deveBloquearBancoProducaoNoProfileDev() {
        DevDatabaseIsolationValidator validator = validator(
                "dev",
                "",
                "jdbc:sqlserver://localhost:1433;databaseName=DASHBOARDS;encrypt=true"
        );

        assertThatThrownBy(validator::validarBancoDeDesenvolvimento)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEV nao pode usar o banco DASHBOARDS");
    }

    @Test
    void deveExigirDatabaseNameExplicitoNoProfileDev() {
        DevDatabaseIsolationValidator validator = validator(
                "dev",
                "",
                "jdbc:sqlserver://localhost:1433;encrypt=true"
        );

        assertThatThrownBy(validator::validarBancoDeDesenvolvimento)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("databaseName explicito");
    }

    @Test
    void deveIgnorarValidacaoForaDeDev() {
        DevDatabaseIsolationValidator validator = validator(
                "prod",
                "",
                "jdbc:sqlserver://localhost:1433;databaseName=DASHBOARDS;encrypt=true"
        );

        assertThatCode(validator::validarBancoDeDesenvolvimento).doesNotThrowAnyException();
    }

    @Test
    void deveExtrairDatabaseAlternativo() {
        assertThat(DevDatabaseIsolationValidator.extrairNomeBanco("jdbc:sqlserver://x;database=DASHBOARDS_DEV"))
                .contains("DASHBOARDS_DEV");
    }

    private static DevDatabaseIsolationValidator validator(String profiles, String environment, String datasourceUrl) {
        DevDatabaseIsolationValidator validator = new DevDatabaseIsolationValidator();
        ReflectionTestUtils.setField(validator, "springProfilesActive", profiles);
        ReflectionTestUtils.setField(validator, "appEnvironment", environment);
        ReflectionTestUtils.setField(validator, "datasourceUrl", datasourceUrl);
        return validator;
    }
}
