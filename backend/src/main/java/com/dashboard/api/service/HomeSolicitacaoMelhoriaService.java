package com.dashboard.api.service;

import com.dashboard.api.dto.home.HomeSolicitacaoMelhoriaAnexoConteudoDTO;
import com.dashboard.api.dto.home.HomeSolicitacaoMelhoriaDTO;
import com.dashboard.api.dto.home.HomeSolicitacaoMelhoriaRequestDTO;
import com.dashboard.api.model.acesso.HomeSolicitacaoMelhoriaAnexoEntity;
import com.dashboard.api.model.acesso.HomeSolicitacaoMelhoriaEntity;
import com.dashboard.api.repository.acesso.HomeSolicitacaoMelhoriaAnexoRepository;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.HomeSolicitacaoMelhoriaRepository;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import com.dashboard.api.util.UploadFileTypeValidator;
import com.dashboard.api.util.UploadFileTypeValidator.FileType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HomeSolicitacaoMelhoriaService {

    private static final Set<String> TIPOS_VALIDOS = Set.of("MELHORIA", "AUTOMACAO", "DASHBOARD", "CORRECAO", "OUTRO");
    private static final int MAX_ANEXOS = 5;
    private static final long MAX_TAMANHO_ANEXO_BYTES = 10L * 1024 * 1024;
    private static final long MAX_TAMANHO_TOTAL_ANEXOS_BYTES = 20L * 1024 * 1024;
    private static final Map<String, String> TIPOS_ANEXO_POR_EXTENSAO = Map.of(
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "xls", "application/vnd.ms-excel",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "csv", "text/csv"
    );
    private final HomeSolicitacaoMelhoriaRepository repository;
    private final HomeSolicitacaoMelhoriaAnexoRepository anexoRepository;
    private final UsuarioRepository usuarioRepository;

    public HomeSolicitacaoMelhoriaService(
            HomeSolicitacaoMelhoriaRepository repository,
            HomeSolicitacaoMelhoriaAnexoRepository anexoRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.repository = repository;
        this.anexoRepository = anexoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<HomeSolicitacaoMelhoriaDTO> listarAtivas() {
        return repository.listarAtivasOrdenadas().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<HomeSolicitacaoMelhoriaDTO> listarAtivasDoSolicitante(String usuarioLogin) {
        return repository.listarAtivasDoSolicitanteOrdenadas(normalizarLogin(usuarioLogin))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public HomeSolicitacaoMelhoriaDTO criar(
            HomeSolicitacaoMelhoriaRequestDTO request,
            String usuarioLogin,
            List<MultipartFile> anexos
    ) {
        String login = normalizarLogin(usuarioLogin);
        UsuarioEntity usuario = usuarioRepository.findByEmailIgnoreCase(login)
                .filter(UsuarioEntity::isAtivo)
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));

        List<AnexoValidado> anexosValidados = validarAnexos(anexos);
        HomeSolicitacaoMelhoriaEntity entity = new HomeSolicitacaoMelhoriaEntity();
        aplicarRequest(entity, request);
        entity.setStatus("ABERTA");
        entity.setAtivo(true);
        entity.setSolicitanteNome(usuario.getNome());
        entity.setSolicitanteEmail(usuario.getEmail());
        entity.setAtualizadoPor(login);
        HomeSolicitacaoMelhoriaEntity salva = repository.save(entity);
        if (!anexosValidados.isEmpty()) {
            anexoRepository.saveAll(anexosValidados.stream().map(anexo -> criarAnexo(salva, anexo)).toList());
        }
        return toDto(salva);
    }

    @Transactional
    public HomeSolicitacaoMelhoriaDTO concluir(Long id, String usuarioLogin) {
        HomeSolicitacaoMelhoriaEntity entity = buscarAtiva(id);
        entity.setStatus("CONCLUIDA");
        entity.setConcluidoEm(Instant.now());
        entity.setAtualizadoPor(normalizarLogin(usuarioLogin));
        return toDto(repository.save(entity));
    }

    @Transactional
    public void arquivar(Long id, String usuarioLogin) {
        HomeSolicitacaoMelhoriaEntity entity = buscarAtiva(id);
        entity.setStatus("ARQUIVADA");
        entity.setArquivadoEm(Instant.now());
        entity.setAtualizadoPor(normalizarLogin(usuarioLogin));
        repository.save(entity);
        anexoRepository.removerConteudosDaSolicitacao(id, Instant.now());
    }

    @Transactional(readOnly = true)
    public HomeSolicitacaoMelhoriaAnexoConteudoDTO baixarAnexo(
            Long solicitacaoId,
            Long anexoId,
            String usuarioLogin,
            boolean podeGerenciarSolicitacoes
    ) {
        HomeSolicitacaoMelhoriaEntity solicitacao = buscarAtiva(solicitacaoId);
        if (!podeGerenciarSolicitacoes
                && !solicitacao.getSolicitanteEmail().equalsIgnoreCase(normalizarLogin(usuarioLogin))) {
            throw new AccessDeniedException("Acesso negado ao anexo da solicitação.");
        }

        HomeSolicitacaoMelhoriaAnexoEntity anexo = anexoRepository
                .findByIdAndSolicitacaoIdAndAtivoTrue(anexoId, solicitacaoId)
                .filter(item -> item.getConteudo() != null)
                .orElseThrow(() -> new IllegalArgumentException("Anexo não encontrado."));

        return new HomeSolicitacaoMelhoriaAnexoConteudoDTO(
                anexo.getNomeOriginal(),
                anexo.getTipoConteudo(),
                anexo.getConteudo()
        );
    }

    private HomeSolicitacaoMelhoriaEntity buscarAtiva(Long id) {
        HomeSolicitacaoMelhoriaEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada."));
        if (!entity.isAtivo()) {
            throw new IllegalArgumentException("Solicitação não encontrada.");
        }
        return entity;
    }

    private void aplicarRequest(HomeSolicitacaoMelhoriaEntity entity, HomeSolicitacaoMelhoriaRequestDTO request) {
        String tipo = request.tipo().trim().toUpperCase(Locale.ROOT);
        if (!TIPOS_VALIDOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de solicitação inválido.");
        }

        entity.setTipo(tipo);
        entity.setTitulo(request.titulo().trim());
        entity.setDescricao(request.descricao().trim());
        entity.setResultadoEsperado(limparOpcional(request.resultadoEsperado()));
        entity.setLocalAplicacao(limparOpcional(request.localAplicacao()));
    }

    private String normalizarLogin(String usuarioLogin) {
        if (usuarioLogin == null || usuarioLogin.isBlank()) {
            throw new IllegalArgumentException("Usuário autenticado não encontrado.");
        }
        return usuarioLogin.trim().toLowerCase(Locale.ROOT);
    }

    private String limparOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }

    private List<AnexoValidado> validarAnexos(List<MultipartFile> anexos) {
        List<MultipartFile> arquivos = anexos == null
                ? List.of()
                : anexos.stream().filter(Objects::nonNull).filter(arquivo -> !arquivo.isEmpty()).toList();
        if (arquivos.size() > MAX_ANEXOS) {
            throw new IllegalArgumentException("Envie no máximo 5 anexos por solicitação.");
        }

        long tamanhoTotal = 0;
        List<AnexoValidado> validados = new ArrayList<>();
        for (MultipartFile arquivo : arquivos) {
            if (arquivo.getSize() > MAX_TAMANHO_ANEXO_BYTES) {
                throw new IllegalArgumentException("Cada anexo pode ter no máximo 10 MB.");
            }
            tamanhoTotal += arquivo.getSize();
            if (tamanhoTotal > MAX_TAMANHO_TOTAL_ANEXOS_BYTES) {
                throw new IllegalArgumentException("Os anexos somados podem ter no máximo 20 MB.");
            }

            String nomeOriginal = normalizarNomeArquivo(arquivo.getOriginalFilename());
            String extensao = extensao(nomeOriginal);
            String tipoConteudo = TIPOS_ANEXO_POR_EXTENSAO.get(extensao);
            if (tipoConteudo == null) {
                throw new IllegalArgumentException("Formato inválido. Envie PDF, imagem PNG/JPG ou planilha XLS/XLSX/CSV.");
            }

            byte[] conteudo = lerConteudo(arquivo);
            if (!conteudoCompativel(arquivo, extensao, conteudo)) {
                throw new IllegalArgumentException("O conteúdo do anexo não corresponde ao formato informado.");
            }
            validados.add(new AnexoValidado(nomeOriginal, tipoConteudo, conteudo));
        }
        return validados;
    }

    private HomeSolicitacaoMelhoriaAnexoEntity criarAnexo(
            HomeSolicitacaoMelhoriaEntity solicitacao,
            AnexoValidado anexo
    ) {
        HomeSolicitacaoMelhoriaAnexoEntity entity = new HomeSolicitacaoMelhoriaAnexoEntity();
        entity.setSolicitacao(solicitacao);
        entity.setNomeOriginal(anexo.nomeOriginal());
        entity.setTipoConteudo(anexo.tipoConteudo());
        entity.setTamanhoBytes(anexo.conteudo().length);
        entity.setConteudo(anexo.conteudo());
        entity.setAtivo(true);
        entity.setCriadoEm(Instant.now());
        return entity;
    }

    private String normalizarNomeArquivo(String nome) {
        String nomeOriginal = Objects.toString(nome, "").replace('\\', '/').trim();
        String nomeBase = nomeOriginal.substring(nomeOriginal.lastIndexOf('/') + 1);
        if (nomeBase.isBlank() || nomeBase.length() > 255 || nomeBase.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("O nome do anexo é inválido.");
        }
        return nomeBase;
    }

    private String extensao(String nome) {
        int indice = nome.lastIndexOf('.');
        return indice > 0 && indice < nome.length() - 1
                ? nome.substring(indice + 1).toLowerCase(Locale.ROOT)
                : "";
    }

    private byte[] lerConteudo(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Não foi possível ler o anexo enviado.");
        }
    }

    private boolean conteudoCompativel(MultipartFile arquivo, String extensao, byte[] conteudo) {
        if ("csv".equals(extensao)) {
            try {
                UploadFileTypeValidator.validarAssinatura(
                        arquivo,
                        UploadFileTypeValidator.tipos(FileType.CSV),
                        "O conteúdo do anexo não corresponde ao formato informado."
                );
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }

        return switch (extensao) {
            case "pdf" -> iniciaCom(conteudo, "%PDF-".getBytes(StandardCharsets.US_ASCII));
            case "png" -> iniciaCom(conteudo, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "jpg", "jpeg" -> iniciaCom(conteudo, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "xls" -> iniciaCom(conteudo, new byte[] {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1});
            case "xlsx" -> conteudo.length >= 4 && conteudo[0] == 0x50 && conteudo[1] == 0x4B
                    && (conteudo[2] == 0x03 || conteudo[2] == 0x05 || conteudo[2] == 0x07)
                    && (conteudo[3] == 0x04 || conteudo[3] == 0x06 || conteudo[3] == 0x08);
            default -> false;
        };
    }

    private boolean iniciaCom(byte[] conteudo, byte[] assinatura) {
        if (conteudo.length < assinatura.length) return false;
        for (int indice = 0; indice < assinatura.length; indice++) {
            if (conteudo[indice] != assinatura[indice]) return false;
        }
        return true;
    }

    private HomeSolicitacaoMelhoriaDTO toDto(HomeSolicitacaoMelhoriaEntity entity) {
        return new HomeSolicitacaoMelhoriaDTO(
                String.valueOf(entity.getId()),
                entity.getTipo(),
                entity.getTitulo(),
                entity.getDescricao(),
                entity.getResultadoEsperado(),
                entity.getLocalAplicacao(),
                entity.getStatus(),
                entity.getSolicitanteNome(),
                entity.getSolicitanteEmail(),
                entity.getCriadoEm(),
                entity.getConcluidoEm(),
                entity.getArquivadoEm(),
                entity.getAtualizadoPor(),
                anexoRepository.listarMetadadosAtivos(entity.getId())
        );
    }

    private record AnexoValidado(String nomeOriginal, String tipoConteudo, byte[] conteudo) {
    }
}
