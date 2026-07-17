package com.dashboard.api.controller;

import com.dashboard.api.config.EslContextoOperacionalProvider;
import com.dashboard.api.dto.esl.EslCotacaoCriacaoRequestDTO;
import com.dashboard.api.dto.esl.EslCotacaoRespostaDTO;
import com.dashboard.api.service.EslOperacoesService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/esl/cotacoes")
@PreAuthorize("@acessoSeguranca.podeAcessar('cotacoes')")
public class EslCotacoesController {

    private final EslOperacoesService eslOperacoesService;
    private final EslContextoOperacionalProvider contextoProvider;

    public EslCotacoesController(
            EslOperacoesService eslOperacoesService,
            EslContextoOperacionalProvider contextoProvider
    ) {
        this.eslOperacoesService = eslOperacoesService;
        this.contextoProvider = contextoProvider;
    }

    @PostMapping
    public ResponseEntity<EslCotacaoRespostaDTO> criar(
            @RequestParam @NotBlank @Size(max = 160) String filial,
            @Valid @RequestBody EslCotacaoCriacaoRequestDTO solicitacao
    ) {
        EslCotacaoRespostaDTO recibo = eslOperacoesService.criarCotacao(solicitacao, contextoProvider.obter(filial));
        return ResponseEntity.status(HttpStatus.CREATED).body(recibo);
    }
}
