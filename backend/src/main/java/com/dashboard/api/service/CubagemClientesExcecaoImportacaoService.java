package com.dashboard.api.service;

import com.dashboard.api.dto.indicadoresgestao.CubagemClientesImportacaoErroDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemClientesImportacaoImportadoDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemClientesImportacaoPreviewLinhaDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemClientesImportacaoPreviewResponseDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemClientesImportacaoResultadoDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemClientesImportacaoTotaisDTO;
import com.dashboard.api.repository.ClienteExcecaoCubagemSqlRepository;
import com.dashboard.api.repository.ClienteExcecaoCubagemSqlRepository.ClienteExcecaoCubagemRegistro;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CubagemClientesExcecaoImportacaoService {

    private final CubagemClientesExcecaoImportacaoParser parser;
    private final ClienteExcecaoCubagemSqlRepository repository;

    public CubagemClientesExcecaoImportacaoService(
            CubagemClientesExcecaoImportacaoParser parser,
            ClienteExcecaoCubagemSqlRepository repository
    ) {
        this.parser = parser;
        this.repository = repository;
    }

    public CubagemClientesImportacaoPreviewResponseDTO preValidar(MultipartFile arquivo) {
        CubagemClientesExcecaoImportacaoParser.PlanilhaImportada planilha = parser.parse(arquivo);
        List<LinhaAnalise> analise = analisar(planilha.linhas());
        return montarPreview(planilha.nomeArquivo(), analise);
    }

    @Transactional
    public CubagemClientesImportacaoResultadoDTO importar(MultipartFile arquivo, String authenticationName) {
        CubagemClientesExcecaoImportacaoParser.PlanilhaImportada planilha = parser.parse(arquivo);
        List<LinhaAnalise> analise = analisar(planilha.linhas());

        List<ClienteExcecaoCubagemRegistro> registros = new ArrayList<>();
        List<CubagemClientesImportacaoImportadoDTO> importados = new ArrayList<>();
        List<CubagemClientesImportacaoErroDTO> erros = new ArrayList<>();

        for (LinhaAnalise linha : analise) {
            CubagemClientesExcecaoImportacaoParser.LinhaPlanilha item = linha.linha();
            if (!linha.mensagens().isEmpty()) {
                erros.add(new CubagemClientesImportacaoErroDTO(
                        item.linha(),
                        String.join(" ", linha.mensagens()),
                        "validacao"
                ));
                continue;
            }

            registros.add(new ClienteExcecaoCubagemRegistro(
                    item.clienteCnpj(),
                    item.razaoSocial(),
                    item.nomeFantasia(),
                    item.cidadeUf()
            ));
            importados.add(new CubagemClientesImportacaoImportadoDTO(
                    item.linha(),
                    item.clienteCnpj(),
                    item.razaoSocial()
            ));
        }

        if (!registros.isEmpty()) {
            repository.merge(registros, normalizarUsuario(authenticationName));
        }

        return new CubagemClientesImportacaoResultadoDTO(
                analise.size(),
                importados.size(),
                erros.size(),
                List.copyOf(importados),
                List.copyOf(erros)
        );
    }

    private CubagemClientesImportacaoPreviewResponseDTO montarPreview(String arquivo, List<LinhaAnalise> analise) {
        List<CubagemClientesImportacaoPreviewLinhaDTO> preview = analise.stream()
                .map(this::previewLinha)
                .toList();
        int validas = (int) analise.stream().filter(item -> item.mensagens().isEmpty()).count();
        int invalidas = analise.size() - validas;

        return new CubagemClientesImportacaoPreviewResponseDTO(
                arquivo,
                new CubagemClientesImportacaoTotaisDTO(analise.size(), validas, invalidas),
                invalidas == 0,
                preview
        );
    }

    private CubagemClientesImportacaoPreviewLinhaDTO previewLinha(LinhaAnalise linha) {
        CubagemClientesExcecaoImportacaoParser.LinhaPlanilha item = linha.linha();
        return new CubagemClientesImportacaoPreviewLinhaDTO(
                item.linha(),
                item.clienteCnpj(),
                item.razaoSocial(),
                item.nomeFantasia(),
                item.cidadeUf(),
                linha.mensagens().isEmpty() ? "PRONTA" : "ERRO_VALIDACAO",
                linha.mensagens()
        );
    }

    private List<LinhaAnalise> analisar(List<CubagemClientesExcecaoImportacaoParser.LinhaPlanilha> linhas) {
        Map<String, Integer> frequencias = new LinkedHashMap<>();
        for (CubagemClientesExcecaoImportacaoParser.LinhaPlanilha linha : linhas) {
            frequencias.merge(linha.chaveImportacao(), 1, Integer::sum);
        }

        return linhas.stream()
                .map(linha -> {
                    List<String> mensagens = new ArrayList<>(linha.mensagens());
                    if (linha.clienteCnpj() != null
                            && !linha.clienteCnpj().isBlank()
                            && linha.clienteCnpj().length() == 14
                            && frequencias.getOrDefault(linha.chaveImportacao(), 0) > 1) {
                        mensagens.add("CNPJ duplicado na planilha.");
                    }
                    return new LinhaAnalise(linha, List.copyOf(mensagens));
                })
                .toList();
    }

    private String normalizarUsuario(String authenticationName) {
        if (authenticationName == null || authenticationName.isBlank()) {
            return "sistema";
        }
        String valor = authenticationName.trim();
        return valor.length() > 100 ? valor.substring(0, 100) : valor;
    }

    private record LinhaAnalise(
            CubagemClientesExcecaoImportacaoParser.LinhaPlanilha linha,
            List<String> mensagens
    ) {
    }
}
