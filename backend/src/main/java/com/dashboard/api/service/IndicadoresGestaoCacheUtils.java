package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

final class IndicadoresGestaoCacheUtils {

    private IndicadoresGestaoCacheUtils() {
    }

    static CacheKey chave(FiltroConsultaDTO filtro, EscopoFilialService.EscopoFilial escopo) {
        return new CacheKey(
                filtro.dataInicio(),
                filtro.dataFim(),
                normalizar(filtro.valores("filiais")),
                escopo.acessoTotal(),
                normalizar(escopo.filiaisOrdenadas())
        );
    }

    static boolean contextoWebAtivo() {
        return RequestContextHolder.getRequestAttributes() != null;
    }

    private static List<String> normalizar(Collection<String> valores) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(valor -> valor.trim().toLowerCase(Locale.ROOT))
                .sorted()
                .distinct()
                .toList();
    }

    record CacheKey(
            LocalDate dataInicio,
            LocalDate dataFim,
            List<String> filiaisFiltro,
            boolean acessoTotal,
            List<String> filiaisEscopo
    ) {
    }

    record CacheEntry<T>(CompletableFuture<T> future, Instant expiraEm) {
        boolean validaEm(Instant instante) {
            return expiraEm.isAfter(instante);
        }
    }
}
