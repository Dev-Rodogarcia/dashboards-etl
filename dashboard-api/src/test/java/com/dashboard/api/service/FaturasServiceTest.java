package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.faturas.FaturaResumoDTO;
import com.dashboard.api.dto.faturas.FaturasOverviewDTO;
import com.dashboard.api.model.VisaoFaturasClienteEntity;
import com.dashboard.api.model.VisaoFaturasGraphqlEntity;
import com.dashboard.api.repository.VisaoFaturasClienteRepository;
import com.dashboard.api.repository.VisaoFaturasGraphqlRepository;
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
class FaturasServiceTest {

    @Mock
    private VisaoFaturasGraphqlRepository graphqlRepository;

    @Mock
    private VisaoFaturasClienteRepository clienteRepository;

    private FaturasService service;

    @BeforeEach
    void setUp() {
        service = new FaturasService(new ValidadorPeriodoService(), graphqlRepository, clienteRepository);
    }

    @Test
    void buscarDeveRetornarVazioFinanceiroQuandoNaoHaTitulosMesmoComOperacional() {
        when(graphqlRepository.findByEmissaoBetween(any(), any())).thenReturn(List.of());
        when(clienteRepository.findByDataEmissaoCteBetween(any(), any())).thenReturn(List.of(
                operacional("uid-1", "DOC-OPER-1", "Cliente A", "Filial SP", "120.00")
        ));

        FaturasOverviewDTO overview = service.buscarOverview(filtroPadrao());

        assertThat(overview.valorFaturado()).isEqualByComparingTo("0.00");
        assertThat(overview.valorRecebido()).isEqualByComparingTo("0.00");
        assertThat(overview.saldoAberto()).isEqualByComparingTo("0.00");
        assertThat(overview.titulosEmAtraso()).isZero();
        assertThat(overview.clientesAtivos()).isZero();
        assertThat(overview.hasFinancialData()).isFalse();

        assertThat(service.buscarMensal(filtroPadrao())).isEmpty();
        assertThat(service.buscarAging(filtroPadrao())).isEmpty();
        assertThat(service.buscarTopClientes(filtroPadrao(), 10)).isEmpty();
        assertThat(service.buscarStatusProcesso(filtroPadrao())).isEmpty();
        assertThat(service.buscarReconciliacao(filtroPadrao(), 10)).isEmpty();
        assertThat(service.buscarTabela(filtroPadrao(), 10)).isEmpty();
    }

    @Test
    void buscarDeveUsarFonteFinanceiraQuandoHaTitulos() {
        when(graphqlRepository.findByEmissaoBetween(any(), any())).thenReturn(List.of(
                titulo(1L, "DOC-1", "Filial SP", "Pago", "Liquidado", "100.00", "100.00", "0.00",
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), LocalDateTime.of(2026, 3, 23, 8, 0)),
                titulo(2L, "DOC-2", "Filial SP", "Nao Pago", "Aberto", "150.00", "50.00", "100.00",
                        LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 15), LocalDateTime.of(2026, 3, 23, 9, 0))
        ));
        when(clienteRepository.findByDataEmissaoCteBetween(any(), any())).thenReturn(List.of(
                operacional("uid-1", "DOC-1", "Cliente A", "Filial SP", "100.00"),
                operacional("uid-2", "DOC-2", "Cliente B", "Filial SP", "150.00")
        ));

        FaturasOverviewDTO overview = service.buscarOverview(filtroPadrao());

        assertThat(overview.valorFaturado()).isEqualByComparingTo("250.00");
        assertThat(overview.valorRecebido()).isEqualByComparingTo("150.00");
        assertThat(overview.saldoAberto()).isEqualByComparingTo("100.00");
        assertThat(overview.taxaAdimplencia()).isEqualTo(50.0);
        assertThat(overview.dsoMedioDias()).isEqualTo(9.5);
        assertThat(overview.titulosEmAtraso()).isEqualTo(1);
        assertThat(overview.clientesAtivos()).isEqualTo(2);
        assertThat(overview.hasFinancialData()).isTrue();

        assertThat(service.buscarMensal(filtroPadrao())).hasSize(1);
        assertThat(service.buscarAging(filtroPadrao()))
                .filteredOn(bucket -> bucket.titulos() > 0)
                .singleElement()
                .satisfies(bucket -> {
                    assertThat(bucket.titulos()).isEqualTo(1);
                    assertThat(bucket.valor()).isEqualByComparingTo("100.00");
                });
        assertThat(service.buscarTopClientes(filtroPadrao(), 10)).hasSize(2);
        assertThat(service.buscarStatusProcesso(filtroPadrao()))
                .singleElement()
                .satisfies(status -> {
                    assertThat(status.statusProcesso()).isEqualTo("Faturado");
                    assertThat(status.total()).isEqualTo(2);
                });
        assertThat(service.buscarReconciliacao(filtroPadrao(), 10)).hasSize(2);

        List<FaturaResumoDTO> tabela = service.buscarTabela(filtroPadrao(), 10);
        assertThat(tabela).hasSize(2);
        assertThat(tabela).extracting(FaturaResumoDTO::documento).containsExactly("DOC-1", "DOC-2");
    }

    private static FiltroConsultaDTO filtroPadrao() {
        return new FiltroConsultaDTO(LocalDate.of(2026, 2, 21), LocalDate.of(2026, 3, 23), Map.of());
    }

    private static VisaoFaturasGraphqlEntity titulo(
            Long id,
            String documento,
            String filial,
            String pago,
            String status,
            String valor,
            String valorPago,
            String valorAPagar,
            LocalDate emissao,
            LocalDate vencimento,
            LocalDateTime dataExtracao
    ) {
        VisaoFaturasGraphqlEntity entity = Objects.requireNonNull(novaInstancia(VisaoFaturasGraphqlEntity.class));
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "documento", documento);
        ReflectionTestUtils.setField(entity, "filialNome", filial);
        ReflectionTestUtils.setField(entity, "pago", pago);
        ReflectionTestUtils.setField(entity, "status", status);
        ReflectionTestUtils.setField(entity, "valor", new BigDecimal(valor));
        ReflectionTestUtils.setField(entity, "valorPago", new BigDecimal(valorPago));
        ReflectionTestUtils.setField(entity, "valorAPagar", new BigDecimal(valorAPagar));
        ReflectionTestUtils.setField(entity, "emissao", emissao);
        ReflectionTestUtils.setField(entity, "vencimento", vencimento);
        ReflectionTestUtils.setField(entity, "dataExtracao", dataExtracao);
        return entity;
    }

    private static VisaoFaturasClienteEntity operacional(
            String uniqueId,
            String documentoFatura,
            String pagadorNome,
            String filial,
            String valorFitAnt
    ) {
        VisaoFaturasClienteEntity entity = Objects.requireNonNull(novaInstancia(VisaoFaturasClienteEntity.class));
        ReflectionTestUtils.setField(entity, "uniqueId", uniqueId);
        ReflectionTestUtils.setField(entity, "documentoFatura", documentoFatura);
        ReflectionTestUtils.setField(entity, "pagadorNome", pagadorNome);
        ReflectionTestUtils.setField(entity, "filial", filial);
        ReflectionTestUtils.setField(entity, "valorFitAnt", new BigDecimal(valorFitAnt));
        ReflectionTestUtils.setField(entity, "emissaoFatura", LocalDate.of(2026, 3, 1));
        ReflectionTestUtils.setField(entity, "dataEmissaoCte", OffsetDateTime.of(2026, 3, 5, 10, 0, 0, 0, ZoneOffset.UTC));
        ReflectionTestUtils.setField(entity, "dataExtracao", LocalDateTime.of(2026, 3, 23, 10, 0));
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
