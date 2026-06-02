package com.dashboard.api.util;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Locale;
import org.springframework.web.context.request.RequestContextHolder;

public final class IndicadoresGestaoCacheUtils {

    private IndicadoresGestaoCacheUtils() {
    }

    public static CacheKey chave(FiltroConsultaDTO filtro, EscopoFilialService.EscopoFilial escopo) {
        return new CacheKey(
                filtro.dataInicio(),
                filtro.dataFim(),
                normalizar(filtro.valores("filiais")),
                escopo.acessoTotal(),
                normalizar(escopo.filiaisOrdenadas())
        );
    }

    public static boolean contextoWebAtivo() {
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

    public record CacheKey(
            LocalDate dataInicio,
            LocalDate dataFim,
            List<String> filiaisFiltro,
            boolean acessoTotal,
            List<String> filiaisEscopo
    ) {
    }

    public record CacheEntry<T>(CompletableFuture<T> future, Instant expiraEm) {
        public boolean validaEm(Instant instante) {
            return expiraEm.isAfter(instante);
        }
    }
}
