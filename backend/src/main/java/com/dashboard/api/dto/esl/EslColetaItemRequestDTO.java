package com.dashboard.api.dto.esl;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record EslColetaItemRequestDTO(
        @NotBlank(message = "O modal é obrigatório")
        @Pattern(regexp = "rodo|air", message = "O modal deve ser rodo ou air")
        String modal,
        @NotNull(message = "O valor das notas fiscais é obrigatório")
        @DecimalMin(value = "0.00", message = "O valor das notas fiscais não pode ser negativo")
        BigDecimal valorNotasFiscais,
        @NotNull(message = "A quantidade de volumes é obrigatória")
        @Min(value = 1, message = "A quantidade de volumes deve ser maior que zero")
        Integer quantidadeVolumes,
        @NotNull(message = "O peso real é obrigatório")
        @DecimalMin(value = "0.000", message = "O peso real não pode ser negativo")
        BigDecimal pesoRealNotasFiscais,
        @NotBlank(message = "O documento do remetente é obrigatório")
        @Size(max = 20, message = "O documento do remetente deve ter no máximo 20 caracteres")
        String documentoRemetente,
        @NotBlank(message = "O documento do destinatário é obrigatório")
        @Size(max = 20, message = "O documento do destinatário deve ter no máximo 20 caracteres")
        String documentoDestinatario,
        @NotBlank(message = "O documento do pagador é obrigatório")
        @Size(max = 20, message = "O documento do pagador deve ter no máximo 20 caracteres")
        String documentoPagador,
        @NotEmpty(message = "Informe ao menos uma nota fiscal para o item da coleta")
        List<@NotBlank(message = "O invoiceId ESL é obrigatório")
                @Size(max = 80, message = "O invoiceId ESL deve ter no máximo 80 caracteres") String> invoiceIds,
        @DecimalMin(value = "0.000", message = "A altura não pode ser negativa")
        BigDecimal altura,
        @DecimalMin(value = "0.000", message = "O comprimento não pode ser negativo")
        BigDecimal comprimento,
        @DecimalMin(value = "0.000", message = "A largura não pode ser negativa")
        BigDecimal largura,
        @DecimalMin(value = "0.000", message = "O peso cubado não pode ser negativo")
        BigDecimal pesoCubado,
        OffsetDateTime previsaoEntrega
) {
}
