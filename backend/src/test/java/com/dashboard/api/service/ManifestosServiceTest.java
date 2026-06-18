package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.manifestos.ManifestoResumoDTO;
import com.dashboard.api.dto.manifestos.ManifestosOverviewDTO;
import com.dashboard.api.model.VisaoManifestosEntity;
import com.dashboard.api.model.VisaoManifestosId;
import com.dashboard.api.repository.VisaoManifestosRepository;
import jakarta.persistence.EmbeddedId;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManifestosServiceTest {

    @Mock
    private VisaoManifestosRepository repository;

    private ManifestosService service;

    @BeforeEach
    void setUp() {
        service = new ManifestosService(new ValidadorPeriodoService(), repository);
    }

    @Test
    void buscarOverviewDeveUsarAgregadoSqlPreservandoLinhasComMesmoNumero() {
        when(repository.buscarOverviewAgregado(
                any(), any(), anyList(), anyInt(), anyList(), anyInt(), anyList(), anyInt(),
                anyList(), anyInt(), anyList(), anyInt(), anyList(), anyInt(), anyList(), anyInt(),
                anyList(), anyInt()
        )).thenReturn(overview());

        ManifestosOverviewDTO overview = service.buscarOverview(filtroPadrao());

        assertThat(overview.totalManifestos()).isEqualTo(3);
        assertThat(overview.emTransito()).isEqualTo(1);
        assertThat(overview.encerrados()).isEqualTo(2);
        assertThat(overview.kmTotal()).isEqualByComparingTo("350.00");
        assertThat(overview.custoTotal()).isEqualByComparingTo("1750.00");
        assertThat(overview.custoPorKm()).isEqualByComparingTo("5.00");
        assertThat(overview.ocupacaoPesoMediaPct()).isEqualTo(200.0);
        assertThat(overview.ocupacaoCubagemMediaPct()).isEqualTo(116.67);
    }

    @Test
    void buscarTabelaDevePreservarIdentificadoresUnicosParaLinhasComMesmoNumero() {
        when(repository.buscarTabelaPaginada(
                any(), any(), anyList(), anyInt(), anyList(), anyInt(), anyList(), anyInt(),
                anyList(), anyInt(), anyList(), anyInt(), anyList(), anyInt(), anyList(), anyInt(),
                anyList(), anyInt(), anyInt()
        )).thenReturn(List.of(
                resumo(62848L, "62848_MDFE_4380"),
                resumo(62848L, "62848_MDFE_4381")
        ));

        List<ManifestoResumoDTO> tabela = service.buscarTabela(filtroPadrao(), 10);

        assertThat(tabela).hasSize(2);
        assertThat(tabela).extracting(ManifestoResumoDTO::numero).containsExactly(62848L, 62848L);
        assertThat(tabela).extracting(ManifestoResumoDTO::identificadorUnico)
                .containsExactly("62848_MDFE_4380", "62848_MDFE_4381");
        assertThat(tabela).extracting(ManifestoResumoDTO::receitaTotalTransportada)
                .allSatisfy(valor -> assertThat(valor).isEqualByComparingTo("1000.00"));
        assertThat(tabela).extracting(ManifestoResumoDTO::capacidadeKg)
                .allSatisfy(valor -> assertThat(valor).isEqualByComparingTo("12000.00"));
        assertThat(tabela).extracting(ManifestoResumoDTO::itensFinalizados).containsOnly(8);
        assertThat(tabela).extracting(ManifestoResumoDTO::itensTotal).containsOnly(10);
    }

    @Test
    void entidadeDeveUsarChaveCompostaComNumeroEIdentificadorUnico() {
        VisaoManifestosEntity entity = Objects.requireNonNull(novaInstancia(VisaoManifestosEntity.class));
        ReflectionTestUtils.setField(entity, "id", new VisaoManifestosId(62848L, "62848_MDFE_4380"));

        assertThat(idEhEmbedded()).isTrue();
        assertThat(entity.getId()).isEqualTo(new VisaoManifestosId(62848L, "62848_MDFE_4380"));
        assertThat(entity.getNumero()).isEqualTo(62848L);
        assertThat(entity.getIdentificadorUnico()).isEqualTo("62848_MDFE_4380");
    }

    private static FiltroConsultaDTO filtroPadrao() {
        return new FiltroConsultaDTO(LocalDate.of(2026, 2, 21), LocalDate.of(2026, 3, 23), Map.of());
    }

    private static VisaoManifestosRepository.ManifestosOverviewProjection overview() {
        return new VisaoManifestosRepository.ManifestosOverviewProjection() {
            @Override public LocalDateTime getUpdatedAt() { return LocalDateTime.of(2026, 3, 23, 12, 0); }
            @Override public int getTotalManifestos() { return 3; }
            @Override public int getEmTransito() { return 1; }
            @Override public int getEncerrados() { return 2; }
            @Override public BigDecimal getKmTotal() { return new BigDecimal("350.00"); }
            @Override public BigDecimal getCustoTotal() { return new BigDecimal("1750.00"); }
            @Override public BigDecimal getOcupacaoPesoMediaPct() { return new BigDecimal("200.00"); }
            @Override public BigDecimal getOcupacaoCubagemMediaPct() { return new BigDecimal("116.67"); }
        };
    }

    private static VisaoManifestosRepository.ManifestoResumoProjection resumo(Long numero, String identificadorUnico) {
        return new VisaoManifestosRepository.ManifestoResumoProjection() {
            @Override public Long getNumero() { return numero; }
            @Override public String getIdentificadorUnico() { return identificadorUnico; }
            @Override public String getStatus() { return "em trânsito"; }
            @Override public String getClassificacao() { return null; }
            @Override public String getFilial() { return "SPO"; }
            @Override public String getDataCriacao() { return "2026-03-23T10:00:00Z"; }
            @Override public String getFechamento() { return null; }
            @Override public String getMotorista() { return "Motorista"; }
            @Override public String getVeiculoPlaca() { return "ABC1D23"; }
            @Override public String getTipoVeiculo() { return "Truck"; }
            @Override public BigDecimal getTotalPesoTaxado() { return BigDecimal.ZERO; }
            @Override public BigDecimal getTotalM3() { return BigDecimal.ZERO; }
            @Override public BigDecimal getCustoTotal() { return BigDecimal.ZERO; }
            @Override public BigDecimal getValorFrete() { return BigDecimal.ZERO; }
            @Override public BigDecimal getCombustivel() { return BigDecimal.ZERO; }
            @Override public BigDecimal getPedagio() { return BigDecimal.ZERO; }
            @Override public BigDecimal getSaldoPagar() { return BigDecimal.ZERO; }
            @Override public BigDecimal getKmTotal() { return BigDecimal.ZERO; }
            @Override public BigDecimal getReceitaTotalTransportada() { return new BigDecimal("1000.00"); }
            @Override public BigDecimal getCapacidadeKg() { return new BigDecimal("12000.00"); }
            @Override public Integer getItensFinalizados() { return 8; }
            @Override public Integer getItensTotal() { return 10; }
        };
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

    private static boolean idEhEmbedded() {
        try {
            return VisaoManifestosEntity.class.getDeclaredField("id").isAnnotationPresent(EmbeddedId.class);
        } catch (NoSuchFieldException ex) {
            throw new IllegalStateException("Campo id nao encontrado em VisaoManifestosEntity", ex);
        }
    }
}
