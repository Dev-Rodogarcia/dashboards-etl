package com.dashboard.api.dto.esl;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Recibo efêmero de validação. O invoiceId pertence ao ESL e não é persistido
 * pelo Dashboard.
 */
public record EslNotaFiscalValidadaDTO(
        String invoiceId,
        String chaveAcesso,
        String numero,
        String serie,
        LocalDate dataEmissao,
        String status,
        BigDecimal valor,
        BigDecimal peso,
        BigDecimal volume
) {
}
