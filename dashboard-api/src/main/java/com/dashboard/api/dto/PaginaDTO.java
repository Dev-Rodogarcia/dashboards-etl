package com.dashboard.api.dto;

import java.util.List;

public record PaginaDTO<T>(
        List<T> conteudo,
        long totalElementos,
        int totalPaginas,
        int paginaAtual,
        int tamanhoPagina
) {
}
