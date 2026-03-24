package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

final class ConsultaSpecificationUtils {

    private ConsultaSpecificationUtils() {
    }

    @SafeVarargs
    @NonNull
    static <T> Specification<T> allOf(Specification<T>... specifications) {
        Specification<T> acumulada = sempreVerdadeiro();
        for (Specification<T> specification : specifications) {
            if (specification != null) {
                acumulada = acumulada.and(specification);
            }
        }
        return acumulada;
    }

    @NonNull
    static <T> Specification<T> sempreVerdadeiro() {
        return (root, query, cb) -> cb.conjunction();
    }

    @NonNull
    static <T> Specification<T> sempreFalso() {
        return (root, query, cb) -> cb.disjunction();
    }

    @NonNull
    static <T, Y extends Comparable<? super Y>> Specification<T> between(String campo, Y inicio, Y fim) {
        return (root, query, cb) -> cb.between(root.get(campo), inicio, fim);
    }

    @NonNull
    static <T> Specification<T> filtroTexto(FiltroConsultaDTO filtro, String chaveFiltro, String campo) {
        return valoresIgnoreCase(campo, filtro.valores(chaveFiltro));
    }

    @NonNull
    static <T> Specification<T> filtroTextoQualquerCampo(FiltroConsultaDTO filtro, String chaveFiltro, String... campos) {
        return valoresIgnoreCaseQualquerCampo(filtro.valores(chaveFiltro), campos);
    }

    @NonNull
    static <T> Specification<T> escopoFiliais(EscopoFilialService.EscopoFilial escopo, String... campos) {
        if (escopo.acessoTotal()) {
            return sempreVerdadeiro();
        }
        List<String> filiais = escopo.filiaisOrdenadas();
        if (filiais.isEmpty()) {
            return sempreFalso();
        }
        return valoresIgnoreCaseQualquerCampo(filiais, campos);
    }

    @NonNull
    static <T> Specification<T> valoresIgnoreCase(String campo, Collection<String> valores) {
        List<String> normalizados = normalizar(valores);
        if (normalizados.isEmpty()) {
            return sempreVerdadeiro();
        }

        return (root, query, cb) -> cb.lower(root.get(campo)).in(normalizados);
    }

    @NonNull
    static <T> Specification<T> valoresIgnoreCaseQualquerCampo(Collection<String> valores, String... campos) {
        List<String> normalizados = normalizar(valores);
        if (normalizados.isEmpty()) {
            return sempreVerdadeiro();
        }
        if (campos == null || campos.length == 0) {
            return sempreVerdadeiro();
        }

        return (root, query, cb) -> cb.or(Arrays.stream(campos)
                .map(campo -> cb.lower(root.get(campo)).in(normalizados))
                .toArray(jakarta.persistence.criteria.Predicate[]::new));
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
}
