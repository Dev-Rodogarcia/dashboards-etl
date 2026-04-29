package com.dashboard.api.service;

import com.dashboard.api.dto.PaginaDTO;

import java.util.List;

final class PaginacaoListaUtils {

    private static final int PAGINA_PADRAO = 1;
    private static final int TAMANHO_PADRAO = 10;
    private static final int TAMANHO_MAXIMO = 100;

    private PaginacaoListaUtils() {
    }

    static <T> PaginaDTO<T> paginar(List<T> registros, int paginaSolicitada, int tamanhoSolicitado) {
        int pagina = Math.max(PAGINA_PADRAO, paginaSolicitada);
        int tamanho = ConsultaLimiteUtils.limitar(tamanhoSolicitado, TAMANHO_PADRAO, TAMANHO_MAXIMO);
        int total = registros.size();
        int totalPaginas = total == 0 ? 0 : (int) Math.ceil(total / (double) tamanho);
        long inicioLong = (long) (pagina - 1) * tamanho;

        if (inicioLong >= total) {
            return new PaginaDTO<>(List.of(), total, totalPaginas, pagina, tamanho);
        }

        int inicio = (int) inicioLong;
        int fim = Math.min(inicio + tamanho, total);
        return new PaginaDTO<>(registros.subList(inicio, fim), total, totalPaginas, pagina, tamanho);
    }
}
