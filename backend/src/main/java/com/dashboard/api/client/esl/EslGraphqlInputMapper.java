package com.dashboard.api.client.esl;

import com.dashboard.api.dto.esl.EslCidadeRequestDTO;
import com.dashboard.api.dto.esl.EslColetaAtualizacaoRequestDTO;
import com.dashboard.api.dto.esl.EslColetaCancelamentoRequestDTO;
import com.dashboard.api.dto.esl.EslColetaCriacaoRequestDTO;
import com.dashboard.api.dto.esl.EslColetaItemRequestDTO;
import com.dashboard.api.dto.esl.EslColetaListagemRequestDTO;
import com.dashboard.api.dto.esl.EslContextoOperacionalDTO;
import com.dashboard.api.dto.esl.EslCotacaoCriacaoRequestDTO;
import com.dashboard.api.dto.esl.EslCotacaoTrechoRequestDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EslGraphqlInputMapper {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Sao_Paulo");

    public Map<String, Object> paraQuoteCreate(
            EslCotacaoCriacaoRequestDTO solicitacao,
            EslContextoOperacionalDTO contexto
    ) {
        Objects.requireNonNull(solicitacao, "A solicitação de cotação é obrigatória.");
        Objects.requireNonNull(contexto, "O contexto operacional é obrigatório.");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("corporation", documento(contexto.documentoCorporacao()));
        params.put("customer", documento(solicitacao.documentoCliente()));
        params.put("requestedAt", formatarDataHora(OffsetDateTime.now(ZONA_NEGOCIO)));
        params.put("requesterName", contexto.nomeSolicitante());
        params.put("requesterEmail", contexto.emailSolicitante());
        adicionarSeHouverTexto(params, "requesterPhone", contexto.telefoneSolicitante());
        params.put("effectiveUntil", formatarData(solicitacao.validade()));
        adicionarSeHouverTexto(params, "referenceNumber", solicitacao.referencia());
        adicionarSeHouverTexto(params, "comments", solicitacao.observacoes());
        params.put("quoteStretchBidsAttributes", solicitacao.trechos().stream()
                .map(this::paraTrechoCotacao)
                .toList());
        return Map.of("params", params);
    }

    public Map<String, Object> paraPickCreate(
            EslColetaCriacaoRequestDTO solicitacao,
            EslContextoOperacionalDTO contexto
    ) {
        Objects.requireNonNull(solicitacao, "A solicitação de coleta é obrigatória.");
        Objects.requireNonNull(contexto, "O contexto operacional é obrigatório.");

        LocalDateTime abertura = LocalDateTime.now(ZONA_NEGOCIO);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("corporation", documento(contexto.documentoCorporacao()));
        params.put("customer", documento(solicitacao.documentoCliente()));
        params.put("requestDate", formatarData(abertura.toLocalDate()));
        params.put("requestHour", abertura.toLocalTime().withSecond(0).withNano(0).toString());
        params.put("requester", contexto.nomeSolicitante());
        adicionarSeHouverTexto(params, "department", contexto.departamento());
        adicionarSeHouverTexto(params, "referenceNumber", solicitacao.referencia());
        params.put("pickupLocation", documento(solicitacao.documentoLocalColeta()));
        params.put("serviceDate", formatarData(solicitacao.dataAgendada()));
        params.put("serviceStartHour", solicitacao.horaInicial().toString());
        params.put("serviceEndHour", solicitacao.horaFinal().toString());
        adicionarSeHouverTexto(params, "notificationEmail", solicitacao.emailNotificacao());
        adicionarSeHouverTexto(params, "notificationPhone", solicitacao.telefoneNotificacao());
        adicionarSeHouverTexto(params, "comments", solicitacao.observacoes());
        params.put("pickItemsAttributes", solicitacao.itens().stream()
                .map(this::paraItemColeta)
                .toList());
        return Map.of("params", params);
    }

    public Map<String, Object> paraPickUpdate(String coletaId, EslColetaAtualizacaoRequestDTO solicitacao) {
        Objects.requireNonNull(solicitacao, "A solicitação de atualização é obrigatória.");

        Map<String, Object> params = new LinkedHashMap<>();
        adicionarSeNaoNulo(params, "requestDate", formatarData(solicitacao.dataSolicitacao()));
        adicionarSeNaoNulo(params, "requestHour", formatarHora(solicitacao.horaSolicitacao()));
        adicionarSeNaoNulo(params, "serviceDate", formatarData(solicitacao.dataAgendada()));
        adicionarSeNaoNulo(params, "serviceStartHour", formatarHora(solicitacao.horaInicial()));
        adicionarSeNaoNulo(params, "serviceEndHour", formatarHora(solicitacao.horaFinal()));
        adicionarSeNaoNulo(params, "notificationEmail", solicitacao.emailNotificacao());
        adicionarSeNaoNulo(params, "notificationPhone", solicitacao.telefoneNotificacao());
        adicionarSeNaoNulo(params, "comments", solicitacao.observacoes());
        if (params.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um campo para atualizar a coleta.");
        }
        return Map.of("id", coletaId, "params", params);
    }

    public Map<String, Object> paraPickCancellation(String coletaId, EslColetaCancelamentoRequestDTO solicitacao) {
        Objects.requireNonNull(solicitacao, "A solicitação de cancelamento é obrigatória.");
        return Map.of(
                "id", coletaId,
                "params", Map.of(
                        "cancellationReason", solicitacao.motivo().valorEsl(),
                        "cancellationDatetime", formatarDataHora(OffsetDateTime.now(ZONA_NEGOCIO))
                )
        );
    }

    public Map<String, Object> paraPickList(
            EslColetaListagemRequestDTO solicitacao,
            EslContextoOperacionalDTO contexto
    ) {
        Objects.requireNonNull(solicitacao, "A solicitação de listagem é obrigatória.");
        Objects.requireNonNull(contexto, "O contexto operacional é obrigatório.");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("requestDate", formatarData(solicitacao.dataSolicitacao()));
        if (contexto.idCorporacao() != null) {
            params.put("corporationId", contexto.idCorporacao());
        } else {
            params.put("corporation", documento(contexto.documentoCorporacao()));
        }

        Map<String, Object> variaveis = new LinkedHashMap<>();
        variaveis.put("params", params);
        adicionarSeHouverTexto(variaveis, "after", solicitacao.cursor());
        variaveis.put("first", solicitacao.tamanhoPaginaNormalizado());
        return variaveis;
    }

    public Map<String, Object> paraInvoiceLookup(String chaveOuNumero) {
        Objects.requireNonNull(chaveOuNumero, "A chave ou número da nota fiscal é obrigatório.");
        String identificador = chaveOuNumero.trim();
        String possivelChave = identificador.replaceAll("\\D", "");
        boolean ehChaveAcesso = possivelChave.length() == 44;
        String campo = ehChaveAcesso ? "key" : "number";
        String valor = ehChaveAcesso ? possivelChave : identificador;
        return Map.of("params", Map.of(campo, valor), "first", 5);
    }

    private Map<String, Object> paraTrechoCotacao(EslCotacaoTrechoRequestDTO trecho) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("modal", trecho.modal());
        resultado.put("calculationType", trecho.tipoCalculo());
        if (StringUtils.hasText(trecho.tabelaPreco())) {
            resultado.put("customerPriceTable", Map.of("name", trecho.tabelaPreco()));
        }
        resultado.put("payer", documento(trecho.documentoPagador()));
        resultado.put("sender", documento(trecho.documentoRemetente()));
        resultado.put("recipient", documento(trecho.documentoDestinatario()));
        resultado.put("originCity", cidade(trecho.cidadeOrigem()));
        resultado.put("destinationCity", cidade(trecho.cidadeDestino()));
        adicionarSeHouverTexto(resultado, "originPostalCode", trecho.cepOrigem());
        adicionarSeHouverTexto(resultado, "destinationPostalCode", trecho.cepDestino());
        resultado.put("productClassification", Map.of("name", trecho.classificacaoProduto()));
        resultado.put("invoicesValue", trecho.valorNotasFiscais());
        resultado.put("invoicesVolumes", trecho.quantidadeVolumes());
        resultado.put("realWeight", trecho.pesoReal());
        resultado.put("cubicVolume", trecho.volumeCubico());
        return resultado;
    }

    private String formatarData(LocalDate data) {
        return data == null ? null : DateTimeFormatter.ISO_LOCAL_DATE.format(data);
    }

    private String formatarDataHora(OffsetDateTime dataHora) {
        return dataHora == null ? null : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dataHora);
    }

    private Map<String, Object> paraItemColeta(EslColetaItemRequestDTO item) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("modal", item.modal());
        resultado.put("invoicesValue", item.valorNotasFiscais());
        resultado.put("invoicesVolumes", item.quantidadeVolumes());
        resultado.put("invoicesRealWeight", item.pesoRealNotasFiscais());
        resultado.put("recipient", documento(item.documentoDestinatario()));
        resultado.put("payer", documento(item.documentoPagador()));
        resultado.put("sender", documento(item.documentoRemetente()));
        resultado.put("pickItemInvoicesAttributes", item.invoiceIds().stream()
                .map(invoiceId -> Map.of("invoiceId", invoiceId))
                .toList());
        adicionarSeNaoNulo(resultado, "invoicesHeight", item.altura());
        adicionarSeNaoNulo(resultado, "invoicesLength", item.comprimento());
        adicionarSeNaoNulo(resultado, "invoicesWidth", item.largura());
        adicionarSeNaoNulo(resultado, "invoicesCubedWeight", item.pesoCubado());
        adicionarSeNaoNulo(resultado, "deliveryPredictionAt", item.previsaoEntrega());
        return resultado;
    }

    private Map<String, Object> documento(String valor) {
        return Map.of("document", valor);
    }

    private Map<String, Object> cidade(EslCidadeRequestDTO cidade) {
        return Map.of("name", cidade.nome(), "stateCode", cidade.uf().toUpperCase());
    }

    private String formatarHora(java.time.LocalTime horario) {
        return horario == null ? null : horario.toString();
    }

    private void adicionarSeHouverTexto(Map<String, Object> destino, String chave, String valor) {
        if (StringUtils.hasText(valor)) {
            destino.put(chave, valor.trim());
        }
    }

    private void adicionarSeNaoNulo(Map<String, Object> destino, String chave, Object valor) {
        if (valor != null) {
            destino.put(chave, valor);
        }
    }
}
