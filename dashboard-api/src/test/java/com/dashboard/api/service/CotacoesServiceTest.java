package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.cotacoes.CotacoesChartsDTO;
import com.dashboard.api.dto.cotacoes.CotacoesOverviewDTO;
import com.dashboard.api.model.VisaoCotacoesEntity;
import com.dashboard.api.repository.VisaoCotacoesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CotacoesServiceTest {

    @Mock
    private VisaoCotacoesRepository repository;

    private CotacoesService service;

    @BeforeEach
    void setUp() {
        service = new CotacoesService(new ValidadorPeriodoService(), repository);
    }

    @Test
    void buscarGraficosDeveRetornarMotivosVaziosQuandoNaoHaMotivoPerdaNemReprovacao() {
        when(repository.findByDataCotacaoBetween(any(), any())).thenReturn(List.of(
                cotacao(1L, "Convertida", null, "SP > RJ", "100.00"),
                cotacao(2L, "Convertida", "", "SP > RJ", "200.00")
        ));

        CotacoesOverviewDTO overview = service.buscarOverview(filtroPadrao());
        CotacoesChartsDTO graficos = service.buscarGraficos(filtroPadrao());

        assertThat(overview.totalCotacoes()).isEqualTo(2);
        assertThat(overview.taxaReprovacao()).isEqualTo(0.0);
        assertThat(graficos.motivosPerda()).isEmpty();
        assertThat(graficos.funil()).extracting(item -> item.etapa(), item -> item.total())
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Convertida", 2));
        assertThat(graficos.corredoresMaisValiosos()).singleElement().satisfies(corredor -> {
            assertThat(corredor.trecho()).isEqualTo("SP > RJ");
            assertThat(corredor.valorFrete()).isEqualByComparingTo("300.00");
            assertThat(corredor.cotacoes()).isEqualTo(2);
        });
    }

    private static FiltroConsultaDTO filtroPadrao() {
        return new FiltroConsultaDTO(LocalDate.of(2026, 2, 21), LocalDate.of(2026, 3, 23), Map.of());
    }

    private static VisaoCotacoesEntity cotacao(
            Long sequenceCode,
            String statusConversao,
            String motivoPerda,
            String trecho,
            String valorFrete
    ) {
        VisaoCotacoesEntity entity = Objects.requireNonNull(novaInstancia(VisaoCotacoesEntity.class));
        ReflectionTestUtils.setField(entity, "sequenceCode", sequenceCode);
        ReflectionTestUtils.setField(entity, "statusConversao", statusConversao);
        ReflectionTestUtils.setField(entity, "motivoPerda", motivoPerda);
        ReflectionTestUtils.setField(entity, "trecho", trecho);
        ReflectionTestUtils.setField(entity, "valorFrete", new BigDecimal(valorFrete));
        ReflectionTestUtils.setField(entity, "pesoTaxado", new BigDecimal("10.00"));
        ReflectionTestUtils.setField(entity, "dataCotacao", OffsetDateTime.of(2026, 3, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        ReflectionTestUtils.setField(entity, "cteEmissao", OffsetDateTime.of(2026, 3, 20, 12, 0, 0, 0, ZoneOffset.UTC));
        ReflectionTestUtils.setField(entity, "dataExtracao", LocalDateTime.of(2026, 3, 23, 9, 0));
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
