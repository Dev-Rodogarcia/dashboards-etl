package com.dashboard.api.service;

import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalBranchDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalEffectiveDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalHistoryDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalOverrideDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalOverridesByIndicatorDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalUserDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalsFullDTO;
import com.dashboard.api.dto.indicadoresgestao.KpiGoalsUpdateRequestDTO;
import com.dashboard.api.exception.KpiGoalOverrideConflictException;
import com.dashboard.api.model.acesso.KpiGoalEntity;
import com.dashboard.api.model.acesso.KpiGoalHistoryEntity;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.KpiGoalHistoryRepository;
import com.dashboard.api.repository.acesso.KpiGoalRepository;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KpiGoalService {

    public static final String GLOBAL_BRANCH_ID = "GLOBAL";
    public static final String SOURCE_GLOBAL = "GLOBAL";
    public static final String SOURCE_BRANCH_OVERRIDE = "BRANCH_OVERRIDE";
    public static final String DELIVERY_PERFORMANCE = "delivery_performance";
    public static final String COLLECTOR_USAGE = "collector_usage";
    public static final String CARGO_CUBAGE = "cargo_cubage";
    public static final String CARGO_INDEMNITY = "cargo_indemnity";
    public static final String CUTOFF_TIME = "cutoff_time";

    private static final String ACTION_GLOBAL_UPDATE = "GLOBAL_UPDATE";
    private static final String ACTION_BRANCH_UPDATE = "BRANCH_UPDATE";
    private static final String ACTION_BRANCH_OVERRIDE_REMOVED = "BRANCH_OVERRIDE_REMOVED";
    private static final int MAX_HISTORY_PAGE_SIZE = 50;
    private static final int MAX_HISTORY_ROWS_PER_SCOPE = 200;

    private static final List<String> INDICATOR_ORDER = List.of(
            DELIVERY_PERFORMANCE,
            COLLECTOR_USAGE,
            CARGO_CUBAGE,
            CARGO_INDEMNITY,
            CUTOFF_TIME
    );
    private static final Set<String> VALID_INDICATORS = Set.copyOf(INDICATOR_ORDER);
    private static final Map<String, BigDecimal> DEFAULT_GOALS = Map.of(
            DELIVERY_PERFORMANCE, BigDecimal.valueOf(95),
            COLLECTOR_USAGE, BigDecimal.valueOf(90),
            CARGO_CUBAGE, BigDecimal.valueOf(85),
            CARGO_INDEMNITY, BigDecimal.valueOf(2),
            CUTOFF_TIME, BigDecimal.valueOf(98)
    );

    private final KpiGoalRepository repository;
    private final KpiGoalHistoryRepository historyRepository;
    private final UsuarioRepository usuarioRepository;

    public KpiGoalService(
            KpiGoalRepository repository,
            KpiGoalHistoryRepository historyRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public KpiGoalsFullDTO buscarMetasCompletas() {
        Map<String, BigDecimal> globalGoals = buscarMetasGlobaisEfetivas();
        List<KpiGoalBranchDTO> branches = branchesFromOverrides(repository.findAllBranchOverrides(), globalGoals);
        return new KpiGoalsFullDTO(globalGoals, branches);
    }

    @Transactional(readOnly = true)
    public KpiGoalEffectiveDTO buscarMetaEfetiva(String branchId) {
        String normalizedBranchId = normalizarBranchId(branchId);
        Map<String, BigDecimal> globalGoals = buscarMetasGlobaisEfetivas();
        if (normalizedBranchId == null) {
            return new KpiGoalEffectiveDTO(GLOBAL_BRANCH_ID, SOURCE_GLOBAL, globalGoals);
        }

        List<KpiGoalEntity> branchOverrides = repository.findAllByBranchId(normalizedBranchId);
        Map<String, BigDecimal> effectiveGoals = mergeGoals(globalGoals, branchOverrides);
        String source = branchOverrides.isEmpty() ? SOURCE_GLOBAL : SOURCE_BRANCH_OVERRIDE;
        return new KpiGoalEffectiveDTO(normalizedBranchId, source, effectiveGoals);
    }

    @Transactional
    public KpiGoalsFullDTO atualizarMetasGlobais(KpiGoalsUpdateRequestDTO request, String usuarioEmail) {
        Map<String, BigDecimal> newGoals = normalizarGoals(request.goals());
        List<KpiGoalEntity> branchOverrides = repository.findAllBranchOverrides();
        Map<String, BigDecimal> currentGlobalGoals = buscarMetasGlobaisEfetivas();
        List<KpiGoalBranchDTO> branchSpecificGoals = branchesFromOverrides(branchOverrides, currentGlobalGoals);

        if (!branchSpecificGoals.isEmpty()) {
            throw new KpiGoalOverrideConflictException(branchSpecificGoals);
        }

        UsuarioEntity usuario = usuarioAutenticado(usuarioEmail);
        Map<String, KpiGoalEntity> globalEntities = entitiesByIndicator(repository.findGlobalGoals());

        for (String indicatorKey : INDICATOR_ORDER) {
            BigDecimal oldValue = currentGlobalGoals.get(indicatorKey);
            BigDecimal newValue = newGoals.get(indicatorKey);
            KpiGoalEntity entity = globalEntities.getOrDefault(indicatorKey, new KpiGoalEntity());
            entity.setBranchId(null);
            entity.setIndicatorKey(indicatorKey);
            entity.setGoalValue(newValue);
            entity.setUpdatedByUser(usuario);
            repository.save(entity);

            if (!mesmoValor(oldValue, newValue)) {
                registrarHistorico(null, indicatorKey, oldValue, newValue, usuario, ACTION_GLOBAL_UPDATE);
            }
        }

        return buscarMetasCompletas();
    }

    @Transactional
    public KpiGoalEffectiveDTO atualizarMetasFilial(String branchId, KpiGoalsUpdateRequestDTO request, String usuarioEmail) {
        String normalizedBranchId = normalizarBranchOverrideId(branchId);
        Map<String, BigDecimal> newGoals = normalizarGoals(request.goals());
        UsuarioEntity usuario = usuarioAutenticado(usuarioEmail);
        Map<String, BigDecimal> globalGoals = buscarMetasGlobaisEfetivas();
        List<KpiGoalEntity> currentOverrides = repository.findAllByBranchId(normalizedBranchId);
        Map<String, KpiGoalEntity> currentByIndicator = entitiesByIndicator(currentOverrides);

        for (String indicatorKey : INDICATOR_ORDER) {
            BigDecimal newValue = newGoals.get(indicatorKey);
            BigDecimal globalValue = globalGoals.get(indicatorKey);
            KpiGoalEntity current = currentByIndicator.get(indicatorKey);

            if (mesmoValor(newValue, globalValue)) {
                if (current != null) {
                    registrarHistorico(
                            normalizedBranchId,
                            indicatorKey,
                            current.getGoalValue(),
                            globalValue,
                            usuario,
                            ACTION_BRANCH_OVERRIDE_REMOVED
                    );
                    repository.delete(current);
                }
                continue;
            }

            BigDecimal oldValue = current != null ? current.getGoalValue() : globalValue;
            if (current == null) {
                current = new KpiGoalEntity();
                current.setBranchId(normalizedBranchId);
                current.setIndicatorKey(indicatorKey);
            }
            current.setGoalValue(newValue);
            current.setUpdatedByUser(usuario);
            repository.save(current);

            if (!mesmoValor(oldValue, newValue)) {
                registrarHistorico(normalizedBranchId, indicatorKey, oldValue, newValue, usuario, ACTION_BRANCH_UPDATE);
            }
        }

        return buscarMetaEfetiva(normalizedBranchId);
    }

    @Transactional
    public KpiGoalEffectiveDTO removerOverrideFilial(String branchId, String usuarioEmail) {
        String normalizedBranchId = normalizarBranchOverrideId(branchId);
        UsuarioEntity usuario = usuarioAutenticado(usuarioEmail);
        Map<String, BigDecimal> globalGoals = buscarMetasGlobaisEfetivas();
        List<KpiGoalEntity> overrides = repository.findAllByBranchId(normalizedBranchId);

        for (KpiGoalEntity override : overrides) {
            registrarHistorico(
                    normalizedBranchId,
                    override.getIndicatorKey(),
                    override.getGoalValue(),
                    globalGoals.get(override.getIndicatorKey()),
                    usuario,
                    ACTION_BRANCH_OVERRIDE_REMOVED
            );
        }
        repository.deleteAll(overrides);

        return new KpiGoalEffectiveDTO(normalizedBranchId, SOURCE_GLOBAL, globalGoals);
    }

    @Transactional(readOnly = true)
    public List<KpiGoalHistoryDTO> buscarHistorico(String branchId, int limit) {
        return buscarHistoricoPaginado(branchId, 1, limit).conteudo();
    }

    @Transactional(readOnly = true)
    public PaginaDTO<KpiGoalHistoryDTO> buscarHistoricoPaginado(String branchId, int pagina, int tamanhoPagina) {
        int paginaAplicada = Math.max(1, pagina);
        int tamanhoAplicado = Math.max(1, Math.min(tamanhoPagina, MAX_HISTORY_PAGE_SIZE));
        PageRequest pageRequest = PageRequest.of(paginaAplicada - 1, tamanhoAplicado);
        String normalizedBranchId = branchId == null || branchId.isBlank()
                ? null
                : normalizarBranchId(branchId);

        Page<KpiGoalHistoryEntity> history = branchId == null || branchId.isBlank()
                ? historyRepository.findAllByOrderByUpdatedAtDesc(pageRequest)
                : normalizedBranchId == null
                    ? historyRepository.findByBranchIdIsNullOrderByUpdatedAtDesc(pageRequest)
                    : historyRepository.findByBranchIdOrderByUpdatedAtDesc(normalizedBranchId, pageRequest);

        return new PaginaDTO<>(
                history.getContent().stream().map(this::toHistoryDto).toList(),
                history.getTotalElements(),
                history.getTotalPages(),
                history.getNumber() + 1,
                history.getSize()
        );
    }

    @Transactional(readOnly = true)
    public KpiGoalOverridesByIndicatorDTO buscarOverridesPorIndicador(String indicatorKey) {
        String normalizedIndicator = normalizarIndicatorKey(indicatorKey);
        BigDecimal globalGoal = buscarMetasGlobaisEfetivas().get(normalizedIndicator);
        List<KpiGoalOverrideDTO> overrides = repository.findAllBranchOverridesByIndicatorKey(normalizedIndicator)
                .stream()
                .map(goal -> new KpiGoalOverrideDTO(
                        goal.getBranchId(),
                        goal.getBranchId(),
                        goal.getGoalValue(),
                        goal.getUpdatedAt(),
                        toUserDto(goal.getUpdatedByUser())
                ))
                .sorted(Comparator.comparing(KpiGoalOverrideDTO::branchName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new KpiGoalOverridesByIndicatorDTO(normalizedIndicator, globalGoal, overrides);
    }

    public Map<String, BigDecimal> buscarMetasEfetivasPorIndicador(String indicatorKey, Collection<String> branchIds) {
        String normalizedIndicator = normalizarIndicatorKey(indicatorKey);
        BigDecimal globalGoal = buscarMetasGlobaisEfetivas().get(normalizedIndicator);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (String branchId : branchIds) {
            String normalizedBranch = normalizarBranchId(branchId);
            if (normalizedBranch != null) {
                result.put(normalizedBranch, globalGoal);
            }
        }
        if (result.isEmpty()) {
            return result;
        }

        repository.findAllByBranchIdIn(result.keySet()).stream()
                .filter(goal -> normalizedIndicator.equals(goal.getIndicatorKey()))
                .forEach(goal -> result.put(goal.getBranchId(), goal.getGoalValue()));

        return result;
    }

    public static List<String> indicatorOrder() {
        return INDICATOR_ORDER;
    }

    private Map<String, BigDecimal> buscarMetasGlobaisEfetivas() {
        Map<String, BigDecimal> result = defaultGoals();
        repository.findGlobalGoals().forEach(goal -> {
            if (VALID_INDICATORS.contains(goal.getIndicatorKey())) {
                result.put(goal.getIndicatorKey(), goal.getGoalValue());
            }
        });
        return result;
    }

    private Map<String, BigDecimal> defaultGoals() {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        INDICATOR_ORDER.forEach(indicator -> result.put(indicator, DEFAULT_GOALS.get(indicator)));
        return result;
    }

    private List<KpiGoalBranchDTO> branchesFromOverrides(
            List<KpiGoalEntity> overrides,
            Map<String, BigDecimal> baseGoals
    ) {
        return overrides.stream()
                .filter(goal -> goal.getBranchId() != null && !goal.getBranchId().isBlank())
                .collect(Collectors.groupingBy(
                        KpiGoalEntity::getBranchId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .map(entry -> toBranchDto(entry.getKey(), entry.getValue(), baseGoals))
                .sorted(Comparator.comparing(KpiGoalBranchDTO::branchId, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private KpiGoalBranchDTO toBranchDto(String branchId, List<KpiGoalEntity> overrides, Map<String, BigDecimal> baseGoals) {
        KpiGoalEntity lastUpdate = overrides.stream()
                .max(Comparator.comparing(
                        KpiGoalEntity::getUpdatedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())
                ))
                .orElse(null);

        return new KpiGoalBranchDTO(
                branchId,
                mergeGoals(baseGoals, overrides),
                lastUpdate != null ? lastUpdate.getUpdatedAt() : null,
                lastUpdate != null ? toUserDto(lastUpdate.getUpdatedByUser()) : null
        );
    }

    private Map<String, BigDecimal> mergeGoals(Map<String, BigDecimal> baseGoals, List<KpiGoalEntity> overrides) {
        Map<String, BigDecimal> result = new LinkedHashMap<>(baseGoals);
        for (KpiGoalEntity goal : overrides) {
            if (VALID_INDICATORS.contains(goal.getIndicatorKey())) {
                result.put(goal.getIndicatorKey(), goal.getGoalValue());
            }
        }
        return result;
    }

    private Map<String, KpiGoalEntity> entitiesByIndicator(List<KpiGoalEntity> goals) {
        Map<String, KpiGoalEntity> result = new LinkedHashMap<>();
        for (KpiGoalEntity goal : goals) {
            if (VALID_INDICATORS.contains(goal.getIndicatorKey())) {
                result.put(goal.getIndicatorKey(), goal);
            }
        }
        return result;
    }

    private String normalizarBranchId(String branchId) {
        String normalized = branchId == null || branchId.isBlank() ? GLOBAL_BRANCH_ID : branchId.trim();
        if (GLOBAL_BRANCH_ID.equalsIgnoreCase(normalized)) {
            return null;
        }
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("Filial da meta excede 120 caracteres.");
        }
        return normalized;
    }

    private String normalizarBranchOverrideId(String branchId) {
        String normalized = normalizarBranchId(branchId);
        if (normalized == null) {
            throw new IllegalArgumentException("Use o endpoint de meta global para alterar GLOBAL.");
        }
        return normalized;
    }

    private Map<String, BigDecimal> normalizarGoals(Map<String, BigDecimal> goals) {
        Objects.requireNonNull(goals, "goals é obrigatório.");
        for (String indicatorKey : goals.keySet()) {
            normalizarIndicatorKey(indicatorKey);
        }

        Map<String, BigDecimal> normalized = new LinkedHashMap<>();
        for (String indicatorKey : INDICATOR_ORDER) {
            if (!goals.containsKey(indicatorKey)) {
                throw new IllegalArgumentException("Informe meta para o indicador: " + indicatorKey);
            }
            normalized.put(indicatorKey, normalizarGoalValue(goals.get(indicatorKey)));
        }
        return normalized;
    }

    private String normalizarIndicatorKey(String indicatorKey) {
        String normalized = Objects.requireNonNull(indicatorKey, "indicatorKey é obrigatório.").trim();
        if (!VALID_INDICATORS.contains(normalized)) {
            throw new IllegalArgumentException("Indicador de meta inválido: " + normalized);
        }
        return normalized;
    }

    private BigDecimal normalizarGoalValue(BigDecimal goalValue) {
        BigDecimal normalized = Objects.requireNonNull(goalValue, "goalValue é obrigatório.")
                .setScale(3, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0 || normalized.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Meta deve estar entre 0 e 100.");
        }
        return normalized;
    }

    private boolean mesmoValor(BigDecimal left, BigDecimal right) {
        return Optional.ofNullable(left).orElse(BigDecimal.ZERO)
                .compareTo(Optional.ofNullable(right).orElse(BigDecimal.ZERO)) == 0;
    }

    private UsuarioEntity usuarioAutenticado(String usuarioEmail) {
        return usuarioRepository.findByEmailIgnoreCase(Objects.toString(usuarioEmail, ""))
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
    }

    private void registrarHistorico(
            String branchId,
            String indicatorKey,
            BigDecimal oldValue,
            BigDecimal newValue,
            UsuarioEntity usuario,
            String action
    ) {
        KpiGoalHistoryEntity history = new KpiGoalHistoryEntity();
        history.setBranchId(branchId);
        history.setIndicatorKey(indicatorKey);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setUpdatedByUser(usuario);
        history.setAction(action);
        historyRepository.save(history);
        removerHistoricoExcedente(branchId);
    }

    private void removerHistoricoExcedente(String branchId) {
        long total = branchId == null
                ? historyRepository.countByBranchIdIsNull()
                : historyRepository.countByBranchId(branchId);
        long excedente = total - MAX_HISTORY_ROWS_PER_SCOPE;
        if (excedente <= 0) {
            return;
        }

        int quantidadeRemover = excedente > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) excedente;
        PageRequest pageRequest = PageRequest.of(0, quantidadeRemover);
        List<KpiGoalHistoryEntity> antigos = branchId == null
                ? historyRepository.findByBranchIdIsNullOrderByUpdatedAtAsc(pageRequest)
                : historyRepository.findByBranchIdOrderByUpdatedAtAsc(branchId, pageRequest);
        if (!antigos.isEmpty()) {
            historyRepository.deleteAllInBatch(antigos);
        }
    }

    private KpiGoalHistoryDTO toHistoryDto(KpiGoalHistoryEntity history) {
        return new KpiGoalHistoryDTO(
                history.getBranchId() == null ? GLOBAL_BRANCH_ID : history.getBranchId(),
                history.getIndicatorKey(),
                history.getOldValue(),
                history.getNewValue(),
                toUserDto(history.getUpdatedByUser()),
                history.getUpdatedAt(),
                history.getAction()
        );
    }

    private KpiGoalUserDTO toUserDto(UsuarioEntity usuario) {
        if (usuario == null) {
            return null;
        }
        return new KpiGoalUserDTO(
                usuario.getId() != null ? String.valueOf(usuario.getId()) : null,
                usuario.getNome()
        );
    }
}
