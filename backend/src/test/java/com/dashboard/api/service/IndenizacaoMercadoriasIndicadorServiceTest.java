package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasRowDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasSeriePointDTO;
import com.dashboard.api.model.VisaoFretesEntity;
import com.dashboard.api.model.VisaoSinistrosEntity;
import com.dashboard.api.repository.VisaoFretesRepository;
import com.dashboard.api.repository.VisaoSinistrosRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndenizacaoMercadoriasIndicadorServiceTest {

    @Mock
    private VisaoSinistrosRepository sinistrosRepository;

    @Mock
    private VisaoFretesRepository fretesRepository;

    private IndenizacaoMercadoriasIndicadorService service;

    @BeforeEach
    void setUp() {
        service = new IndenizacaoMercadoriasIndicadorService(
                new ValidadorPeriodoService(),
                sinistrosRepository,
                fretesRepository,
                escopoSemRestricao(),
                PeriodoOffsetDateTimeHelper.padrao()
        );
    }

    @Test
    void buscarOverviewDeveDeduplicarPorSinistroEUsarValorAPagarAoClientePorDataAbertura() {
        VisaoFretesEntity cortesia = frete(9003L, "SPO", new BigDecimal("9999.99"), "EMITIDO", OffsetDateTime.parse("2026-04-06T10:00:00-03:00"), LocalDateTime.of(2026, 4, 6, 9, 0));
        TestReflectionUtils.setField(cortesia, "cortesiaFlag", true);

        VisaoFretesEntity pendente = frete(9004L, "SPO", new BigDecimal("7777.77"), "EMITIDO", OffsetDateTime.parse("2026-04-07T10:00:00-03:00"), LocalDateTime.of(2026, 4, 7, 9, 0));
        TestReflectionUtils.setField(pendente, "documentoOficialTipo", "Pendente/Não Emitido");

        VisaoFretesEntity semValor = frete(9005L, "SPO", new BigDecimal("0.01"), "EMITIDO", OffsetDateTime.parse("2026-04-08T10:00:00-03:00"), LocalDateTime.of(2026, 4, 8, 9, 0));

        when(fretesRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                frete(9001L, "SPO", new BigDecimal("10000.00"), "EMITIDO", OffsetDateTime.parse("2026-04-02T10:00:00-03:00"), LocalDateTime.of(2026, 4, 3, 9, 0)),
                frete(9002L, "SPO", new BigDecimal("5000.00"), "cancelado", OffsetDateTime.parse("2026-04-04T10:00:00-03:00"), LocalDateTime.of(2026, 4, 4, 9, 0)),
                cortesia,
                pendente,
                semValor
        ));
        when(sinistrosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                sinistro(701L, 9001L, new BigDecimal("100.00"), "SPO", LocalDate.of(2026, 4, 2), LocalDateTime.of(2026, 4, 3, 10, 0), "Avaria Parcial", "RESPONSAVEL ANTIGO"),
                sinistro(701L, 9001L, new BigDecimal("100.00"), "SPO", LocalDate.of(2026, 4, 2), LocalDateTime.of(2026, 4, 4, 10, 0), "Avaria Parcial", "RESPONSAVEL NOVO"),
                sinistro(702L, 9001L, new BigDecimal("50.00"), "SPO", LocalDate.of(2026, 4, 5), LocalDateTime.of(2026, 4, 5, 10, 0), "Extravio Parcial", "EM ABERTO"),
                sinistro(703L, 9001L, new BigDecimal("200.00"), "SPO", LocalDate.of(2026, 4, 10), LocalDateTime.of(2026, 4, 10, 10, 0), "Avaria Total", "FECHADO"),
                sinistro(704L, 9001L, new BigDecimal("50.00"), "SPO", LocalDate.of(2026, 4, 11), LocalDateTime.of(2026, 4, 11, 10, 0), "Crédito", "AJUSTE")
        ));

        IndenizacaoMercadoriasOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of("filiais", List.of("SPO")))
        );

        assertThat(overview.totalSinistros()).isEqualTo(4);
        assertThat(overview.valorIndenizadoOriginal()).isEqualByComparingTo("400.00");
        assertThat(overview.valorIndenizadoAbs()).isEqualByComparingTo("400.00");
        assertThat(overview.faturamentoBase()).isEqualByComparingTo("22777.77");
        assertThat(overview.pctIndenizacao()).isEqualTo(1.756);
    }

    @Test
    void buscarOverviewDeveUsarPessoaNomeFantasiaComoFilialDeCusto() {
        when(fretesRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                frete(9001L, "CWB", new BigDecimal("10000.00"), "EMITIDO", OffsetDateTime.parse("2026-04-02T10:00:00-03:00"), LocalDateTime.of(2026, 4, 3, 9, 0)),
                frete(9002L, "RJR", new BigDecimal("5000.00"), "EMITIDO", OffsetDateTime.parse("2026-04-05T10:00:00-03:00"), LocalDateTime.of(2026, 4, 5, 9, 0))
        ));
        when(sinistrosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                sinistro(701L, 9001L, new BigDecimal("100.00"), "RJR", LocalDate.of(2026, 4, 2), LocalDateTime.of(2026, 4, 3, 10, 0), "Extravio Total", "FECHADO")
        ));

        FiltroConsultaDTO filtro = new FiltroConsultaDTO(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                Map.of("filiais", List.of("RJR"))
        );

        IndenizacaoMercadoriasOverviewDTO overview = service.buscarOverview(filtro);
        List<IndenizacaoMercadoriasSeriePointDTO> serie = service.buscarSerie(filtro);

        assertThat(overview.totalSinistros()).isEqualTo(1);
        assertThat(overview.valorIndenizadoAbs()).isEqualByComparingTo("100.00");
        assertThat(overview.faturamentoBase()).isEqualByComparingTo("5000.00");
        assertThat(overview.pctIndenizacao()).isEqualTo(2.0);
        assertThat(serie)
                .singleElement()
                .satisfies(point -> {
                    assertThat(point.date()).isEqualTo("2026-04-01");
                    assertThat(point.filial()).isEqualTo("RJR");
                    assertThat(point.faturamentoPeriodoFilial()).isEqualByComparingTo("5000.00");
                });
    }

    @Test
    void buscarSerieDeveAgruparPorMesDeAbertura() {
        when(fretesRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                frete(9001L, "SPO", new BigDecimal("3000.00"), "EMITIDO", OffsetDateTime.parse("2026-04-02T10:00:00-03:00"), LocalDateTime.of(2026, 4, 3, 9, 0)),
                frete(9002L, "SPO", new BigDecimal("2000.00"), "EMITIDO", OffsetDateTime.parse("2026-04-12T10:00:00-03:00"), LocalDateTime.of(2026, 4, 12, 9, 0)),
                frete(9003L, "SPO", new BigDecimal("7000.00"), "EMITIDO", OffsetDateTime.parse("2026-05-07T10:00:00-03:00"), LocalDateTime.of(2026, 5, 7, 9, 0))
        ));
        when(sinistrosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                sinistro(701L, 9001L, new BigDecimal("50.00"), "SPO", LocalDate.of(2026, 4, 2), LocalDateTime.of(2026, 4, 3, 10, 0), "Avaria Parcial", "FECHADO"),
                sinistro(702L, 9002L, new BigDecimal("30.00"), "SPO", LocalDate.of(2026, 4, 20), LocalDateTime.of(2026, 4, 20, 10, 0), "Extravio Parcial", "FECHADO"),
                sinistro(703L, 9003L, new BigDecimal("70.00"), "SPO", LocalDate.of(2026, 5, 8), LocalDateTime.of(2026, 5, 8, 10, 0), "Avaria Total", "FECHADO")
        ));

        List<IndenizacaoMercadoriasSeriePointDTO> serie = service.buscarSerie(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31), Map.of("filiais", List.of("SPO")))
        );

        assertThat(serie).hasSize(2);
        assertThat(serie.get(0).date()).isEqualTo("2026-04-01");
        assertThat(serie.get(0).totalSinistros()).isEqualTo(2);
        assertThat(serie.get(0).valorIndenizadoOriginal()).isEqualByComparingTo("80.00");
        assertThat(serie.get(0).valorIndenizadoAbs()).isEqualByComparingTo("80.00");
        assertThat(serie.get(0).faturamentoBase()).isEqualByComparingTo("5000.00");
        assertThat(serie.get(0).faturamentoPeriodoFilial()).isEqualByComparingTo("12000.00");
        assertThat(serie.get(1).date()).isEqualTo("2026-05-01");
        assertThat(serie.get(1).valorIndenizadoAbs()).isEqualByComparingTo("70.00");
        assertThat(serie.get(1).faturamentoBase()).isEqualByComparingTo("7000.00");
    }

    @Test
    void buscarTabelaDeveExporDataAberturaECausaRaizCorretaNoCampoCompativel() {
        when(fretesRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                frete(9001L, "RJR", new BigDecimal("5000.00"), "EMITIDO", OffsetDateTime.parse("2026-04-15T10:00:00-03:00"), LocalDateTime.of(2026, 4, 15, 9, 0))
        ));
        when(sinistrosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                sinistro(701L, 9001L, new BigDecimal("125.55"), "RJR", LocalDate.of(2026, 4, 15), LocalDateTime.of(2026, 4, 15, 10, 0), "Avaria Parcial", "JOSE CLAUDIO DA SILVA")
        ));

        List<IndenizacaoMercadoriasRowDTO> tabela = service.buscarTabela(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of("filiais", List.of("RJR"))),
                100
        );

        assertThat(tabela)
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.dataFinalizacao()).isEqualTo("2026-04-15T00:00:00");
                    assertThat(row.filial()).isEqualTo("RJR");
                    assertThat(row.causaRaiz()).isEqualTo("Avaria Parcial");
                    assertThat(row.solucao()).isEqualTo("JOSE CLAUDIO DA SILVA");
                });
    }

    private static VisaoFretesEntity frete(
            Long numeroMinuta,
            String filialEmissora,
            BigDecimal valorTotal,
            String status,
            OffsetDateTime dataFrete,
            LocalDateTime dataExtracao
    ) {
        VisaoFretesEntity entity = TestReflectionUtils.novaInstancia(VisaoFretesEntity.class);
        TestReflectionUtils.setField(entity, "id", numeroMinuta);
        TestReflectionUtils.setField(entity, "numeroMinuta", numeroMinuta);
        TestReflectionUtils.setField(entity, "filialEmissora", filialEmissora);
        TestReflectionUtils.setField(entity, "valorTotal", valorTotal);
        TestReflectionUtils.setField(entity, "status", status);
        TestReflectionUtils.setField(entity, "documentoOficialTipo", "CT-e");
        TestReflectionUtils.setField(entity, "cortesiaFlag", false);
        TestReflectionUtils.setField(entity, "dataFrete", dataFrete);
        TestReflectionUtils.setField(entity, "dataExtracao", dataExtracao);
        return entity;
    }

    private static VisaoSinistrosEntity sinistro(
            Long numeroSinistro,
            Long minuta,
            BigDecimal valorAPagarCliente,
            String pessoaNomeFantasia,
            LocalDate dataAbertura,
            LocalDateTime dataExtracao,
            String ocorrenciaDescricao,
            String solucao
    ) {
        VisaoSinistrosEntity entity = TestReflectionUtils.novaInstancia(VisaoSinistrosEntity.class);
        TestReflectionUtils.setField(entity, "identificadorUnico", "sin-" + numeroSinistro + "-" + dataExtracao);
        TestReflectionUtils.setField(entity, "numeroSinistro", numeroSinistro);
        TestReflectionUtils.setField(entity, "minuta", minuta);
        TestReflectionUtils.setField(entity, "valorAPagarCliente", valorAPagarCliente);
        TestReflectionUtils.setField(entity, "resultadoFinal", valorAPagarCliente.negate());
        TestReflectionUtils.setField(entity, "dataAbertura", dataAbertura);
        TestReflectionUtils.setField(entity, "dataFinalizacao", dataAbertura);
        TestReflectionUtils.setField(entity, "pessoaNomeFantasia", pessoaNomeFantasia);
        TestReflectionUtils.setField(entity, "ocorrencia", solucao);
        TestReflectionUtils.setField(entity, "ocorrenciaDescricao", ocorrenciaDescricao);
        TestReflectionUtils.setField(entity, "solucao", solucao);
        TestReflectionUtils.setField(entity, "dataExtracao", dataExtracao);
        return entity;
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
