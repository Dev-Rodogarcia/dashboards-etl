package com.dashboard.api.client.esl;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EslGraphqlOperationsTest {

    @Test
    void deveManterONomeDaOperacaoDeListagemIgualAoDeclaradoNoDocumentoGraphql() {
        assertThat(EslGraphqlOperations.PICK_LIST).isEqualTo("pickList");
        assertThat(EslGraphqlOperations.PICK_LIST_RESULT).isEqualTo("pick");
        assertThat(EslGraphqlOperations.QUERY_PICK_LIST)
                .contains("query " + EslGraphqlOperations.PICK_LIST + "(");
    }
}
