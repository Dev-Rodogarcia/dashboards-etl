package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.faturascliente.FaturaPorClienteResumoDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteOverviewDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteStatusProcessoDTO;
import com.dashboard.api.model.VisaoFaturasClienteEntity;
import com.dashboard.api.repository.VisaoFaturasClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaturasPorClienteServiceTest {

    @Mock
    private VisaoFaturasClienteRepository repository;

    private FaturasPorClienteService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-23T12:00:00Z"), ZoneOffset.UTC);
        service = new FaturasPorClienteService(new ValidadorPeriodoService(), repository, clock);
    }

    @Test
    void buscarOverviewDeveNormalizarLinhasDuplicadasPorIdUnico() {
        when(repository.findPowerBiRowsByDataEmissaoCteNaJanela(any(), any())).thenReturn(List.of(
                linha("uid-1", "DOC-1", "100.00", null, null, "Cliente A", "Filial 1",
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), null,
                        LocalDateTime.of(2026, 3, 21, 10, 0)),
                linha("uid-1", "DOC-1", "100.00", null, null, "Cliente A", "Filial 1",
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), null,
                        LocalDateTime.of(2026, 3, 23, 10, 0)),
                linha("uid-2", null, null, null, "80.00", "Cliente B", "Filial 1",
                        null, null, null, LocalDateTime.of(2026, 3, 22, 9, 0))
        ));

        FaturasPorClienteOverviewDTO overview = service.buscarOverview(filtroPadrao());

        assertThat(overview.valorFaturado()).isEqualByComparingTo("100.00");
        assertThat(overview.registrosFaturados()).isEqualTo(1);
        assertThat(overview.aguardandoFaturamento()).isEqualTo(1);
        assertThat(overview.clientesAtivos()).isEqualTo(2);
    }

    @Test
    void buscarOverviewDeveContarTitulosEmAtrasoApenasComDocumentoEVencidosSemBaixa() {
        when(repository.findPowerBiRowsByDataEmissaoCteNaJanela(any(), any())).thenReturn(List.of(
                linha("uid-1", "DOC-1", "100.00", null, null, "Cliente A", "Filial 1",
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), null,
                        LocalDateTime.of(2026, 3, 23, 8, 0)),
                linha("uid-2", "DOC-2", "120.00", null, null, "Cliente B", "Filial 1",
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 25), null,
                        LocalDateTime.of(2026, 3, 23, 8, 0)),
                linha("uid-3", "DOC-3", "90.00", null, null, "Cliente C", "Filial 1",
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 11),
                        LocalDateTime.of(2026, 3, 23, 8, 0)),
                linha("uid-4", null, null, null, "70.00", "Cliente D", "Filial 1",
                        null, LocalDate.of(2026, 3, 5), null,
                        LocalDateTime.of(2026, 3, 23, 8, 0))
        ));

        FaturasPorClienteOverviewDTO overview = service.buscarOverview(filtroPadrao());

        assertThat(overview.titulosEmAtraso()).isEqualTo(1);
    }

    @Test
    void buscarStatusProcessoDeveSepararFaturadoEAguardando() {
        when(repository.findPowerBiRowsByDataEmissaoCteNaJanela(any(), any())).thenReturn(List.of(
                linha("uid-1", "DOC-1", "100.00", null, null, "Cliente A", "Filial 1",
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), null,
                        LocalDateTime.of(2026, 3, 23, 8, 0)),
                linha("uid-2", null, null, null, "80.00", "Cliente B", "Filial 1",
                        null, null, null, LocalDateTime.of(2026, 3, 23, 8, 0)),
                linha("uid-3", null, null, null, "70.00", "Cliente C", "Filial 1",
                        null, null, null, LocalDateTime.of(2026, 3, 23, 8, 0))
        ));

        List<FaturasPorClienteStatusProcessoDTO> status = service.buscarStatusProcesso(filtroPadrao());

        assertThat(status).containsExactlyInAnyOrder(
                new FaturasPorClienteStatusProcessoDTO("Aguardando Faturamento", 2),
                new FaturasPorClienteStatusProcessoDTO("Faturado", 1)
        );
    }

    @Test
    void buscarTabelaDeveUsarIdUnicoComoChaveDaLinha() {
        when(repository.findPowerBiRowsByDataEmissaoCteNaJanela(any(), any())).thenReturn(List.of(
                linha("uid-1", "DOC-1", "100.00", null, null, "Cliente A", "Filial 1",
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), null,
                        LocalDateTime.of(2026, 3, 23, 8, 0))
        ));

        List<FaturaPorClienteResumoDTO> tabela = service.buscarTabela(filtroPadrao(), 10);

        assertThat(tabela).hasSize(1);
        assertThat(tabela.get(0).idUnico()).isEqualTo("uid-1");
        assertThat(tabela.get(0).documentoFatura()).isEqualTo("DOC-1");
        assertThat(tabela.get(0).statusProcesso()).isEqualTo("Faturado");
    }

    @Test
    void buscarOverviewDeveConsultarPeriodoNoFusoDeSaoPaulo() {
        when(repository.findPowerBiRowsByDataEmissaoCteNaJanela(any(), any())).thenReturn(List.of());

        service.buscarOverview(filtroPadrao());

        ArgumentCaptor<OffsetDateTime> inicio = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> fim = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).findPowerBiRowsByDataEmissaoCteNaJanela(inicio.capture(), fim.capture());

        assertThat(inicio.getValue())
                .isEqualTo(OffsetDateTime.of(2026, 2, 21, 0, 0, 0, 0, ZoneOffset.ofHours(-3)));
        assertThat(fim.getValue())
                .isEqualTo(OffsetDateTime.of(2026, 3, 24, 0, 0, 0, 0, ZoneOffset.ofHours(-3)));
    }

    private static FiltroConsultaDTO filtroPadrao() {
        return new FiltroConsultaDTO(LocalDate.of(2026, 2, 21), LocalDate.of(2026, 3, 23), Map.of());
    }

    private static VisaoFaturasClienteEntity linha(
            String uniqueId,
            String documentoFatura,
            String valorFitAnt,
            String valorFatura,
            String valorFrete,
            String pagadorNome,
            String filial,
            LocalDate emissaoFatura,
            LocalDate vencimento,
            LocalDate baixa,
            LocalDateTime dataExtracao
    ) {
        VisaoFaturasClienteEntity entity = Objects.requireNonNull(novaInstancia(VisaoFaturasClienteEntity.class));
        ReflectionTestUtils.setField(entity, "uniqueId", uniqueId);
        ReflectionTestUtils.setField(entity, "documentoFatura", documentoFatura);
        ReflectionTestUtils.setField(entity, "valorFitAnt", valorFitAnt != null ? new BigDecimal(valorFitAnt) : null);
        ReflectionTestUtils.setField(entity, "valorFatura", valorFatura != null ? new BigDecimal(valorFatura) : null);
        ReflectionTestUtils.setField(entity, "valorFrete", valorFrete != null ? new BigDecimal(valorFrete) : null);
        ReflectionTestUtils.setField(entity, "pagadorNome", pagadorNome);
        ReflectionTestUtils.setField(entity, "filial", filial);
        ReflectionTestUtils.setField(entity, "emissaoFatura", emissaoFatura);
        ReflectionTestUtils.setField(entity, "dataVencimentoFatura", vencimento);
        ReflectionTestUtils.setField(entity, "dataBaixaFatura", baixa);
        ReflectionTestUtils.setField(entity, "dataExtracao", dataExtracao);
        ReflectionTestUtils.setField(entity, "dataEmissaoCte", OffsetDateTime.of(2026, 3, 5, 10, 0, 0, 0, ZoneOffset.UTC));
        ReflectionTestUtils.setField(entity, "numeroCte", 12345L);
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
}
