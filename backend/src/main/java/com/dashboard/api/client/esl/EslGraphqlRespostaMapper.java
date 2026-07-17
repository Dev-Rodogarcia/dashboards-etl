package com.dashboard.api.client.esl;

import com.dashboard.api.dto.esl.EslColetaListagemItemDTO;
import com.dashboard.api.dto.esl.EslColetaListagemRespostaDTO;
import com.dashboard.api.dto.esl.EslColetaRespostaDTO;
import com.dashboard.api.dto.esl.EslCotacaoRespostaDTO;
import com.dashboard.api.dto.esl.EslCotacaoTrechoRespostaDTO;
import com.dashboard.api.dto.esl.EslNotaFiscalValidadaDTO;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EslGraphqlRespostaMapper {

    public EslCotacaoRespostaDTO paraCotacao(JsonNode recurso) {
        List<EslCotacaoTrechoRespostaDTO> trechos = new ArrayList<>();
        recurso.path("quoteStretchBids").forEach(trecho -> trechos.add(
                new EslCotacaoTrechoRespostaDTO(decimal(trecho, "total"))
        ));

        BigDecimal valorFreteTotal = trechos.stream()
                .map(EslCotacaoTrechoRespostaDTO::valorFrete)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new EslCotacaoRespostaDTO(
                texto(recurso, "id"),
                numeroLongo(recurso, "sequenceCode"),
                texto(recurso, "referenceNumber"),
                data(recurso, "effectiveUntil"),
                texto(recurso, "printUrl"),
                numeroInteiro(recurso, "bidsPendingCount"),
                List.copyOf(trechos),
                valorFreteTotal
        );
    }

    public EslColetaRespostaDTO paraColeta(JsonNode recurso) {
        return new EslColetaRespostaDTO(
                texto(recurso, "id"),
                numeroLongo(recurso, "sequenceCode"),
                texto(recurso, "status"),
                data(recurso, "requestDate"),
                texto(recurso, "requestHour"),
                data(recurso, "serviceDate"),
                texto(recurso, "serviceStartHour"),
                texto(recurso, "serviceEndHour"),
                texto(recurso, "referenceNumber"),
                texto(recurso, "comments"),
                texto(recurso, "cancellationReason"),
                decimal(recurso, "invoicesValue"),
                numeroInteiro(recurso, "invoicesVolumes"),
                decimal(recurso, "invoicesWeight"),
                decimal(recurso, "taxedWeight")
        );
    }

    public EslColetaListagemRespostaDTO paraListagemColetas(JsonNode resultado) {
        List<EslColetaListagemItemDTO> itens = new ArrayList<>();
        resultado.path("edges").forEach(edge -> {
            JsonNode node = edge.path("node");
            itens.add(new EslColetaListagemItemDTO(
                    texto(node, "id"),
                    numeroLongo(node, "sequenceCode"),
                    texto(node, "status"),
                    data(node, "requestDate"),
                    texto(node, "requestHour"),
                    data(node, "serviceDate"),
                    texto(node, "serviceStartHour"),
                    texto(node, "serviceEndHour"),
                    texto(node, "referenceNumber"),
                    texto(node, "cancellationReason"),
                    texto(node, "comments")
            ));
        });

        JsonNode pageInfo = resultado.path("pageInfo");
        return new EslColetaListagemRespostaDTO(
                List.copyOf(itens),
                pageInfo.path("hasNextPage").asBoolean(false),
                texto(pageInfo, "endCursor")
        );
    }

    public List<EslNotaFiscalValidadaDTO> paraNotasFiscais(JsonNode resultado) {
        List<EslNotaFiscalValidadaDTO> notas = new ArrayList<>();
        resultado.path("edges").forEach(edge -> {
            JsonNode node = edge.path("node");
            notas.add(new EslNotaFiscalValidadaDTO(
                    texto(node, "id"),
                    texto(node, "key"),
                    texto(node, "number"),
                    texto(node, "series"),
                    dataOuTimestamp(node, "issuedAt"),
                    texto(node, "status"),
                    decimal(node, "value"),
                    decimal(node, "weight"),
                    decimal(node, "volume")
            ));
        });
        return List.copyOf(notas);
    }

    private String texto(JsonNode node, String campo) {
        JsonNode valor = node.path(campo);
        return valor.isMissingNode() || valor.isNull() ? null : valor.asText();
    }

    private Long numeroLongo(JsonNode node, String campo) {
        JsonNode valor = node.path(campo);
        return valor.isNumber() ? valor.longValue() : null;
    }

    private Integer numeroInteiro(JsonNode node, String campo) {
        JsonNode valor = node.path(campo);
        return valor.isNumber() ? valor.intValue() : null;
    }

    private BigDecimal decimal(JsonNode node, String campo) {
        JsonNode valor = node.path(campo);
        return valor.isNumber() ? valor.decimalValue() : null;
    }

    private LocalDate data(JsonNode node, String campo) {
        String valor = texto(node, campo);
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDate dataOuTimestamp(JsonNode node, String campo) {
        String valor = texto(node, campo);
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException ex) {
            try {
                return OffsetDateTime.parse(valor).toLocalDate();
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }
}
