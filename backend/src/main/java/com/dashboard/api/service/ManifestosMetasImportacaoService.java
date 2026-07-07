package com.dashboard.api.service;

import com.dashboard.api.dto.manifestos.ManifestosCostGoalConfigRequestDTO;
import com.dashboard.api.dto.manifestos.ManifestosMetasImportacaoErroDTO;
import com.dashboard.api.dto.manifestos.ManifestosMetasImportacaoImportadoDTO;
import com.dashboard.api.dto.manifestos.ManifestosMetasImportacaoPreviewLinhaDTO;
import com.dashboard.api.dto.manifestos.ManifestosMetasImportacaoPreviewResponseDTO;
import com.dashboard.api.dto.manifestos.ManifestosMetasImportacaoResultadoDTO;
import com.dashboard.api.dto.manifestos.ManifestosMetasImportacaoTotaisDTO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ManifestosMetasImportacaoService {

    private final ManifestosMetasImportacaoExcelParser parser;
    private final ManifestosCostGoalService costGoalService;

    public ManifestosMetasImportacaoService(
            ManifestosMetasImportacaoExcelParser parser,
            ManifestosCostGoalService costGoalService
    ) {
        this.parser = parser;
        this.costGoalService = costGoalService;
    }

    public ManifestosMetasImportacaoPreviewResponseDTO preValidar(MultipartFile arquivo) {
        ManifestosMetasImportacaoExcelParser.PlanilhaImportada planilha = parser.parse(arquivo);
        List<LinhaAnalise> analise = analisar(planilha.linhas());
        return montarPreview(planilha.nomeArquivo(), analise);
    }

    public byte[] gerarTemplateExcel() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Metas Manifestos");
            var header = sheet.createRow(0);
            List<String> colunas = parser.cabecalhoEsperado();
            for (int index = 0; index < colunas.size(); index++) {
                header.createCell(index).setCellValue(colunas.get(index));
            }

            var exemplo = sheet.createRow(1);
            exemplo.createCell(0).setCellValue("05/2026");
            exemplo.createCell(1).setCellValue("GLOBAL");
            exemplo.createCell(2).setCellValue("GERAL");
            exemplo.createCell(3).setCellValue("GERAL");
            exemplo.createCell(4).setCellValue(8400000.00);

            var exemploFilial = sheet.createRow(2);
            exemploFilial.createCell(0).setCellValue("05/2026");
            exemploFilial.createCell(1).setCellValue("SPO");
            exemploFilial.createCell(2).setCellValue("FROTA + PX");
            exemploFilial.createCell(3).setCellValue("DISTRIBUIÇÃO");
            exemploFilial.createCell(4).setCellValue(1200000.00);

            for (int index = 0; index < colunas.size(); index++) {
                sheet.autoSizeColumn(index);
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível gerar o template da importação de metas.");
        }
    }

    @Transactional
    public ManifestosMetasImportacaoResultadoDTO importar(MultipartFile arquivo, String authenticationName) {
        ManifestosMetasImportacaoExcelParser.PlanilhaImportada planilha = parser.parse(arquivo);
        List<LinhaAnalise> analise = analisar(planilha.linhas());

        List<ManifestosMetasImportacaoImportadoDTO> importados = new ArrayList<>();
        List<ManifestosMetasImportacaoErroDTO> erros = new ArrayList<>();

        for (LinhaAnalise linha : analise) {
            if (!linha.mensagens().isEmpty()) {
                erros.add(new ManifestosMetasImportacaoErroDTO(
                        linha.linha().linha(),
                        String.join(" ", linha.mensagens()),
                        "validacao"
                ));
                continue;
            }

            try {
                ManifestosMetasImportacaoExcelParser.LinhaPlanilha item = linha.linha();
                costGoalService.salvar(new ManifestosCostGoalConfigRequestDTO(
                        item.branchId() == null ? "GLOBAL" : item.branchId(),
                        item.contractType(),
                        item.contractTypeKey(),
                        item.classificationKey(),
                        item.ano(),
                        item.mes(),
                        item.costGoal()
                ), authenticationName);
                importados.add(new ManifestosMetasImportacaoImportadoDTO(
                        item.linha(),
                        item.ano(),
                        item.mes(),
                        item.branchId() == null ? "GLOBAL" : item.branchId(),
                        item.contractTypeKey(),
                        item.classificationKey(),
                        item.costGoal()
                ));
            } catch (RuntimeException ex) {
                erros.add(new ManifestosMetasImportacaoErroDTO(
                        linha.linha().linha(),
                        ex.getMessage() == null || ex.getMessage().isBlank() ? "Falha ao importar a meta." : ex.getMessage(),
                        "processamento"
                ));
            }
        }

        return new ManifestosMetasImportacaoResultadoDTO(
                analise.size(),
                importados.size(),
                erros.size(),
                List.copyOf(importados),
                List.copyOf(erros)
        );
    }

    private ManifestosMetasImportacaoPreviewResponseDTO montarPreview(String arquivo, List<LinhaAnalise> analise) {
        List<ManifestosMetasImportacaoPreviewLinhaDTO> preview = analise.stream()
                .map(this::previewLinha)
                .toList();
        int validas = (int) analise.stream().filter(item -> item.mensagens().isEmpty()).count();
        int invalidas = analise.size() - validas;

        return new ManifestosMetasImportacaoPreviewResponseDTO(
                arquivo,
                new ManifestosMetasImportacaoTotaisDTO(analise.size(), validas, invalidas),
                invalidas == 0,
                preview
        );
    }

    private ManifestosMetasImportacaoPreviewLinhaDTO previewLinha(LinhaAnalise linha) {
        ManifestosMetasImportacaoExcelParser.LinhaPlanilha item = linha.linha();
        return new ManifestosMetasImportacaoPreviewLinhaDTO(
                item.linha(),
                item.ano(),
                item.mes(),
                item.branchId() == null ? "GLOBAL" : item.branchId(),
                item.contractType(),
                item.contractTypeKey(),
                item.classificationKey(),
                escala(item.costGoal()),
                linha.mensagens().isEmpty() ? "PRONTA" : "ERRO_VALIDACAO",
                linha.mensagens()
        );
    }

    private List<LinhaAnalise> analisar(List<ManifestosMetasImportacaoExcelParser.LinhaPlanilha> linhas) {
        Map<String, Integer> frequencias = new LinkedHashMap<>();
        for (ManifestosMetasImportacaoExcelParser.LinhaPlanilha linha : linhas) {
            frequencias.merge(linha.chaveImportacao(), 1, Integer::sum);
        }

        return linhas.stream()
                .map(linha -> {
                    List<String> mensagens = new ArrayList<>(linha.mensagens());
                    if (linha.ano() == null || linha.mes() == null) {
                        mensagens.add("Ano e mês da competência precisam estar válidos.");
                    }
                    if (linha.costGoal() == null) {
                        mensagens.add("Valor da Meta precisa estar válido.");
                    }
                    if (frequencias.getOrDefault(linha.chaveImportacao(), 0) > 1) {
                        mensagens.add("Chave duplicada na planilha para filial, competência, contrato e classificação.");
                    }
                    return new LinhaAnalise(linha, List.copyOf(mensagens));
                })
                .toList();
    }

    private BigDecimal escala(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private record LinhaAnalise(
            ManifestosMetasImportacaoExcelParser.LinhaPlanilha linha,
            List<String> mensagens
    ) {
    }
}
