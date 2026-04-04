package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.HorariosCorteOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.HorariosCorteSeriePointDTO;
import com.dashboard.api.model.VisaoHorariosCorteEntity;
import com.dashboard.api.repository.VisaoHorariosCorteRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndicadoresGestaoAVistaServiceTest {

    @Mock
    private VisaoHorariosCorteRepository repository;

    private EscopoFilialServiceStub escopoFilialService;
    private StubHorarioCorteFilialMapperService filialMapperService;

    private IndicadoresGestaoAVistaService service;

    @BeforeEach
    void setUp() {
        escopoFilialService = new EscopoFilialServiceStub();
        filialMapperService = new StubHorarioCorteFilialMapperService();
        service = new IndicadoresGestaoAVistaService(new ValidadorPeriodoService(), repository, escopoFilialService, filialMapperService);
    }

    @Test
    void buscarOverviewDeveCalcularTotaisPercentualEUltimaImportacao() {
        when(repository.findByDataBetween(any(), any())).thenReturn(List.of(
                row(1L, "SPO", LocalDate.of(2026, 4, 2), true, 0, "arquivo-1.xlsx", LocalDateTime.of(2026, 4, 2, 10, 0)),
                row(2L, "SPO", LocalDate.of(2026, 4, 2), false, 62, "arquivo-1.xlsx", LocalDateTime.of(2026, 4, 2, 10, 0)),
                row(3L, "REC", LocalDate.of(2026, 4, 3), true, -451, "arquivo-2.xlsx", LocalDateTime.of(2026, 4, 3, 9, 30))
        ));

        HorariosCorteOverviewDTO overview = service.buscarHorariosCorteOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(overview.totalProgramado()).isEqualTo(3);
        assertThat(overview.saidasNoHorario()).isEqualTo(2);
        assertThat(overview.pctNoHorario()).isEqualTo(66.7);
        assertThat(overview.ultimaImportacaoArquivo()).isEqualTo("arquivo-2.xlsx");
        assertThat(overview.ultimaImportacaoEm()).isEqualTo("2026-04-03T09:30:00");
    }

    @Test
    void buscarSerieDeveAgruparPorDataEFilial() {
        when(repository.findByDataBetween(any(), any())).thenReturn(List.of(
                row(1L, "SPO", LocalDate.of(2026, 4, 2), true, 0, "arquivo-1.xlsx", LocalDateTime.of(2026, 4, 2, 10, 0)),
                row(2L, "SPO", LocalDate.of(2026, 4, 2), false, 62, "arquivo-1.xlsx", LocalDateTime.of(2026, 4, 2, 10, 0)),
                row(3L, "REC", LocalDate.of(2026, 4, 2), true, 0, "arquivo-1.xlsx", LocalDateTime.of(2026, 4, 2, 10, 0))
        ));

        List<HorariosCorteSeriePointDTO> serie = service.buscarHorariosCorteSerie(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(serie).extracting(HorariosCorteSeriePointDTO::filial, HorariosCorteSeriePointDTO::totalProgramado, HorariosCorteSeriePointDTO::saidasNoHorario)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("REC", 1, 1),
                        org.assertj.core.groups.Tuple.tuple("SPO", 2, 1)
                );
    }

    @Test
    void buscarOverviewDeveRespeitarFiltroDeFiliais() {
        when(repository.findByDataBetween(any(), any())).thenReturn(List.of(
                row(1L, "SPO", LocalDate.of(2026, 4, 2), true, 0, "arquivo-1.xlsx", LocalDateTime.of(2026, 4, 2, 10, 0)),
                row(2L, "REC", LocalDate.of(2026, 4, 2), false, 45, "arquivo-1.xlsx", LocalDateTime.of(2026, 4, 2, 10, 0))
        ));

        HorariosCorteOverviewDTO overview = service.buscarHorariosCorteOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of("filiais", List.of("SPO")))
        );

        assertThat(overview.totalProgramado()).isEqualTo(1);
        assertThat(overview.saidasNoHorario()).isEqualTo(1);
        assertThat(overview.pctNoHorario()).isEqualTo(100.0);
    }

    @Test
    void buscarOverviewDeveResolverFilialAPartirDaLinhaOperacaoQuandoVierNaoMapeada() {
        filialMapperService.definirMapeamento("SPO-CAS", "SPO");

        when(repository.findByDataBetween(any(), any())).thenReturn(List.of(
                row(1L, HorarioCorteFilialMapperService.FILIAL_NAO_MAPEADA, "SPO-CAS", LocalDate.of(2026, 4, 2), true, 0, "arquivo-1.xlsx", LocalDateTime.of(2026, 4, 2, 10, 0))
        ));

        HorariosCorteOverviewDTO overview = service.buscarHorariosCorteOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of("filiais", List.of("SPO")))
        );

        List<HorariosCorteSeriePointDTO> serie = service.buscarHorariosCorteSerie(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(overview.totalProgramado()).isEqualTo(1);
        assertThat(overview.saidasNoHorario()).isEqualTo(1);
        assertThat(serie).extracting(HorariosCorteSeriePointDTO::filial)
                .containsExactly("SPO");
    }

    private static VisaoHorariosCorteEntity row(
            Long id,
            String filial,
            LocalDate data,
            Boolean saiuNoHorario,
            Integer atrasoMinutos,
            String arquivo,
            LocalDateTime importadoEm
    ) {
        return row(id, filial, filial + "-CAS", data, saiuNoHorario, atrasoMinutos, arquivo, importadoEm);
    }

    private static VisaoHorariosCorteEntity row(
            Long id,
            String filial,
            String linhaOuOperacao,
            LocalDate data,
            Boolean saiuNoHorario,
            Integer atrasoMinutos,
            String arquivo,
            LocalDateTime importadoEm
    ) {
        VisaoHorariosCorteEntity entity = novaInstancia(VisaoHorariosCorteEntity.class);
        setField(entity, "id", id);
        setField(entity, "filial", filial);
        setField(entity, "linhaOuOperacao", linhaOuOperacao);
        setField(entity, "data", data);
        setField(entity, "inicio", LocalTime.of(23, 20));
        setField(entity, "manifestado", LocalTime.of(0, 30));
        setField(entity, "smGerada", LocalTime.of(0, 32));
        setField(entity, "corte", LocalTime.of(23, 30));
        setField(entity, "saiuNoHorario", saiuNoHorario);
        setField(entity, "atrasoMinutos", atrasoMinutos);
        setField(entity, "nomeArquivo", arquivo);
        setField(entity, "importadoEm", importadoEm);
        setField(entity, "dataExtracao", importadoEm);
        return entity;
    }

    private static <T> T novaInstancia(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Nao foi possivel instanciar " + type.getSimpleName(), ex);
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

    private static final class EscopoFilialServiceStub extends EscopoFilialService {

        private EscopoFilial escopoAtual = EscopoFilial.comAcessoTotal();

        private EscopoFilialServiceStub() {
            super(null, null);
        }

        @Override
        public EscopoFilial escopoAtual() {
            return escopoAtual;
        }
    }

    private static final class StubHorarioCorteFilialMapperService extends HorarioCorteFilialMapperService {

        private final Map<String, String> mapeamentos = new java.util.HashMap<>();

        private StubHorarioCorteFilialMapperService() {
            super(null);
        }

        void definirMapeamento(String linhaOuOperacao, String filialCanonica) {
            mapeamentos.put(linhaOuOperacao, filialCanonica);
        }

        @Override
        public FilialMappingContext criarContexto() {
            return new FilialMappingContext(Map.of());
        }

        @Override
        public String mapearFilialCanonica(String linhaOuOperacao, FilialMappingContext context) {
            return mapeamentos.getOrDefault(linhaOuOperacao, FILIAL_NAO_MAPEADA);
        }
    }
}
