package com.dashboard.api.service.acesso;

import com.dashboard.api.dto.acesso.UsuarioImportacaoCriadoDTO;
import com.dashboard.api.dto.acesso.UsuarioImportacaoCredencialTemporariaDTO;
import com.dashboard.api.dto.acesso.UsuarioImportacaoErroDTO;
import com.dashboard.api.dto.acesso.UsuarioImportacaoIgnoradoDTO;
import com.dashboard.api.dto.acesso.UsuarioImportacaoLoteRequestDTO;
import com.dashboard.api.dto.acesso.UsuarioImportacaoPreValidacaoResponseDTO;
import com.dashboard.api.dto.acesso.UsuarioImportacaoPreviewLinhaDTO;
import com.dashboard.api.dto.acesso.UsuarioImportacaoResultadoDTO;
import com.dashboard.api.dto.acesso.UsuarioImportacaoSetorResolucaoDTO;
import com.dashboard.api.dto.acesso.UsuarioImportacaoTotaisDTO;
import com.dashboard.api.dto.acesso.UsuarioRequestDTO;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.model.acesso.UsuarioImportacaoLoteEntity;
import com.dashboard.api.repository.acesso.UsuarioImportacaoLoteRepository;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UsuarioImportacaoService {

    private static final String CARACTERES_MAIUSCULOS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String CARACTERES_MINUSCULOS = "abcdefghijkmnopqrstuvwxyz";
    private static final String CARACTERES_NUMERICOS = "23456789";
    private static final String CARACTERES_ESPECIAIS = "!@#$%&*()-_=+?";
    private static final String TODOS_CARACTERES = CARACTERES_MAIUSCULOS + CARACTERES_MINUSCULOS + CARACTERES_NUMERICOS + CARACTERES_ESPECIAIS;

    private final UsuarioImportacaoExcelParser excelParser;
    private final UsuarioImportacaoValidator validator;
    private final UsuarioImportacaoLoteRepository loteRepository;
    private final GestaoUsuarioService gestaoUsuarioService;
    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;
    private final PoliticaSenhaService politicaSenhaService;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public UsuarioImportacaoService(
            UsuarioImportacaoExcelParser excelParser,
            UsuarioImportacaoValidator validator,
            UsuarioImportacaoLoteRepository loteRepository,
            GestaoUsuarioService gestaoUsuarioService,
            UsuarioRepository usuarioRepository,
            AuditService auditService,
            PoliticaSenhaService politicaSenhaService,
            ObjectMapper objectMapper
    ) {
        this.excelParser = excelParser;
        this.validator = validator;
        this.loteRepository = loteRepository;
        this.gestaoUsuarioService = gestaoUsuarioService;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
        this.politicaSenhaService = politicaSenhaService;
        this.objectMapper = objectMapper;
    }

    public UsuarioImportacaoPreValidacaoResponseDTO preValidar(MultipartFile arquivo) {
        loteRepository.deleteByExpiraEmBefore(Instant.now());

        UsuarioImportacaoExcelParser.PlanilhaImportada planilha = excelParser.parse(arquivo);
        String importacaoId = UUID.randomUUID().toString();

        UsuarioImportacaoLoteEntity lote = new UsuarioImportacaoLoteEntity();
        lote.setTokenImportacao(importacaoId);
        lote.setArquivoNome(planilha.nomeArquivo());
        lote.setPayloadJson(serializar(new LotePayload(planilha.nomeArquivo(), planilha.linhas())));
        lote.setCriadoPor(loginAtual());
        lote.setExpiraEm(Instant.now().plus(24, ChronoUnit.HOURS));
        loteRepository.save(lote);

        return montarResposta(importacaoId, planilha.nomeArquivo(), analisar(planilha.linhas(), Map.of()));
    }

    public byte[] gerarTemplateExcel() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Usuários");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Nome do Usuário");
            header.createCell(1).setCellValue("E-mail");
            header.createCell(2).setCellValue("Setor");

            sheet.createRow(1).createCell(0).setCellValue("Fulano da Silva");
            sheet.getRow(1).createCell(1).setCellValue("fulano@empresa.com");
            sheet.getRow(1).createCell(2).setCellValue("Logística");

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível gerar o template da importação.");
        }
    }

    public UsuarioImportacaoPreValidacaoResponseDTO revalidar(UsuarioImportacaoLoteRequestDTO request) {
        LotePayload lote = carregarLotePayload(request.importacaoId());
        UsuarioImportacaoValidator.AnaliseResultado analise = analisar(lote.linhas(), mapearResolucoes(request.resolucoesSetor()));
        return montarResposta(request.importacaoId(), lote.arquivoNome(), analise);
    }

    public UsuarioImportacaoResultadoDTO importar(UsuarioImportacaoLoteRequestDTO request) {
        LotePayload lote = carregarLotePayload(request.importacaoId());
        Map<String, String> resolucoes = mapearResolucoes(request.resolucoesSetor());
        UsuarioImportacaoValidator.AnaliseResultado analise = analisar(lote.linhas(), resolucoes);

        if (!analise.setoresInexistentes().isEmpty()) {
            throw new IllegalStateException("Existem setores inexistentes pendentes de resolução antes da importação.");
        }

        List<UsuarioImportacaoCriadoDTO> criados = new ArrayList<>();
        List<UsuarioImportacaoIgnoradoDTO> ignorados = new ArrayList<>();
        List<UsuarioImportacaoErroDTO> erros = new ArrayList<>();
        List<UsuarioImportacaoCredencialTemporariaDTO> credenciais = new ArrayList<>();

        for (UsuarioImportacaoValidator.LinhaAnalise linha : analise.linhas()) {
            switch (linha.status()) {
                case PRONTA -> importarLinha(linha, criados, ignorados, erros, credenciais);
                case CONFLITO_EMAIL_EXISTENTE -> ignorados.add(new UsuarioImportacaoIgnoradoDTO(
                        linha.email(),
                        primeiraMensagem(linha.mensagens(), "Já existe um usuário cadastrado com este e-mail.")
                ));
                case ERRO_VALIDACAO -> erros.add(new UsuarioImportacaoErroDTO(
                        linha.linha(),
                        linha.email(),
                        String.join(" ", linha.mensagens()),
                        "validacao"
                ));
                case SETOR_INEXISTENTE -> throw new IllegalStateException("Existem setores inexistentes pendentes de resolução antes da importação.");
            }
        }

        UsuarioImportacaoResultadoDTO resultado = new UsuarioImportacaoResultadoDTO(
                analise.linhas().size(),
                criados.size(),
                ignorados.size(),
                erros.size(),
                List.copyOf(criados),
                List.copyOf(ignorados),
                List.copyOf(erros),
                List.copyOf(credenciais)
        );

        auditarLote(request.importacaoId(), lote.arquivoNome(), resolucoes, resultado);
        loteRepository.findByTokenImportacao(request.importacaoId()).ifPresent(loteRepository::delete);
        return resultado;
    }

    private void importarLinha(
            UsuarioImportacaoValidator.LinhaAnalise linha,
            List<UsuarioImportacaoCriadoDTO> criados,
            List<UsuarioImportacaoIgnoradoDTO> ignorados,
            List<UsuarioImportacaoErroDTO> erros,
            List<UsuarioImportacaoCredencialTemporariaDTO> credenciais
    ) {
        String senhaTemporaria = gerarSenhaTemporaria();
        UsuarioRequestDTO request = new UsuarioRequestDTO(
                linha.nome(),
                linha.email(),
                senhaTemporaria,
                senhaTemporaria,
                linha.setorResolvidoId(),
                PermissaoResolverService.PAPEL_USUARIO_COMUM,
                List.of(),
                List.of(),
                true
        );

        try {
            var usuarioCriado = gestaoUsuarioService.criarUsuario(request);
            criados.add(new UsuarioImportacaoCriadoDTO(
                    usuarioCriado.nome(),
                    usuarioCriado.email(),
                    usuarioCriado.setorNome()
            ));
            credenciais.add(new UsuarioImportacaoCredencialTemporariaDTO(
                    usuarioCriado.email(),
                    senhaTemporaria
            ));
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("Já existe um usuário com este e-mail")) {
                ignorados.add(new UsuarioImportacaoIgnoradoDTO(linha.email(), ex.getMessage()));
                return;
            }
            erros.add(new UsuarioImportacaoErroDTO(linha.linha(), linha.email(), ex.getMessage(), "processamento"));
        } catch (RuntimeException ex) {
            erros.add(new UsuarioImportacaoErroDTO(
                    linha.linha(),
                    linha.email(),
                    ex.getMessage() == null || ex.getMessage().isBlank() ? "Falha ao processar a linha." : ex.getMessage(),
                    "processamento"
            ));
        }
    }

    private UsuarioImportacaoValidator.AnaliseResultado analisar(
            List<UsuarioImportacaoExcelParser.LinhaPlanilha> linhas,
            Map<String, String> resolucoes
    ) {
        return validator.analisar(linhas, resolucoes);
    }

    private UsuarioImportacaoPreValidacaoResponseDTO montarResposta(
            String importacaoId,
            String arquivo,
            UsuarioImportacaoValidator.AnaliseResultado analise
    ) {
        List<UsuarioImportacaoPreviewLinhaDTO> preview = analise.linhas().stream()
                .map(linha -> new UsuarioImportacaoPreviewLinhaDTO(
                        linha.linha(),
                        linha.nome(),
                        linha.email(),
                        linha.setorOriginal(),
                        linha.setorResolvido(),
                        linha.status().name(),
                        linha.mensagens()
                ))
                .toList();

        return new UsuarioImportacaoPreValidacaoResponseDTO(
                importacaoId,
                arquivo,
                new UsuarioImportacaoTotaisDTO(preview.size(), analise.validas(), analise.invalidas()),
                analise.setoresInexistentes(),
                analise.podeImportar(),
                preview
        );
    }

    private LotePayload carregarLotePayload(String importacaoId) {
        loteRepository.deleteByExpiraEmBefore(Instant.now());
        UsuarioImportacaoLoteEntity lote = loteRepository.findByTokenImportacao(importacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Importação não encontrada ou expirada."));

        if (lote.getExpiraEm().isBefore(Instant.now())) {
            loteRepository.delete(lote);
            throw new IllegalArgumentException("Importação não encontrada ou expirada.");
        }

        try {
            return objectMapper.readValue(lote.getPayloadJson(), LotePayload.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Não foi possível reprocessar o arquivo importado.");
        }
    }

    private Map<String, String> mapearResolucoes(List<UsuarioImportacaoSetorResolucaoDTO> resolucoesSetor) {
        Map<String, String> resolucoes = new LinkedHashMap<>();
        if (resolucoesSetor == null) {
            return resolucoes;
        }

        for (UsuarioImportacaoSetorResolucaoDTO resolucao : resolucoesSetor) {
            if (resolucao == null || resolucao.setorOriginal() == null || resolucao.setorDestinoId() == null) {
                continue;
            }
            resolucoes.put(resolucao.setorOriginal(), resolucao.setorDestinoId());
        }

        return resolucoes;
    }

    private void auditarLote(
            String importacaoId,
            String arquivoNome,
            Map<String, String> resolucoes,
            UsuarioImportacaoResultadoDTO resultado
    ) {
        UsuarioEntity operador = usuarioAutenticado();
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("importacaoId", importacaoId);
        detalhes.put("arquivo", arquivoNome);
        detalhes.put("resolucoesSetor", resolucoes);
        detalhes.put("totalProcessados", resultado.totalProcessados());
        detalhes.put("totalCriados", resultado.totalCriados());
        detalhes.put("totalIgnorados", resultado.totalIgnorados());
        detalhes.put("totalErros", resultado.totalErros());

        auditService.registrar(
                AcaoAudit.USUARIO_IMPORTACAO_LOTE,
                operador.getId(),
                operador.getLogin(),
                "usuario-importacao",
                serializar(detalhes)
        );
    }

    private UsuarioEntity usuarioAutenticado() {
        String login = loginAtual();
        return usuarioRepository.findByEmailIgnoreCase(login)
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
    }

    private String loginAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "sistema";
        }
        return authentication.getName().trim().toLowerCase();
    }

    private String gerarSenhaTemporaria() {
        while (true) {
            String senha = gerarSenhaTemporariaBruta();
            try {
                politicaSenhaService.validar(senha);
                return senha;
            } catch (IllegalArgumentException ignored) {
                // tenta novamente mantendo aleatoriedade
            }
        }
    }

    private String gerarSenhaTemporariaBruta() {
        List<Character> caracteres = new ArrayList<>();
        caracteres.add(sortear(CARACTERES_MAIUSCULOS));
        caracteres.add(sortear(CARACTERES_MINUSCULOS));
        caracteres.add(sortear(CARACTERES_NUMERICOS));
        caracteres.add(sortear(CARACTERES_ESPECIAIS));

        while (caracteres.size() < 14) {
            caracteres.add(sortear(TODOS_CARACTERES));
        }

        for (int index = caracteres.size() - 1; index > 0; index--) {
            int destino = secureRandom.nextInt(index + 1);
            char atual = caracteres.get(index);
            caracteres.set(index, caracteres.get(destino));
            caracteres.set(destino, atual);
        }

        StringBuilder builder = new StringBuilder(caracteres.size());
        for (char caractere : caracteres) {
            builder.append(caractere);
        }
        return builder.toString();
    }

    private char sortear(String origem) {
        return origem.charAt(secureRandom.nextInt(origem.length()));
    }

    private String primeiraMensagem(List<String> mensagens, String fallback) {
        return mensagens == null || mensagens.isEmpty() ? fallback : mensagens.get(0);
    }

    private String serializar(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Não foi possível serializar os dados da importação.");
        }
    }

    private record LotePayload(
            String arquivoNome,
            List<UsuarioImportacaoExcelParser.LinhaPlanilha> linhas
    ) {
        private LotePayload {
            linhas = linhas == null ? List.of() : List.copyOf(linhas);
        }
    }
}
