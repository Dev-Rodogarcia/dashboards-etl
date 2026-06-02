package com.dashboard.api.service;

import com.dashboard.api.dto.indicadoresgestao.KpiGoalEffectiveDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalsUpdateRequestDTO;
import com.dashboard.api.exception.KpiGoalOverrideConflictException;
import com.dashboard.api.model.acesso.KpiGoalEntity;
import com.dashboard.api.model.acesso.KpiGoalHistoryEntity;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.KpiGoalHistoryRepository;
import com.dashboard.api.repository.acesso.KpiGoalRepository;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataAccessResourceFailureException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KpiGoalServiceTest {

    @Mock
    private KpiGoalRepository repository;
    @Mock
    private KpiGoalHistoryRepository historyRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private KpiGoalService service;

    @BeforeEach
    void setUp() {
        service = new KpiGoalService(repository, historyRepository, usuarioRepository);
    }

    @Test
    void buscarMetaEfetivaDeveUsarOverrideDepoisGlobalDepoisDefault() {
        when(repository.findGlobalGoals()).thenReturn(List.of(goal(null, "collector_usage", "91")));
        when(repository.findAllByBranchId("OSASCO")).thenReturn(List.of(goal("OSASCO", "delivery_performance", "92")));

        KpiGoalEffectiveDTO dto = service.buscarMetaEfetiva("OSASCO");

        assertThat(dto.source()).isEqualTo(KpiGoalService.SOURCE_BRANCH_OVERRIDE);
        assertThat(dto.goals())
                .containsEntry("delivery_performance", new BigDecimal("92"))
                .containsEntry("collector_usage", new BigDecimal("91"))
                .containsEntry("cargo_cubage", BigDecimal.valueOf(85))
                .containsEntry("cargo_indemnity", BigDecimal.valueOf(2))
                .containsEntry("cutoff_time", BigDecimal.valueOf(98));
    }

    @Test
    void buscarMetaEfetivaGlobalDeveAceitarBranchIdGlobal() {
        when(repository.findGlobalGoals()).thenReturn(List.of(goal(null, "delivery_performance", "94")));

        KpiGoalEffectiveDTO dto = service.buscarMetaEfetiva("GLOBAL");

        assertThat(dto.branchId()).isEqualTo("GLOBAL");
        assertThat(dto.source()).isEqualTo(KpiGoalService.SOURCE_GLOBAL);
        assertThat(dto.goals()).containsEntry("delivery_performance", new BigDecimal("94"));
    }

    @Test
    void buscarMetaEfetivaDevePropagarFalhaQuandoTabelaIndisponivel() {
        when(repository.findGlobalGoals()).thenThrow(new DataAccessResourceFailureException("schema ausente"));

        assertThatThrownBy(() -> service.buscarMetaEfetiva("GLOBAL"))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void atualizarGlobalDeveRetornarConflitoQuandoHaMetaEspecifica() {
        when(repository.findAllBranchOverrides())
                .thenReturn(List.of(goal("SPO", "delivery_performance", "95")));
        when(repository.findGlobalGoals()).thenReturn(List.of());

        assertThatThrownBy(() -> service.atualizarMetasGlobais(
                new KpiGoalsUpdateRequestDTO(defaultGoals(), false),
                "admin@empresa.com"
        )).isInstanceOf(KpiGoalOverrideConflictException.class)
                .hasMessageContaining("Remova todas as metas isoladas")
                .satisfies(error -> {
                    KpiGoalOverrideConflictException ex = (KpiGoalOverrideConflictException) error;
                    assertThat(ex.getBranches()).extracting("branchId").containsExactly("SPO");
                });
    }

    @Test
    void atualizarGlobalComForceTambemDeveBloquearQuandoHaMetaEspecifica() {
        KpiGoalEntity override = goal("SPO", "delivery_performance", "92");
        when(repository.findAllBranchOverrides()).thenReturn(List.of(override));
        when(repository.findGlobalGoals()).thenReturn(List.of());

        assertThatThrownBy(() -> service.atualizarMetasGlobais(
                new KpiGoalsUpdateRequestDTO(defaultGoals(), true),
                "admin@empresa.com"
        )).isInstanceOf(KpiGoalOverrideConflictException.class);

        verify(repository, never()).deleteAll(List.of(override));
    }

    @Test
    void atualizarFilialDeveCriarOverrideApenasQuandoDiferenteDoGlobal() {
        UsuarioEntity usuario = usuario("Admin");
        Map<String, BigDecimal> goals = defaultGoals();
        goals.put("delivery_performance", BigDecimal.valueOf(92));
        when(usuarioRepository.findByEmailIgnoreCase("admin@empresa.com")).thenReturn(Optional.of(usuario));
        when(repository.findGlobalGoals()).thenReturn(List.of());
        when(repository.findAllByBranchId("OSASCO")).thenReturn(List.of());
        when(repository.save(any(KpiGoalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.atualizarMetasFilial(
                "OSASCO",
                new KpiGoalsUpdateRequestDTO(goals, null),
                "admin@empresa.com"
        );

        ArgumentCaptor<KpiGoalEntity> captor = ArgumentCaptor.forClass(KpiGoalEntity.class);
        verify(repository).save(captor.capture());
        KpiGoalEntity salvo = captor.getValue();
        assertThat(salvo.getBranchId()).isEqualTo("OSASCO");
        assertThat(salvo.getIndicatorKey()).isEqualTo("delivery_performance");
        assertThat(salvo.getGoalValue()).isEqualByComparingTo("92.000");
        assertThat(salvo.getUpdatedByUser()).isSameAs(usuario);
    }

    @Test
    void atualizarFilialDeveRejeitarIndicadorInvalido() {
        Map<String, BigDecimal> goals = defaultGoals();
        goals.put("invalido", BigDecimal.TEN);

        assertThatThrownBy(() -> service.atualizarMetasFilial(
                "OSASCO",
                new KpiGoalsUpdateRequestDTO(goals, null),
                "admin@empresa.com"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Indicador de meta inválido");
    }

    @Test
    void atualizarFilialDeveRejeitarValorForaDaFaixa() {
        Map<String, BigDecimal> goals = defaultGoals();
        goals.put("delivery_performance", BigDecimal.valueOf(101));

        assertThatThrownBy(() -> service.atualizarMetasFilial(
                "OSASCO",
                new KpiGoalsUpdateRequestDTO(goals, null),
                "admin@empresa.com"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Meta deve estar entre 0 e 100");
    }

    @Test
    void buscarOverridesPorIndicadorDeveRetornarMetasEspecificasDoIndicador() {
        UsuarioEntity usuario = usuario("Admin");
        when(repository.findGlobalGoals()).thenReturn(List.of(goal(null, "collector_usage", "90")));
        KpiGoalEntity divergente = goal("SPO", "collector_usage", "85");
        divergente.setUpdatedByUser(usuario);
        KpiGoalEntity igualGlobalAtual = goal("REC", "collector_usage", "90");
        when(repository.findAllBranchOverridesByIndicatorKey("collector_usage"))
                .thenReturn(List.of(divergente, igualGlobalAtual));

        var response = service.buscarOverridesPorIndicador("collector_usage");

        assertThat(response.globalGoal()).isEqualByComparingTo("90");
        assertThat(response.overrides()).hasSize(2);
        assertThat(response.overrides()).first().satisfies(override -> {
            assertThat(override.branchId()).isEqualTo("REC");
            assertThat(override.goalValue()).isEqualByComparingTo("90");
        });
        assertThat(response.overrides()).element(1).satisfies(override -> {
            assertThat(override.branchId()).isEqualTo("SPO");
            assertThat(override.branchName()).isEqualTo("SPO");
            assertThat(override.goalValue()).isEqualByComparingTo("85");
            assertThat(override.updatedBy().name()).isEqualTo("Admin");
        });
    }

    @Test
    void buscarOverridesPorIndicadorDevePropagarFalhaQuandoTabelaIndisponivel() {
        when(repository.findGlobalGoals()).thenThrow(new DataAccessResourceFailureException("schema ausente"));

        assertThatThrownBy(() -> service.buscarOverridesPorIndicador("collector_usage"))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void buscarHistoricoPaginadoDeveRetornarMetadadosDaPagina() {
        KpiGoalHistoryEntity registro = history("SPO", "collector_usage", "90", "88", usuario("Admin"));
        when(historyRepository.findByBranchIdOrderByUpdatedAtDesc(eq("SPO"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(registro), PageRequest.of(1, 10), 12));

        var pagina = service.buscarHistoricoPaginado("SPO", 2, 10);

        assertThat(pagina.totalElementos()).isEqualTo(11);
        assertThat(pagina.totalPaginas()).isEqualTo(2);
        assertThat(pagina.paginaAtual()).isEqualTo(2);
        assertThat(pagina.tamanhoPagina()).isEqualTo(10);
        assertThat(pagina.conteudo()).hasSize(1);
        assertThat(pagina.conteudo().get(0).branchId()).isEqualTo("SPO");
        assertThat(pagina.conteudo().get(0).indicatorKey()).isEqualTo("collector_usage");
    }

    @Test
    void atualizarFilialDeveRemoverHistoricoMaisAntigoQuandoExcederLimite() {
        UsuarioEntity usuario = usuario("Admin");
        Map<String, BigDecimal> goals = defaultGoals();
        goals.put("delivery_performance", BigDecimal.valueOf(92));
        KpiGoalHistoryEntity antigo = history("OSASCO", "delivery_performance", "95", "94", usuario);

        when(usuarioRepository.findByEmailIgnoreCase("admin@empresa.com")).thenReturn(Optional.of(usuario));
        when(repository.findGlobalGoals()).thenReturn(List.of());
        when(repository.findAllByBranchId("OSASCO")).thenReturn(List.of());
        when(repository.save(any(KpiGoalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.countByBranchId("OSASCO")).thenReturn(201L);
        when(historyRepository.findByBranchIdOrderByUpdatedAtAsc(eq("OSASCO"), any(Pageable.class)))
                .thenReturn(List.of(antigo));

        service.atualizarMetasFilial(
                "OSASCO",
                new KpiGoalsUpdateRequestDTO(goals, null),
                "admin@empresa.com"
        );

        verify(historyRepository).deleteAllInBatch(List.of(antigo));
    }

    @Test
    void buscarMetasEfetivasPorIndicadorDevePropagarFalhaQuandoTabelaIndisponivel() {
        when(repository.findGlobalGoals()).thenThrow(new DataAccessResourceFailureException("schema ausente"));

        assertThatThrownBy(() -> service.buscarMetasEfetivasPorIndicador(
                "collector_usage",
                List.of("SPO", "GLOBAL", "REC")
        )).isInstanceOf(DataAccessResourceFailureException.class);
    }

    private Map<String, BigDecimal> defaultGoals() {
        return new java.util.LinkedHashMap<>(Map.of(
                "delivery_performance", BigDecimal.valueOf(95),
                "collector_usage", BigDecimal.valueOf(90),
                "cargo_cubage", BigDecimal.valueOf(85),
                "cargo_indemnity", BigDecimal.valueOf(2),
                "cutoff_time", BigDecimal.valueOf(98)
        ));
    }

    private UsuarioEntity usuario(String nome) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(44L);
        usuario.setNome(nome);
        usuario.setEmail("admin@empresa.com");
        return usuario;
    }

    private KpiGoalEntity goal(String branchId, String indicatorKey, String value) {
        KpiGoalEntity goal = new KpiGoalEntity();
        goal.setBranchId(branchId);
        goal.setIndicatorKey(indicatorKey);
        goal.setGoalValue(new BigDecimal(value));
        return goal;
    }

    private KpiGoalHistoryEntity history(
            String branchId,
            String indicatorKey,
            String oldValue,
            String newValue,
            UsuarioEntity usuario
    ) {
        KpiGoalHistoryEntity history = new KpiGoalHistoryEntity();
        history.setBranchId(branchId);
        history.setIndicatorKey(indicatorKey);
        history.setOldValue(new BigDecimal(oldValue));
        history.setNewValue(new BigDecimal(newValue));
        history.setUpdatedByUser(usuario);
        history.setAction("BRANCH_UPDATE");
        return history;
    }
}
