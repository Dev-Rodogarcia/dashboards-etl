package com.dashboard.api.service;

import com.dashboard.api.dto.indicadoresgestao.HorariosCorteImportacaoResultadoDTO;
import com.dashboard.api.model.HorarioCorteEntity;
import com.dashboard.api.repository.HorarioCorteRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class HorariosCorteImportacaoServiceTest {

    private HorarioCorteRepository repository;
    private HorarioCorteRepositoryStub repositoryStub;

    private StubHorarioCorteFilialMapperService filialMapperService;

    private HorariosCorteImportacaoService service;

    @BeforeEach
    void setUp() {
        repositoryStub = new HorarioCorteRepositoryStub();
        repository = repositoryStub.createProxy();
        filialMapperService = new StubHorarioCorteFilialMapperService();
        service = new HorariosCorteImportacaoService(repository, filialMapperService);
    }

    @Test
    void importarDeveLerPlanilhaXlsxSalvarRegistroEReportarAvisoDeFilialNaoMapeada() throws Exception {
        filialMapperService.definirContexto(new LinkedHashMap<>());
        filialMapperService.definirMapeamento("SPO-CAS", HorarioCorteFilialMapperService.FILIAL_NAO_MAPEADA);
        repositoryStub.definirBusca(Optional.empty());

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "horarios-corte.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookPadrao(new Object[][]{
                        {46114d, "SPO-CAS", 0.9305555556d, 0.9444444444d, 0.9479166667d, 0.9583333333d, "Liberado"}
                })
        );

        HorariosCorteImportacaoResultadoDTO resultado = service.importar(arquivo);

        assertThat(resultado.linhasProcessadas()).isEqualTo(1);
        assertThat(resultado.linhasImportadas()).isEqualTo(1);
        assertThat(resultado.linhasSubstituidas()).isZero();
        assertThat(resultado.avisos()).hasSize(1);
        assertThat(resultado.rejeicoes()).isEmpty();
        assertThat(repositoryStub.quantidadeSaves()).isEqualTo(1);

        HorarioCorteEntity salvo = repositoryStub.ultimoSalvo();
        assertThat(salvo.getDataOperacao()).isEqualTo(LocalDate.of(2026, 4, 2));
        assertThat(salvo.getLinhaOuOperacaoOriginal()).isEqualTo("SPO-CAS");
        assertThat(salvo.getLinhaOuOperacaoChave()).isEqualTo("SPO-CAS");
        assertThat(salvo.getFilialCanonica()).isEqualTo("Não mapeada");
        assertThat(salvo.getInicio()).isEqualTo(LocalTime.of(22, 20));
        assertThat(salvo.getSmGerada()).isEqualTo(LocalTime.of(22, 45));
        assertThat(salvo.getCorte()).isEqualTo(LocalTime.of(23, 0));
        assertThat(salvo.getNomeArquivo()).isEqualTo("horarios-corte.xlsx");
        assertThat(salvo.getImportadoEm()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void importarDeveSubstituirRegistroExistenteDoMesmoRecorte() throws Exception {
        HorarioCorteEntity existente = new HorarioCorteEntity();
        setField(existente, "id", 99L);

        filialMapperService.definirContexto(Map.of("SPO", "SPO"));
        filialMapperService.definirMapeamento("SPO-CAS", "SPO");
        repositoryStub.definirBusca(Optional.of(existente));

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "horarios-corte.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookPadrao(new Object[][]{
                        {46114d, "SPO-CAS", 0.9305555556d, 0.9444444444d, 0.9479166667d, 0.9583333333d, ""}
                })
        );

        HorariosCorteImportacaoResultadoDTO resultado = service.importar(arquivo);

        assertThat(resultado.linhasImportadas()).isEqualTo(1);
        assertThat(resultado.linhasSubstituidas()).isEqualTo(1);
        assertThat(repositoryStub.ultimoSalvo()).isSameAs(existente);
    }

    @Test
    void importarDeveFalharQuandoCabecalhoNaoBateComModelo() throws Exception {
        byte[] conteudo = workbookComCabecalhoInvalido();
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "horarios-corte.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                conteudo
        );

        assertThatThrownBy(() -> service.importar(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cabeçalho inválido");
    }

    private static byte[] workbookPadrao(Object[][] linhas) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Planilha1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("DATA");
            header.createCell(1).setCellValue("LINHA OU OPERAÇÃO");
            header.createCell(2).setCellValue("INÍCIO");
            header.createCell(3).setCellValue("MANIFESTADO");
            header.createCell(4).setCellValue("SM GERADA");
            header.createCell(5).setCellValue("CORTE");
            header.createCell(6).setCellValue("OBSERVAÇÃO");

            for (int i = 0; i < linhas.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < linhas[i].length; j++) {
                    Object valor = linhas[i][j];
                    if (valor instanceof Number numero) {
                        row.createCell(j).setCellValue(numero.doubleValue());
                    } else {
                        row.createCell(j).setCellValue(String.valueOf(valor));
                    }
                }
            }

            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static byte[] workbookComCabecalhoInvalido() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Planilha1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("DATA");
            header.createCell(1).setCellValue("ROTA");
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static void setField(Object target, String nomeCampo, Object valor) {
        try {
            Field field = target.getClass().getDeclaredField(nomeCampo);
            field.setAccessible(true);
            field.set(target, valor);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Nao foi possivel definir o campo " + nomeCampo + ".", ex);
        }
    }

    private static final class HorarioCorteRepositoryStub implements InvocationHandler {

        private Optional<HorarioCorteEntity> busca = Optional.empty();
        private HorarioCorteEntity ultimoSalvo;
        private int quantidadeSaves;

        private HorarioCorteRepository createProxy() {
            return (HorarioCorteRepository) Proxy.newProxyInstance(
                    HorarioCorteRepository.class.getClassLoader(),
                    new Class<?>[]{HorarioCorteRepository.class},
                    this
            );
        }

        private void definirBusca(Optional<HorarioCorteEntity> busca) {
            this.busca = busca;
        }

        private HorarioCorteEntity ultimoSalvo() {
            return ultimoSalvo;
        }

        private int quantidadeSaves() {
            return quantidadeSaves;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findByDataOperacaoAndLinhaOuOperacaoChave" -> busca;
                case "save" -> {
                    ultimoSalvo = (HorarioCorteEntity) args[0];
                    quantidadeSaves++;
                    yield ultimoSalvo;
                }
                case "toString" -> "HorarioCorteRepositoryStub";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException("Metodo nao suportado no stub: " + method.getName());
            };
        }
    }

    private static final class StubHorarioCorteFilialMapperService extends HorarioCorteFilialMapperService {

        private FilialMappingContext context = new FilialMappingContext(new LinkedHashMap<>());
        private final Map<String, String> mapeamentos = new LinkedHashMap<>();

        private StubHorarioCorteFilialMapperService() {
            super(null);
        }

        void definirContexto(Map<String, String> lookup) {
            this.context = new FilialMappingContext(new LinkedHashMap<>(lookup));
        }

        void definirMapeamento(String linhaOuOperacao, String filial) {
            mapeamentos.put(linhaOuOperacao, filial);
        }

        @Override
        public FilialMappingContext criarContexto() {
            return context;
        }

        @Override
        public String mapearFilialCanonica(String linhaOuOperacao, FilialMappingContext context) {
            return mapeamentos.getOrDefault(linhaOuOperacao, FILIAL_NAO_MAPEADA);
        }
    }
}
