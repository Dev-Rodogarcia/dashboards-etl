package com.dashboard.api.dto.esl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record EslCotacaoTrechoRequestDTO(
        @NotBlank(message = "O modal é obrigatório")
        @Pattern(regexp = "rodo|air", message = "O modal deve ser rodo ou air")
        String modal,
        @NotBlank(message = "O tipo de cálculo é obrigatório")
        @Pattern(regexp = "price_table|manual", message = "O tipo de cálculo deve ser price_table ou manual")
        String tipoCalculo,
        @Size(max = 160, message = "A tabela de preço deve ter no máximo 160 caracteres")
        String tabelaPreco,
        @NotBlank(message = "O documento do pagador é obrigatório")
        @Size(max = 20, message = "O documento do pagador deve ter no máximo 20 caracteres")
        String documentoPagador,
        @NotBlank(message = "O documento do remetente é obrigatório")
        @Size(max = 20, message = "O documento do remetente deve ter no máximo 20 caracteres")
        String documentoRemetente,
        @NotBlank(message = "O documento do destinatário é obrigatório")
        @Size(max = 20, message = "O documento do destinatário deve ter no máximo 20 caracteres")
        String documentoDestinatario,
        @NotNull(message = "A cidade de origem é obrigatória")
        @Valid EslCidadeRequestDTO cidadeOrigem,
        @Size(max = 12, message = "O CEP de origem deve ter no máximo 12 caracteres")
        String cepOrigem,
        @NotNull(message = "A cidade de destino é obrigatória")
        @Valid EslCidadeRequestDTO cidadeDestino,
        @Size(max = 12, message = "O CEP de destino deve ter no máximo 12 caracteres")
        String cepDestino,
        @NotBlank(message = "A classificação do produto é obrigatória")
        @Size(max = 160, message = "A classificação do produto deve ter no máximo 160 caracteres")
        String classificacaoProduto,
        @NotNull(message = "O valor das notas fiscais é obrigatório")
        @DecimalMin(value = "0.00", message = "O valor das notas fiscais não pode ser negativo")
        BigDecimal valorNotasFiscais,
        @NotNull(message = "A quantidade de volumes é obrigatória")
        @Min(value = 1, message = "A quantidade de volumes deve ser maior que zero")
        Integer quantidadeVolumes,
        @NotNull(message = "O peso real é obrigatório")
        @DecimalMin(value = "0.000", message = "O peso real não pode ser negativo")
        BigDecimal pesoReal,
        @NotNull(message = "O volume cúbico é obrigatório")
        @DecimalMin(value = "0.000", message = "O volume cúbico não pode ser negativo")
        BigDecimal volumeCubico
) {
}
