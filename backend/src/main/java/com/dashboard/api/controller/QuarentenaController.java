package com.dashboard.api.controller;

import com.dashboard.api.service.IntegracoesService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/etl/quarentena")
@PreAuthorize("@acessoSeguranca.podeAcessar('integracoes')")
public class QuarentenaController {

    private final IntegracoesService integracoesService;

    public QuarentenaController(IntegracoesService integracoesService) {
        this.integracoesService = integracoesService;
    }

    @GetMapping(value = "/erros", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> listarErrosManuais(
            @RequestParam(defaultValue = "0") Integer pagina,
            @RequestParam(defaultValue = "100") Integer tamanho,
            @RequestParam(required = false) List<String> destino
    ) {
        ResponseEntity<String> respostaSatelite = integracoesService.consultarErrosQuarentena(pagina, tamanho, destino);

        return ResponseEntity
                .status(respostaSatelite.getStatusCode())
                .contentType(respostaSatelite.getHeaders().getContentType() != null
                        ? respostaSatelite.getHeaders().getContentType()
                        : MediaType.APPLICATION_JSON)
                .body(respostaSatelite.getBody());
    }

    @GetMapping(value = "/historico", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> listarHistorico(
            @RequestParam(defaultValue = "0") Integer pagina,
            @RequestParam(defaultValue = "100") Integer tamanho,
            @RequestParam(required = false) String dataInicial,
            @RequestParam(required = false) String dataFinal,
            @RequestParam(required = false) List<String> destino
    ) {
        ResponseEntity<String> respostaSatelite = integracoesService.consultarHistoricoQuarentena(
                pagina, tamanho, dataInicial, dataFinal, destino
        );
        return ResponseEntity.status(respostaSatelite.getStatusCode())
                .contentType(respostaSatelite.getHeaders().getContentType() != null
                        ? respostaSatelite.getHeaders().getContentType() : MediaType.APPLICATION_JSON)
                .body(respostaSatelite.getBody());
    }

    @GetMapping(value = "/historico/exportacao", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> exportarHistorico(
            @RequestParam(required = false) String dataInicial,
            @RequestParam(required = false) String dataFinal,
            @RequestParam(required = false) List<String> destino
    ) {
        StreamingResponseBody corpo = outputStream -> integracoesService.exportarHistoricoQuarentena(
                dataInicial, dataFinal, destino, outputStream
        );
        return ResponseEntity.ok().contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("historico-repescagens-integracoes.csv", StandardCharsets.UTF_8).build().toString())
                .body(corpo);
    }

    @GetMapping(value = "/erros/exportacao", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> exportarErrosManuais(
            @RequestParam(required = false) List<String> destino
    ) {
        StreamingResponseBody corpo = outputStream -> integracoesService.exportarErrosQuarentena(destino, outputStream);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("quarentena-integracoes.csv", StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(corpo);
    }
}
