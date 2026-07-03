package com.dashboard.api.contract;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.ResultSetExtractor;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColetasViewContractValidatorTest {

    @Mock
    private JdbcOperations jdbcTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void validarSolicitacaoNativaAceitaTipoDate() {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class))).thenReturn("date", "nvarchar(100)");

        ColetasViewContractValidator validator = new ColetasViewContractValidator(jdbcTemplate);

        assertThatCode(validator::validarSolicitacaoNativa).doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings("unchecked")
    void validarSolicitacaoNativaRejeitaTexto() {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class))).thenReturn("nvarchar");

        ColetasViewContractValidator validator = new ColetasViewContractValidator(jdbcTemplate);

        assertThatThrownBy(validator::validarSolicitacaoNativa)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("vw_coletas_powerbi.[Solicitacao]")
                .hasMessageContaining("tipo de data nativo")
                .hasMessageContaining("nvarchar");
    }
}
