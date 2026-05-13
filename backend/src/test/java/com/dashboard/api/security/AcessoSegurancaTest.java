package com.dashboard.api.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AcessoSegurancaTest {

    private final AcessoSeguranca acessoSeguranca = new AcessoSeguranca();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void podeGerenciarKpiGoalsDeveExigirAuthorityOuPapelElevado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        assertThat(acessoSeguranca.podeGerenciarKpiGoals()).isFalse();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user",
                        null,
                        List.of(new SimpleGrantedAuthority(PermissaoCatalogo.authorityForKey("can_manage_kpi_goals")))
                )
        );

        assertThat(acessoSeguranca.podeGerenciarKpiGoals()).isTrue();
    }

    @Test
    void podeGerenciarHomeComunicadosDeveAceitarAliasNovo() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user",
                        null,
                        List.of(new SimpleGrantedAuthority(PermissaoCatalogo.authorityForKey("can_manage_communications")))
                )
        );

        assertThat(acessoSeguranca.podeGerenciarHomeComunicados()).isTrue();
    }
}
