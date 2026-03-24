package com.dashboard.api.dto.contaspagar;

import java.math.BigDecimal;

public record ContasAPagarFornecedorDTO(
        String fornecedor,
        BigDecimal valor,
        int titulos
) {
}
