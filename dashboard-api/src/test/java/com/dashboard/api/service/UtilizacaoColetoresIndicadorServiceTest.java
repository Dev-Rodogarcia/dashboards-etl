package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresOverviewDTO;
import com.dashboard.api.model.DimFilialEntity;
import com.dashboard.api.model.VisaoManifestosEntity;
import com.dashboard.api.model.VisaoManifestosId;
import com.dashboard.api.repository.DimFilialRepository;
import com.dashboard.api.repository.VisaoManifestosRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilizacaoColetoresIndicadorServiceTest {

    @Mock
    private VisaoManifestosRepository manifestosRepository;

    @Mock
    private DimFilialRepository dimFilialRepository;

    private UtilizacaoColetoresIndicadorService service;

    @BeforeEach
    void setUp() {
        service = new UtilizacaoColetoresIndicadorService(
                new ValidadorPeriodoService(),
                manifestosRepository,
                dimFilialRepository,
                escopoSemRestricao(),
                PeriodoOffsetDateTimeHelper.padrao()
        );
    }

    @Test
    void buscarOverviewDeveCalcularManifestosBipadosEIncompletosPorPernaOperacional() {
        when(dimFilialRepository.findAll()).thenReturn(List.of(filial("SPO"), filial("REC")));
        when(manifestosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                manifesto(10L, "SPO", "REC", "encerrado", "DISTRIBUIÇÃO", null, 4, 2),
                manifesto(11L, "SPO", "SPO, REC", "closed", "TRANSFERÊNCIA", OffsetDateTime.parse("2026-04-02T08:30:00-03:00"), 2, 2)
        ));

        UtilizacaoColetoresOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(overview.manifestosBipados()).isEqualTo(5);
        assertThat(overview.manifestosEmitidos()).isEqualTo(2);
        assertThat(overview.manifestosDescarregamento()).isEqualTo(3);
        assertThat(overview.totalManifestos()).isEqualTo(5);
        assertThat(overview.manifestosIncompletos()).isEqualTo(2);
        assertThat(overview.pctUtilizacao()).isEqualTo(100.0);
    }

    @Test
    void buscarOverviewDeveAplicarStatusExclusoesEPorFilial() {
        when(dimFilialRepository.findAll()).thenReturn(List.of(filial("SPO"), filial("REC")));
        when(manifestosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                manifesto(10L, "SPO", "REC", "encerrado", "DISTRIBUIÇÃO", null, 0, 0),
                manifesto(11L, "SPO", "REC", "encerrado", "CARGA FECHADA (TRÁFEGO)", OffsetDateTime.parse("2026-04-02T08:30:00-03:00"), 2, 2),
                manifesto(12L, "SPO", "REC", "pendente", "DISTRIBUIÇÃO", OffsetDateTime.parse("2026-04-02T08:30:00-03:00"), 2, 2),
                manifesto(13L, "REC", "REC", "encerrado", "TRANSFERÊNCIA", OffsetDateTime.parse("2026-04-02T08:30:00-03:00"), 2, 2)
        ));

        UtilizacaoColetoresOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 30),
                        Map.of("filiais", List.of("SPO"))
                )
        );

        assertThat(overview.manifestosBipados()).isZero();
        assertThat(overview.manifestosEmitidos()).isEqualTo(1);
        assertThat(overview.manifestosDescarregamento()).isZero();
        assertThat(overview.totalManifestos()).isEqualTo(1);
        assertThat(overview.manifestosIncompletos()).isZero();
        assertThat(overview.pctUtilizacao()).isZero();
    }

    @Test
    void buscarOverviewDeveContarManifestosDistintosQuandoIdentificadorUnicoEhReutilizado() {
        when(dimFilialRepository.findAll()).thenReturn(List.of(filial("SPO")));
        when(manifestosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                manifesto(10L, "seq-1", OffsetDateTime.parse("2026-04-02T07:00:00-03:00"), "SPO", null, "encerrado", "DISTRIBUIÇÃO", null, 3, 2),
                manifesto(11L, "seq-1", OffsetDateTime.parse("2026-04-03T07:00:00-03:00"), "SPO", null, "encerrado", "DISTRIBUIÇÃO", OffsetDateTime.parse("2026-04-03T08:30:00-03:00"), 4, 4)
        ));

        UtilizacaoColetoresOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(overview.manifestosEmitidos()).isEqualTo(2);
        assertThat(overview.manifestosBipados()).isEqualTo(2);
        assertThat(overview.manifestosIncompletos()).isEqualTo(1);
        assertThat(overview.totalManifestos()).isEqualTo(2);
    }

    @Test
    void buscarOverviewDeveIgnorarDescarregamentosSemCorrespondenciaNaDimensaoEONullLiteral() {
        when(dimFilialRepository.findAll()).thenReturn(List.of(filial("SPO")));
        when(manifestosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                manifesto(10L, "SPO", "null, PARCEIRO X", "encerrado", "DISTRIBUIÇÃO", OffsetDateTime.parse("2026-04-02T08:30:00-03:00"), 3, 3)
        ));

        FiltroConsultaDTO filtro = new FiltroConsultaDTO(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                Map.of()
        );

        UtilizacaoColetoresOverviewDTO overview = service.buscarOverview(filtro);

        assertThat(overview.manifestosEmitidos()).isEqualTo(1);
        assertThat(overview.manifestosDescarregamento()).isZero();
        assertThat(overview.totalManifestos()).isEqualTo(1);
        assertThat(service.buscarSerie(filtro))
                .extracting("filial")
                .containsExactly("SPO");
    }

    @Test
    void buscarSerieDeveSegregarEClassificarDistribuicaoETransferencia() {
        when(dimFilialRepository.findAll()).thenReturn(List.of(filial("SPO"), filial("REC")));
        when(manifestosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                manifesto(10L, "SPO", "REC", "encerrado", "distribuicao", OffsetDateTime.parse("2026-04-02T08:30:00-03:00"), 1, 1),
                manifesto(11L, "SPO", "REC", "encerrado", "TRANSFERÊNCIA", OffsetDateTime.parse("2026-04-02T08:30:00-03:00"), 1, 1)
        ));

        FiltroConsultaDTO filtro = new FiltroConsultaDTO(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                Map.of("classificacoes", List.of("DISTRIBUIÇÃO"))
        );

        assertThat(service.buscarSerie(filtro))
                .extracting("classificacao")
                .containsOnly("DISTRIBUIÇÃO");

        assertThat(service.buscarTabela(filtro, 100))
                .extracting("classificacao")
                .containsOnly("DISTRIBUIÇÃO");
    }

    @Test
    void buscarSerieDeveManterManifestosEmDatasSeparadasQuandoIdentificadorUnicoEhReutilizado() {
        when(dimFilialRepository.findAll()).thenReturn(List.of(filial("SPO")));
        when(manifestosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                manifesto(10L, "seq-1", OffsetDateTime.parse("2026-04-02T07:00:00-03:00"), "SPO", null, "encerrado", "DISTRIBUIÇÃO", null, 3, 2),
                manifesto(11L, "seq-1", OffsetDateTime.parse("2026-04-03T07:00:00-03:00"), "SPO", null, "encerrado", "DISTRIBUIÇÃO", OffsetDateTime.parse("2026-04-03T08:30:00-03:00"), 4, 4)
        ));

        assertThat(service.buscarSerie(new FiltroConsultaDTO(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                Map.of()
        )))
                .extracting("date", "manifestosEmitidos", "manifestosBipados", "manifestosIncompletos")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("2026-04-02", 1, 1, 1),
                        org.assertj.core.groups.Tuple.tuple("2026-04-03", 1, 1, 0)
                );
    }

    private static VisaoManifestosEntity manifesto(
            Long numero,
            String filialEmissora,
            String localDescarregamento,
            String status,
            String classificacao,
            OffsetDateTime leituraMovelEm,
            Integer itensTotal,
            Integer itensFinalizados
    ) {
        return manifesto(
                numero,
                "uid-" + numero,
                OffsetDateTime.parse("2026-04-02T07:00:00-03:00"),
                filialEmissora,
                localDescarregamento,
                status,
                classificacao,
                leituraMovelEm,
                itensTotal,
                itensFinalizados
        );
    }

    private static VisaoManifestosEntity manifesto(
            Long numero,
            String identificadorUnico,
            OffsetDateTime dataCriacao,
            String filialEmissora,
            String localDescarregamento,
            String status,
            String classificacao,
            OffsetDateTime leituraMovelEm,
            Integer itensTotal,
            Integer itensFinalizados
    ) {
        VisaoManifestosEntity entity = TestReflectionUtils.novaInstancia(VisaoManifestosEntity.class);
        TestReflectionUtils.setField(entity, "id", manifestoId(numero, identificadorUnico));
        TestReflectionUtils.setField(entity, "filialEmissora", filialEmissora);
        TestReflectionUtils.setField(entity, "filial", filialEmissora);
        TestReflectionUtils.setField(entity, "localDescarregamento", localDescarregamento);
        TestReflectionUtils.setField(entity, "status", status);
        TestReflectionUtils.setField(entity, "classificacao", classificacao);
        TestReflectionUtils.setField(entity, "leituraMovelEm", leituraMovelEm);
        TestReflectionUtils.setField(entity, "itensTotal", itensTotal);
        TestReflectionUtils.setField(entity, "itensFinalizados", itensFinalizados);
        TestReflectionUtils.setField(entity, "dataCriacao", dataCriacao);
        TestReflectionUtils.setField(entity, "dataExtracao", LocalDateTime.of(2026, 4, 3, 9, 0));
        return entity;
    }

    private static DimFilialEntity filial(String nome) {
        DimFilialEntity entity = TestReflectionUtils.novaInstancia(DimFilialEntity.class);
        TestReflectionUtils.setField(entity, "nomeFilial", nome);
        return entity;
    }

    private static VisaoManifestosId manifestoId(Long numero, String identificadorUnico) {
        try {
            Constructor<VisaoManifestosId> constructor = VisaoManifestosId.class.getDeclaredConstructor(Long.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(numero, identificadorUnico);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Nao foi possivel instanciar VisaoManifestosId", ex);
        }
    }

    private static EscopoFilialService escopoSemRestricao() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }
}
