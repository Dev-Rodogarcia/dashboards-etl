package com.dashboard.api.service.acesso;

import com.dashboard.api.security.acesso.UsuarioSupremo;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioSupremoTest {

    @Test
    void configuracaoCompletaDeveSerAceita() {
        UsuarioSupremo usuarioSupremo = new UsuarioSupremo(
                "supremo@empresa.com",
                "Senha@123456",
                "Supremo",
                "desenvolvedor",
                1000,
                false
        );

        assertThatCode(usuarioSupremo::validarConfiguracao).doesNotThrowAnyException();
    }

    @Test
    void configuracaoSemSenhaInicialDeveFalharNoStartup() {
        UsuarioSupremo usuarioSupremo = new UsuarioSupremo(
                "supremo@empresa.com",
                "",
                "Supremo",
                "desenvolvedor",
                1000,
                false
        );

        assertThatThrownBy(usuarioSupremo::validarConfiguracao)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACESSO_USUARIO_SUPREMO_SENHA_INICIAL");
    }
}
