package com.dashboard.api.dto.faturascliente;

import java.math.BigDecimal;

public record FaturaPorClienteResumoDTO(
        String idUnico,
        String documentoFatura,
        String emissao,
        String vencimento,
        String baixa,
        String filial,
        String clientePagador,
        Long numeroCte,
        BigDecimal valorFaturado,
        String statusProcesso
) {
}
