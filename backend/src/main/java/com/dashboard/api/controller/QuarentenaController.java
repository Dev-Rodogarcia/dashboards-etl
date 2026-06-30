package com.dashboard.api.controller;

import com.dashboard.api.service.IntegracoesService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestParam(defaultValue = "100") Integer tamanho
    ) {
        ResponseEntity<String> respostaSatelite = integracoesService.consultarErrosQuarentena(pagina, tamanho);

        return ResponseEntity
                .status(respostaSatelite.getStatusCode())
                .contentType(respostaSatelite.getHeaders().getContentType() != null
                        ? respostaSatelite.getHeaders().getContentType()
                        : MediaType.APPLICATION_JSON)
                .body(respostaSatelite.getBody());
    }
}
