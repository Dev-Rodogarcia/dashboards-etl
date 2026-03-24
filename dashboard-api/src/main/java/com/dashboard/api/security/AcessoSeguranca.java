package com.dashboard.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("acessoSeguranca")
public class AcessoSeguranca {

    public boolean ehAdmin() {
        return possuiAuthority("ROLE_ADMIN");
    }

    public boolean possuiPapel(String nomePapel) {
        return possuiAuthority("ROLE_" + nomePapel.toUpperCase());
    }

    public boolean podeAcessar(String permissao) {
        return ehAdmin() || possuiAuthority(PermissaoCatalogo.authorityForKey(permissao));
    }

    public boolean podeAcessarDimensoes() {
        if (podeAcessar("dimensoes")) {
            return true;
        }
        return PermissaoCatalogo.dashboardKeys().stream().anyMatch(this::podeAcessar);
    }

    public boolean podeAcessarDimensaoFiliais() {
        return podeAcessarDimensoes();
    }

    public boolean podeAcessarDimensaoClientes() {
        return podeAcessar("coletas")
                || podeAcessar("fretes")
                || podeAcessar("faturas")
                || podeAcessar("faturasPorCliente")
                || podeAcessar("cotacoes");
    }

    public boolean podeAcessarDimensaoMotoristas() {
        return podeAcessar("manifestos");
    }

    public boolean podeAcessarDimensaoVeiculos() {
        return podeAcessar("manifestos");
    }

    public boolean podeAcessarDimensaoPlanoContas() {
        return podeAcessar("contasAPagar");
    }

    public boolean podeAcessarDimensaoUsuarios() {
        return podeAcessar("coletas");
    }

    private boolean possuiAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }
}
