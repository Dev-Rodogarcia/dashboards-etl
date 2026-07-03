package com.dashboard.api.dto.indicadoresgestao;

public enum NivelVisaoPerformance {
    RESPONSAVEL,
    REGIAO,
    CIDADE;

    public boolean exigeResponsavelFiltro() {
        return this == REGIAO || this == CIDADE;
    }

    public boolean exigeRegiaoFiltro() {
        return this == CIDADE;
    }
}
