package com.dashboard.api.controller;

import com.dashboard.api.client.IntegracaoSateliteClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/painel/integracoes")
@PreAuthorize("@acessoSeguranca.podeAcessar('integracoes')")
public class IntegracoesController {

    private final IntegracaoSateliteClient integracaoSateliteClient;

    public IntegracoesController(IntegracaoSateliteClient integracaoSateliteClient) {
        this.integracaoSateliteClient = integracaoSateliteClient;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> consultarIntegracoes(@RequestParam MultiValueMap<String, String> params) {
        ResponseEntity<String> respostaSatelite = integracaoSateliteClient.buscarIntegracoesClientes(params);

        return ResponseEntity
                .status(respostaSatelite.getStatusCode())
                .contentType(respostaSatelite.getHeaders().getContentType() != null
                        ? respostaSatelite.getHeaders().getContentType()
                        : MediaType.APPLICATION_JSON)
                .body(respostaSatelite.getBody());
    }

    @GetMapping(value = "/logs/{id}/imagem", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE })
    public ResponseEntity<String> consultarImagemCanhoto(@PathVariable Long id) {
        ResponseEntity<String> respostaSatelite = integracaoSateliteClient.buscarImagemLog(id);

        return ResponseEntity
                .status(respostaSatelite.getStatusCode())
                .contentType(respostaSatelite.getHeaders().getContentType() != null
                        ? respostaSatelite.getHeaders().getContentType()
                        : MediaType.TEXT_PLAIN)
                .body(respostaSatelite.getBody());
    }
}
