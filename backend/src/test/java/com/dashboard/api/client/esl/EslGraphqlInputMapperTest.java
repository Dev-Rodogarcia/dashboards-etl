package com.dashboard.api.client.esl;

import com.dashboard.api.dto.esl.EslColetaListagemRequestDTO;
import com.dashboard.api.dto.esl.EslContextoOperacionalDTO;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EslGraphqlInputMapperTest {

    @Test
    void deveEnviarDataDaListagemDeColetasComoTextoIso8601() {
        Map<String, Object> variaveis = new EslGraphqlInputMapper().paraPickList(
                new EslColetaListagemRequestDTO(LocalDate.of(2026, 7, 16), null, 100),
                new EslContextoOperacionalDTO("<CNPJ>", Long.valueOf(123), "Operador", "operador@rodogarcia.com.br", null, null)
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) variaveis.get("params");
        assertThat(params.get("requestDate"))
                .isInstanceOf(String.class)
                .isEqualTo("2026-07-16");
        assertThat(params).containsEntry("corporationId", 123L)
                .doesNotContainKey("corporation");
    }
}
