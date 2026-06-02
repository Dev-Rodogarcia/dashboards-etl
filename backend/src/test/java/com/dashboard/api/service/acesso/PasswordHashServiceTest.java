package com.dashboard.api.service.acesso;

import com.dashboard.api.model.acesso.StatusSenhaUsuario;
import com.dashboard.api.model.acesso.UsuarioEntity;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;

class PasswordHashServiceTest {

    private final PasswordHashService service = new PasswordHashService(
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
            new BCryptPasswordEncoder()
    );

    @Test
    void deveGerarHashArgon2idParaNovasSenhas() {
        PasswordHashService.PasswordHash hash = service.gerarHashSeguro("Senha@123456");

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setSenhaHash(hash.valor());
        usuario.setAlgoritmoHash(hash.algoritmo());

        PasswordHashService.PasswordVerification verificacao = service.verificarSenha(usuario, "Senha@123456");

        assertThat(hash.algoritmo()).isEqualTo(PasswordHashService.ALGORITMO_ARGON2ID);
        assertThat(verificacao.valida()).isTrue();
        assertThat(verificacao.precisaUpgrade()).isFalse();
        assertThat(service.statusAdministrativo(usuario)).isEqualTo(StatusSenhaUsuario.SEGURA);
    }

    @Test
    void deveAceitarBcryptExistenteEMarcarUpgradeNoLogin() {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setSenhaHash(bcrypt.encode("Senha@123456"));
        usuario.setAlgoritmoHash(PasswordHashService.ALGORITMO_BCRYPT);

        PasswordHashService.PasswordVerification verificacao = service.verificarSenha(usuario, "Senha@123456");

        assertThat(verificacao.valida()).isTrue();
        assertThat(verificacao.precisaUpgrade()).isTrue();
        assertThat(service.statusAdministrativo(usuario)).isEqualTo(StatusSenhaUsuario.MIGRAR_NO_LOGIN);
    }

    @Test
    void deveBloquearHashLegadoEExigirResetObrigatorio() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setSenhaHash("5e884898da28047151d0e56f8dc62927");
        usuario.setAlgoritmoHash(PasswordHashService.ALGORITMO_MD5);

        PasswordHashService.PasswordVerification verificacao = service.verificarSenha(usuario, "password");

        assertThat(verificacao.resetObrigatorio()).isTrue();
        assertThat(verificacao.valida()).isFalse();
        assertThat(service.statusAdministrativo(usuario)).isEqualTo(StatusSenhaUsuario.RESET_OBRIGATORIO);
    }
}
