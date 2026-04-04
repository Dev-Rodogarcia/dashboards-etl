package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasOverviewDTO;
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
    void buscarOverviewDeveDeduplicarSinistrosECalcularPercentualSobreFaturamento() {
        when(fretesRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                frete(9001L, "SPO", new BigDecimal("10000.00"), LocalDateTime.of(2026, 4, 3, 9, 0)),
                frete(9002L, "SPO", new BigDecimal("5000.00"), LocalDateTime.of(2026, 4, 3, 9, 0))
        ));
        when(sinistrosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                sinistro(701L, 9001L, new BigDecimal("-100.00"), LocalDateTime.of(2026, 4, 3, 10, 0)),
                sinistro(701L, 9001L, new BigDecimal("-100.00"), LocalDateTime.of(2026, 4, 3, 11, 0)),
                sinistro(702L, 9002L, new BigDecimal("-50.00"), LocalDateTime.of(2026, 4, 3, 10, 0))
        ));

        IndenizacaoMercadoriasOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of("filiais", List.of("SPO")))
        );

        assertThat(overview.totalSinistros()).isEqualTo(2);
        assertThat(overview.valorIndenizadoAbs()).isEqualByComparingTo("150.00");
        assertThat(overview.valorIndenizadoOriginal()).isEqualByComparingTo("-150.00");
        assertThat(overview.faturamentoBase()).isEqualByComparingTo("15000.00");
        assertThat(overview.pctIndenizacao()).isEqualTo(1.0);
    }

    private static VisaoFretesEntity frete(Long numeroMinuta, String filialEmissora, BigDecimal valorTotal, LocalDateTime dataExtracao) {
        VisaoFretesEntity entity = TestReflectionUtils.novaInstancia(VisaoFretesEntity.class);
        TestReflectionUtils.setField(entity, "id", numeroMinuta);
        TestReflectionUtils.setField(entity, "numeroMinuta", numeroMinuta);
        TestReflectionUtils.setField(entity, "filialEmissora", filialEmissora);
        TestReflectionUtils.setField(entity, "valorTotal", valorTotal);
        TestReflectionUtils.setField(entity, "dataFrete", OffsetDateTime.parse("2026-04-02T10:00:00-03:00"));
        TestReflectionUtils.setField(entity, "dataExtracao", dataExtracao);
        return entity;
    }

    private static VisaoSinistrosEntity sinistro(Long numeroSinistro, Long minuta, BigDecimal resultadoFinal, LocalDateTime dataExtracao) {
        VisaoSinistrosEntity entity = TestReflectionUtils.novaInstancia(VisaoSinistrosEntity.class);
        TestReflectionUtils.setField(entity, "identificadorUnico", "sin-" + numeroSinistro + "-" + dataExtracao);
        TestReflectionUtils.setField(entity, "numeroSinistro", numeroSinistro);
        TestReflectionUtils.setField(entity, "minuta", minuta);
        TestReflectionUtils.setField(entity, "resultadoFinal", resultadoFinal);
        TestReflectionUtils.setField(entity, "dataAbertura", LocalDate.of(2026, 4, 2));
        TestReflectionUtils.setField(entity, "pessoaNomeFantasia", "SPO");
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
