package com.dashboard.api.service;

import com.dashboard.api.dto.indicadoresgestao.HorariosCorteImportacaoMensagemDTO;
import com.dashboard.api.dto.indicadoresgestao.HorariosCorteImportacaoResultadoDTO;
import com.dashboard.api.model.HorarioCorteEntity;
import com.dashboard.api.repository.HorarioCorteRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class HorariosCorteImportacaoService {

    private static final Logger log = LoggerFactory.getLogger(HorariosCorteImportacaoService.class);

    private static final List<String> CABECALHO_ESPERADO = List.of(
            "DATA",
            "LINHA OU OPERACAO",
            "INICIO",
            "MANIFESTADO",
            "SM GERADA",
            "CORTE",
            "OBSERVACAO"
    );

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<DateTimeFormatter> TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("H:mm:ss"),
            DateTimeFormatter.ofPattern("HH:mm:ss")
    );

    private final HorarioCorteRepository repository;
    private final HorarioCorteFilialMapperService filialMapperService;

    public HorariosCorteImportacaoService(
            HorarioCorteRepository repository,
            HorarioCorteFilialMapperService filialMapperService
    ) {
        this.repository = repository;
        this.filialMapperService = filialMapperService;
    }

    @Transactional
    public HorariosCorteImportacaoResultadoDTO importar(MultipartFile arquivo) {
        validarArquivo(arquivo);

        List<HorariosCorteImportacaoMensagemDTO> avisos = new ArrayList<>();
        List<HorariosCorteImportacaoMensagemDTO> rejeicoes = new ArrayList<>();
        Map<String, RegistroImportacao> registrosPorChave = new LinkedHashMap<>();
        int linhasProcessadas = 0;
        int linhasSubstituidasArquivo = 0;
        LocalDateTime importadoEm = LocalDateTime.now();
        String importadoPor = usuarioAtual();
        HorarioCorteFilialMapperService.FilialMappingContext mappingContext = filialMapperService.criarContexto();

        try (InputStream inputStream = arquivo.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("A planilha está vazia.");
            }

            validarCabecalho(sheet.getRow(0));

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (linhaVazia(row)) {
                    continue;
                }

                linhasProcessadas++;
                try {
                    RegistroImportacao registro = parseLinha(row, arquivo.getOriginalFilename(), importadoEm, importadoPor, mappingContext, avisos);
                    RegistroImportacao anterior = registrosPorChave.put(registro.chaveSubstituicao(), registro);
                    if (anterior != null) {
                        linhasSubstituidasArquivo++;
                        avisos.add(new HorariosCorteImportacaoMensagemDTO(
                                rowIndex + 1,
                                registro.linhaOuOperacaoOriginal(),
                                "Linha duplicada no mesmo arquivo: a ocorrência mais recente substituiu a anterior."
                        ));
                    }
                } catch (IllegalArgumentException ex) {
                    rejeicoes.add(new HorariosCorteImportacaoMensagemDTO(
                            rowIndex + 1,
                            valorTexto(row, 1),
                            ex.getMessage()
                    ));
                }
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Não foi possível ler a planilha enviada.");
        }

        int linhasSubstituidasBanco = 0;
        for (RegistroImportacao registro : registrosPorChave.values()) {
            HorarioCorteEntity entity = repository.findByDataOperacaoAndLinhaOuOperacaoChave(registro.dataOperacao(), registro.linhaOuOperacaoChave())
                    .orElseGet(HorarioCorteEntity::new);

            if (entity.getId() != null) {
                linhasSubstituidasBanco++;
            }

            entity.setDataOperacao(registro.dataOperacao());
            entity.setLinhaOuOperacaoOriginal(registro.linhaOuOperacaoOriginal());
            entity.setLinhaOuOperacaoChave(registro.linhaOuOperacaoChave());
            entity.setFilialCanonica(registro.filialCanonica());
            entity.setInicio(registro.inicio());
            entity.setManifestado(registro.manifestado());
            entity.setSmGerada(registro.smGerada());
            entity.setCorte(registro.corte());
            entity.setObservacao(registro.observacao());
            entity.setNomeArquivo(registro.nomeArquivo());
            entity.setImportadoEm(registro.importadoEm());
            entity.setImportadoPor(registro.importadoPor());
            repository.save(entity);
        }

        log.info(
                "Importação de horários de corte concluída: arquivo={}, processadas={}, importadas={}, substituidas={}, rejeicoes={}",
                arquivo.getOriginalFilename(),
                linhasProcessadas,
                registrosPorChave.size(),
                linhasSubstituidasArquivo + linhasSubstituidasBanco,
                rejeicoes.size()
        );

        return new HorariosCorteImportacaoResultadoDTO(
                Objects.toString(arquivo.getOriginalFilename(), "horarios-corte.xlsx"),
                importadoEm.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                linhasProcessadas,
                registrosPorChave.size(),
                linhasSubstituidasArquivo + linhasSubstituidasBanco,
                rejeicoes.size(),
                avisos,
                rejeicoes
        );
    }

    private RegistroImportacao parseLinha(
            Row row,
            String nomeArquivo,
            LocalDateTime importadoEm,
            String importadoPor,
            HorarioCorteFilialMapperService.FilialMappingContext mappingContext,
            List<HorariosCorteImportacaoMensagemDTO> avisos
    ) {
        LocalDate data = parseData(row.getCell(0), "DATA");
        String linhaOuOperacao = valorTextoObrigatorio(row.getCell(1), "LINHA OU OPERAÇÃO");
        LocalTime inicio = parseHoraObrigatoria(row.getCell(2), "INÍCIO");
        LocalTime manifestado = parseHoraOpcional(row.getCell(3));
        LocalTime smGerada = parseHoraOpcional(row.getCell(4));
        LocalTime corte = parseHoraObrigatoria(row.getCell(5), "CORTE");
        String observacao = valorTexto(row, 6);

        String filialCanonica = filialMapperService.mapearFilialCanonica(linhaOuOperacao, mappingContext);
        if (HorarioCorteFilialMapperService.FILIAL_NAO_MAPEADA.equals(filialCanonica)) {
            avisos.add(new HorariosCorteImportacaoMensagemDTO(
                    row.getRowNum() + 1,
                    linhaOuOperacao,
                    "Filial não mapeada a partir da linha/operação. O registro será mantido com a filial 'Não mapeada'."
            ));
        }

        return new RegistroImportacao(
                data,
                linhaOuOperacao.trim(),
                normalizarChave(linhaOuOperacao),
                filialCanonica,
                inicio,
                manifestado,
                smGerada,
                corte,
                observacao,
                Objects.toString(nomeArquivo, "horarios-corte.xlsx"),
                importadoEm,
                importadoPor
        );
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Envie um arquivo .xlsx para importar os horários de corte.");
        }

        String nome = arquivo.getOriginalFilename();
        if (nome == null || !nome.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Formato inválido. Envie somente arquivos .xlsx.");
        }
    }

    private void validarCabecalho(Row row) {
        if (row == null) {
            throw new IllegalArgumentException("Cabeçalho da planilha não encontrado.");
        }

        List<String> colunas = new ArrayList<>();
        for (int i = 0; i < CABECALHO_ESPERADO.size(); i++) {
            colunas.add(normalizarCabecalho(valorTexto(row, i)));
        }

        if (!CABECALHO_ESPERADO.equals(colunas)) {
            throw new IllegalArgumentException("Cabeçalho inválido. Use o modelo oficial de Horários de Corte.");
        }
    }

    private boolean linhaVazia(Row row) {
        if (row == null) {
            return true;
        }

        for (int i = 0; i < CABECALHO_ESPERADO.size(); i++) {
            if (!valorTexto(row, i).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private LocalDate parseData(Cell cell, String campo) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            throw new IllegalArgumentException("Campo obrigatório ausente: " + campo + ".");
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            return DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate();
        }

        String valor = valorTexto(cell);
        try {
            return LocalDate.parse(valor, DATE_FMT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Data inválida no campo " + campo + ": " + valor + ".");
        }
    }

    private LocalTime parseHoraObrigatoria(Cell cell, String campo) {
        LocalTime valor = parseHoraOpcional(cell);
        if (valor == null) {
            throw new IllegalArgumentException("Campo obrigatório ausente: " + campo + ".");
        }
        return valor;
    }

    private LocalTime parseHoraOpcional(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            BigDecimal segundos = BigDecimal.valueOf(cell.getNumericCellValue())
                    .remainder(BigDecimal.ONE)
                    .multiply(BigDecimal.valueOf(24 * 60 * 60))
                    .setScale(0, RoundingMode.HALF_UP);
            int segundosDia = segundos.intValue();
            if (segundosDia == 24 * 60 * 60) {
                segundosDia = 0;
            }
            return LocalTime.ofSecondOfDay(segundosDia);
        }

        String valor = valorTexto(cell);
        if (valor.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(valor, formatter);
            } catch (DateTimeParseException ignored) {
                // tenta próximo formato
            }
        }

        throw new IllegalArgumentException("Horário inválido: " + valor + ".");
    }

    private String valorTextoObrigatorio(Cell cell, String campo) {
        String valor = valorTexto(cell);
        if (valor.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório ausente: " + campo + ".");
        }
        return valor;
    }

    private String valorTexto(Row row, int cellIndex) {
        if (row == null) {
            return "";
        }
        return valorTexto(row.getCell(cellIndex));
    }

    private String valorTexto(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("pt-BR"));
        return formatter.formatCellValue(cell).trim();
    }

    private String normalizarCabecalho(String valor) {
        return normalizarChave(valor).replace("-", " ");
    }

    private String normalizarChave(String valor) {
        String semAcento = Normalizer.normalize(Objects.toString(valor, ""), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return semAcento
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private String usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getName() != null && !authentication.getName().isBlank()
                ? authentication.getName()
                : "sistema";
    }

    private record RegistroImportacao(
            LocalDate dataOperacao,
            String linhaOuOperacaoOriginal,
            String linhaOuOperacaoChave,
            String filialCanonica,
            LocalTime inicio,
            LocalTime manifestado,
            LocalTime smGerada,
            LocalTime corte,
            String observacao,
            String nomeArquivo,
            LocalDateTime importadoEm,
            String importadoPor
    ) {
        String chaveSubstituicao() {
            return dataOperacao + "|" + linhaOuOperacaoChave;
        }
    }
}
