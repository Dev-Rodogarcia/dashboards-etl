package com.dashboard.api.service.acesso;

import com.dashboard.api.model.acesso.SetorEntity;
import com.dashboard.api.repository.acesso.SetorRepository;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class UsuarioImportacaoValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    private final SetorRepository setorRepository;
    private final UsuarioRepository usuarioRepository;

    public UsuarioImportacaoValidator(SetorRepository setorRepository, UsuarioRepository usuarioRepository) {
        this.setorRepository = setorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public AnaliseResultado analisar(
            Collection<UsuarioImportacaoExcelParser.LinhaPlanilha> linhas,
            Map<String, String> resolucoesSetor
    ) {
        Map<String, SetorEntity> setoresPorNome = new LinkedHashMap<>();
        Map<String, SetorEntity> setoresPorId = new LinkedHashMap<>();

        for (SetorEntity setor : setorRepository.findAllByAtivoTrue()) {
            setoresPorNome.put(normalizarTexto(setor.getNome()), setor);
            if (setor.getId() != null) {
                setoresPorId.put(String.valueOf(setor.getId()), setor);
            }
        }

        Map<String, Integer> frequenciaEmails = calcularFrequenciaEmails(linhas);
        Map<String, String> resolucoesNormalizadas = new LinkedHashMap<>();
        if (resolucoesSetor != null) {
            resolucoesSetor.forEach((setorOriginal, setorDestinoId) ->
                    resolucoesNormalizadas.put(normalizarTexto(setorOriginal), limpar(setorDestinoId)));
        }

        List<LinhaAnalise> preview = new ArrayList<>();
        Set<String> setoresInexistentes = new LinkedHashSet<>();

        for (UsuarioImportacaoExcelParser.LinhaPlanilha linha : linhas) {
            String nome = limpar(linha.nome());
            String emailDigitado = limpar(linha.email());
            String emailNormalizado = normalizarEmail(emailDigitado);
            String setorOriginal = limpar(linha.setor());
            String chaveSetorOriginal = normalizarTexto(setorOriginal);
            List<String> mensagens = new ArrayList<>();

            if (nome == null) {
                mensagens.add("Nome do usuário é obrigatório.");
            }
            if (emailDigitado == null) {
                mensagens.add("E-mail é obrigatório.");
            } else if (!EMAIL_PATTERN.matcher(emailDigitado).matches()) {
                mensagens.add("E-mail inválido.");
            }
            if (setorOriginal == null) {
                mensagens.add("Setor é obrigatório.");
            }

            if (emailNormalizado != null && frequenciaEmails.getOrDefault(emailNormalizado, 0) > 1) {
                mensagens.add("E-mail duplicado dentro do próprio Excel.");
            }

            SetorEntity setorResolvido = null;
            if (setorOriginal != null) {
                String setorDestinoId = resolucoesNormalizadas.get(chaveSetorOriginal);
                if (setorDestinoId != null) {
                    setorResolvido = setoresPorId.get(setorDestinoId);
                    if (setorResolvido == null) {
                        mensagens.add("O setor mapeado para substituição não foi encontrado.");
                    }
                } else {
                    setorResolvido = setoresPorNome.get(chaveSetorOriginal);
                    if (setorResolvido == null) {
                        setoresInexistentes.add(setorOriginal);
                        mensagens.add("Setor inexistente no sistema.");
                    }
                }
            }

            boolean conflitoEmailExistente = false;
            if (emailNormalizado != null) {
                conflitoEmailExistente = usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)
                        || usuarioRepository.existsByLoginIgnoreCase(emailNormalizado);
                if (conflitoEmailExistente) {
                    mensagens.add("Já existe um usuário cadastrado com este e-mail.");
                }
            }

            LinhaStatus status = resolverStatus(mensagens, conflitoEmailExistente, setorResolvido == null && setorOriginal != null);
            preview.add(new LinhaAnalise(
                    linha.linha(),
                    nome,
                    emailNormalizado,
                    setorOriginal,
                    setorResolvido != null && setorResolvido.getId() != null ? String.valueOf(setorResolvido.getId()) : null,
                    setorResolvido != null ? setorResolvido.getNome() : null,
                    status,
                    mensagens
            ));
        }

        int validas = (int) preview.stream().filter(linha -> linha.status() == LinhaStatus.PRONTA).count();
        int invalidas = preview.size() - validas;

        return new AnaliseResultado(
                preview,
                List.copyOf(setoresInexistentes),
                validas,
                invalidas,
                validas > 0 && setoresInexistentes.isEmpty()
        );
    }

    private Map<String, Integer> calcularFrequenciaEmails(Collection<UsuarioImportacaoExcelParser.LinhaPlanilha> linhas) {
        Map<String, Integer> frequencia = new LinkedHashMap<>();
        for (UsuarioImportacaoExcelParser.LinhaPlanilha linha : linhas) {
            String emailNormalizado = normalizarEmail(linha.email());
            if (emailNormalizado == null) {
                continue;
            }
            frequencia.put(emailNormalizado, frequencia.getOrDefault(emailNormalizado, 0) + 1);
        }
        return frequencia;
    }

    private LinhaStatus resolverStatus(List<String> mensagens, boolean conflitoEmailExistente, boolean setorPendente) {
        if (setorPendente) {
            return LinhaStatus.SETOR_INEXISTENTE;
        }
        if (!mensagens.isEmpty() && !conflitoEmailExistente) {
            return LinhaStatus.ERRO_VALIDACAO;
        }
        if (conflitoEmailExistente) {
            return LinhaStatus.CONFLITO_EMAIL_EXISTENTE;
        }
        return LinhaStatus.PRONTA;
    }

    private String limpar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private String normalizarEmail(String email) {
        String valor = limpar(email);
        return valor == null ? null : valor.toLowerCase(Locale.ROOT);
    }

    private String normalizarTexto(String valor) {
        String texto = limpar(valor);
        return texto == null ? "" : texto.toLowerCase(Locale.ROOT);
    }

    public record AnaliseResultado(
            List<LinhaAnalise> linhas,
            List<String> setoresInexistentes,
            int validas,
            int invalidas,
            boolean podeImportar
    ) {
    }

    public record LinhaAnalise(
            int linha,
            String nome,
            String email,
            String setorOriginal,
            String setorResolvidoId,
            String setorResolvido,
            LinhaStatus status,
            List<String> mensagens
    ) {
        public LinhaAnalise {
            Objects.requireNonNull(status, "status é obrigatório.");
            mensagens = mensagens == null ? List.of() : List.copyOf(mensagens);
        }
    }

    public enum LinhaStatus {
        PRONTA,
        ERRO_VALIDACAO,
        CONFLITO_EMAIL_EXISTENTE,
        SETOR_INEXISTENTE
    }
}
