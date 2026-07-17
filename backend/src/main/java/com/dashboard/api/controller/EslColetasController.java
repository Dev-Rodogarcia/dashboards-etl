package com.dashboard.api.controller;

import com.dashboard.api.config.EslContextoOperacionalProvider;
import com.dashboard.api.dto.esl.EslColetaAtualizacaoRequestDTO;
import com.dashboard.api.dto.esl.EslColetaCancelamentoRequestDTO;
import com.dashboard.api.dto.esl.EslColetaCriacaoRequestDTO;
import com.dashboard.api.dto.esl.EslColetaListagemRequestDTO;
import com.dashboard.api.dto.esl.EslColetaListagemRespostaDTO;
import com.dashboard.api.dto.esl.EslColetaRespostaDTO;
import com.dashboard.api.dto.esl.EslNotaFiscalValidadaDTO;
import com.dashboard.api.service.EslOperacoesService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/esl/coletas")
@PreAuthorize("@acessoSeguranca.podeAcessar('coletas')")
public class EslColetasController {

    private static final int TAMANHO_MAXIMO_LISTAGEM_ESL = 100;

    private final EslOperacoesService eslOperacoesService;
    private final EslContextoOperacionalProvider contextoProvider;

    public EslColetasController(
            EslOperacoesService eslOperacoesService,
            EslContextoOperacionalProvider contextoProvider
    ) {
        this.eslOperacoesService = eslOperacoesService;
        this.contextoProvider = contextoProvider;
    }

    @GetMapping("/validar-nf/{chaveOrNumero}")
    public ResponseEntity<EslNotaFiscalValidadaDTO> validarNotaFiscal(
            @PathVariable
            @Pattern(regexp = "[A-Za-z0-9._-]{1,64}", message = "Informe uma chave de acesso ou número de NF válido")
            String chaveOrNumero,
            @RequestParam @NotBlank @Size(max = 160) String filial
    ) {
        contextoProvider.obter(filial);
        return ResponseEntity.ok(eslOperacoesService.validarNotaFiscal(chaveOrNumero));
    }

    @GetMapping
    public ResponseEntity<EslColetaListagemRespostaDTO> listar(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataSolicitacao,
            @RequestParam @NotBlank @Size(max = 160) String filial
    ) {
        EslColetaListagemRequestDTO solicitacao = new EslColetaListagemRequestDTO(
                dataSolicitacao,
                null,
                TAMANHO_MAXIMO_LISTAGEM_ESL
        );
        return ResponseEntity.ok(eslOperacoesService.listarColetas(solicitacao, contextoProvider.obter(filial)));
    }

    @PostMapping
    public ResponseEntity<EslColetaRespostaDTO> criar(
            @RequestParam @NotBlank @Size(max = 160) String filial,
            @Valid @RequestBody EslColetaCriacaoRequestDTO solicitacao
    ) {
        EslColetaRespostaDTO recibo = eslOperacoesService.criarColeta(solicitacao, contextoProvider.obter(filial));
        return ResponseEntity.status(HttpStatus.CREATED).body(recibo);
    }

    @PatchMapping("/{eslId}")
    public ResponseEntity<EslColetaRespostaDTO> atualizar(
            @PathVariable @NotBlank @Size(max = 80) String eslId,
            @RequestParam @NotBlank @Size(max = 160) String filial,
            @Valid @RequestBody EslColetaAtualizacaoRequestDTO solicitacao
    ) {
        contextoProvider.obter(filial);
        return ResponseEntity.ok(eslOperacoesService.atualizarColeta(eslId, solicitacao));
    }

    @PostMapping("/{eslId}/cancelamento")
    public ResponseEntity<EslColetaRespostaDTO> cancelar(
            @PathVariable @NotBlank @Size(max = 80) String eslId,
            @RequestParam @NotBlank @Size(max = 160) String filial,
            @Valid @RequestBody EslColetaCancelamentoRequestDTO solicitacao
    ) {
        contextoProvider.obter(filial);
        return ResponseEntity.ok(eslOperacoesService.cancelarColeta(eslId, solicitacao));
    }
}
