package com.dashboard.api.dto.faturascliente;

import java.math.BigDecimal;

public record FaturasPorClienteSerieDTO(String periodo, BigDecimal valor, int registros) {
}
