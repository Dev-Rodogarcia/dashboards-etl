package com.dashboard.api.controller;

import com.dashboard.api.dto.LoginRequestDTO;
import com.dashboard.api.dto.LoginResponseDTO;
import com.dashboard.api.dto.SessaoUsuarioDTO;
import com.dashboard.api.dto.acesso.AlterarSenhaRequestDTO;
import com.dashboard.api.exception.RespostaErroPadrao;
import com.dashboard.api.security.RateLimitService;
import com.dashboard.api.service.acesso.AutenticacaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private static final Logger log = LoggerFactory.getLogger(AutenticacaoController.class);
    private final AutenticacaoService autenticacaoService;
    private final RateLimitService rateLimitService;

    public AutenticacaoController(AutenticacaoService autenticacaoService, RateLimitService rateLimitService) {
        this.autenticacaoService = autenticacaoService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        RateLimitService.RateLimitDecision decisao = rateLimitService.consumirTentativaLogin(ip, request.usuario());
        if (!decisao.permitido()) {
            RespostaErroPadrao erro = new RespostaErroPadrao(
                    LocalDateTime.now(),
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too Many Requests",
                    "Muitas tentativas de autenticação. Aguarde alguns minutos e tente novamente."
            );
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(decisao.retryAfterSeconds()))
                    .body(erro);
        }

        try {
            LoginResponseDTO resposta = autenticacaoService.autenticar(request.usuario(), request.senha());
            rateLimitService.limparTentativasLogin(ip, request.usuario());
            log.info("Login realizado com sucesso para usuario={} admin={} setor={}",
                    resposta.usuario().login(),
                    resposta.usuario().admin(),
                    resposta.usuario().setor().nome());
            return ResponseEntity.ok(resposta);
        } catch (IllegalArgumentException ex) {
            log.warn("Falha de autenticacao para usuario={}", request.usuario());
            RespostaErroPadrao erro = new RespostaErroPadrao(
                    LocalDateTime.now(),
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized",
                    "Usuario ou senha invalidos."
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(erro);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<SessaoUsuarioDTO> me(Authentication authentication) {
        return ResponseEntity.ok(autenticacaoService.buscarSessaoAtual(authentication.getName()));
    }

    @PostMapping("/alterar-senha")
    public ResponseEntity<?> alterarSenha(Authentication authentication, @Valid @RequestBody AlterarSenhaRequestDTO request) {
        try {
            autenticacaoService.alterarSenha(authentication.getName(), request.senhaAtual(), request.novaSenha());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException ex) {
            RespostaErroPadrao erro = new RespostaErroPadrao(
                    LocalDateTime.now(),
                    HttpStatus.BAD_REQUEST.value(),
                    "Bad Request",
                    ex.getMessage()
            );
            return ResponseEntity.badRequest().body(erro);
        }
    }
}
