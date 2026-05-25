package com.dashboard.api.service;

import com.dashboard.api.service.acesso.EscopoFilialService;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

final class DashboardQueryFilters {

    private static final String SENTINELA_SEM_FILTRO = "__sem_filtro__";

    private DashboardQueryFilters() {
    }

    static ParametroLista of(Collection<String> valores) {
        List<String> normalizados = normalizar(valores);
        return normalizados.isEmpty()
                ? new ParametroLista(List.of(SENTINELA_SEM_FILTRO), 1)
                : new ParametroLista(normalizados, 0);
    }

    static ParametroLista escopo(EscopoFilialService.EscopoFilial escopo) {
        return escopo.acessoTotal() ? of(List.of()) : of(escopo.filiaisOrdenadas());
    }

    private static List<String> normalizar(Collection<String> valores) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(valor -> valor.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    record ParametroLista(List<String> valores, int vazio) {
    }
}
