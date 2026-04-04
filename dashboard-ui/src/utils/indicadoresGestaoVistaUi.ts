import type {
  CubagemMercadoriasSeriePoint,
  HorariosCorteSeriePoint,
  IndenizacaoMercadoriasSeriePoint,
  PerformanceEntregaSeriePoint,
  UtilizacaoColetoresSeriePoint,
} from '../types/indicadoresGestaoAVista';

export type GoalMode = 'atLeast' | 'atMost';
export type GoalTone = 'positive' | 'warning' | 'negative' | 'neutral' | 'loading' | 'error' | 'empty';

export const GOAL_WARNING_PROGRESS_PCT = 50;

export interface GoalToneStyle {
  bg: string;
  border: string;
  text: string;
  soft: string;
  track: string;
  fill: string;
}

export interface GoalAssessment {
  tone: GoalTone;
  label: string;
  met: boolean | null;
  delta: number | null;
  progressPct: number | null;
}

export interface PerformanceRankingItem {
  group: string;
  pctNoPrazo: number;
  totalEntregas: number;
  entregasNoPrazo: number;
  entregasSemDados: number;
  entregasForaDoPrazo: number;
}

export interface UtilizacaoRankingItem {
  group: string;
  pctUtilizacao: number;
  ordensConferencia: number;
  manifestosEmitidos: number;
  manifestosDescarregamento: number;
  totalManifestos: number;
}

export interface CubagemRankingItem {
  group: string;
  pctCubagem: number;
  totalFretes: number;
  fretesCubados: number;
  fretesNaoCubados: number;
}

export interface IndenizacaoRankingItem {
  group: string;
  pctIndenizacao: number;
  totalSinistros: number;
  valorIndenizadoAbs: number;
  faturamentoBase: number;
}

export interface HorariosRankingItem {
  group: string;
  pctNoHorario: number;
  saidasNoHorario: number;
  saidasForaDoHorario: number;
  totalProgramado: number;
}

interface GoalAssessmentArgs {
  value: number;
  threshold: number;
  mode: GoalMode;
  hasData: boolean;
  isLoading?: boolean;
  isError?: boolean;
}

function percentual(parte: number, total: number, decimais = 1): number {
  if (total <= 0) {
    return 0;
  }
  const valor = (parte / total) * 100;
  return Number(valor.toFixed(decimais));
}

function groupLabel(value: string | null | undefined, fallback = 'Nao informado'): string {
  return value?.trim() ? value.trim() : fallback;
}

export function getGoalToneStyle(tone: GoalTone): GoalToneStyle {
  const palette: Record<GoalTone, GoalToneStyle> = {
    positive: {
      bg: 'rgba(22, 163, 74, 0.10)',
      border: 'rgba(22, 163, 74, 0.28)',
      text: '#15803d',
      soft: 'rgba(22, 163, 74, 0.06)',
      track: 'rgba(22, 163, 74, 0.16)',
      fill: '#16a34a',
    },
    warning: {
      bg: 'rgba(245, 158, 11, 0.10)',
      border: 'rgba(245, 158, 11, 0.28)',
      text: '#a16207',
      soft: 'rgba(245, 158, 11, 0.06)',
      track: 'rgba(245, 158, 11, 0.18)',
      fill: '#f59e0b',
    },
    negative: {
      bg: 'rgba(239, 68, 68, 0.10)',
      border: 'rgba(239, 68, 68, 0.24)',
      text: '#b91c1c',
      soft: 'rgba(239, 68, 68, 0.05)',
      track: 'rgba(239, 68, 68, 0.16)',
      fill: '#ef4444',
    },
    neutral: {
      bg: 'rgba(100, 116, 139, 0.12)',
      border: 'rgba(100, 116, 139, 0.20)',
      text: '#475569',
      soft: 'rgba(100, 116, 139, 0.05)',
      track: 'rgba(100, 116, 139, 0.16)',
      fill: '#64748b',
    },
    loading: {
      bg: 'rgba(59, 130, 246, 0.12)',
      border: 'rgba(59, 130, 246, 0.22)',
      text: '#1d4ed8',
      soft: 'rgba(59, 130, 246, 0.05)',
      track: 'rgba(59, 130, 246, 0.16)',
      fill: '#3b82f6',
    },
    error: {
      bg: 'rgba(239, 68, 68, 0.12)',
      border: 'rgba(239, 68, 68, 0.22)',
      text: '#b91c1c',
      soft: 'rgba(239, 68, 68, 0.05)',
      track: 'rgba(239, 68, 68, 0.16)',
      fill: '#ef4444',
    },
    empty: {
      bg: 'rgba(245, 158, 11, 0.12)',
      border: 'rgba(245, 158, 11, 0.22)',
      text: '#a16207',
      soft: 'rgba(245, 158, 11, 0.05)',
      track: 'rgba(245, 158, 11, 0.18)',
      fill: '#f59e0b',
    },
  };

  return palette[tone];
}

export function calcularProgressoMeta(value: number, threshold: number, mode: GoalMode): number {
  if (!Number.isFinite(value) || !Number.isFinite(threshold) || threshold <= 0) {
    return 0;
  }

  if (mode === 'atLeast') {
    return Math.max(0, Math.min(100, Number(((value / threshold) * 100).toFixed(1))));
  }

  if (value <= 0) {
    return 100;
  }

  return Math.max(0, Math.min(100, Number(((threshold / value) * 100).toFixed(1))));
}

export function resolverTomMetaPorProgresso(progressPct: number): GoalTone {
  if (progressPct >= 100) {
    return 'positive';
  }
  if (progressPct >= GOAL_WARNING_PROGRESS_PCT) {
    return 'warning';
  }
  return 'negative';
}

export function avaliarMetaIndicador({
  value,
  threshold,
  mode,
  hasData,
  isLoading = false,
  isError = false,
}: GoalAssessmentArgs): GoalAssessment {
  if (isError) {
    return { tone: 'error', label: 'Erro tecnico', met: null, delta: null, progressPct: null };
  }

  if (isLoading && !hasData) {
    return { tone: 'loading', label: 'Carregando', met: null, delta: null, progressPct: null };
  }

  if (!hasData) {
    return { tone: 'empty', label: 'Sem dados', met: null, delta: null, progressPct: null };
  }

  const met = mode === 'atLeast' ? value >= threshold : value <= threshold;
  const progressPct = calcularProgressoMeta(value, threshold, mode);
  const tone = met ? 'positive' : resolverTomMetaPorProgresso(progressPct);
  return {
    tone,
    label: met ? 'Dentro da meta' : tone === 'warning' ? 'Em atenção' : 'Crítico',
    met,
    delta: Number((value - threshold).toFixed(2)),
    progressPct,
  };
}

export function aggregatePerformanceRanking(points: PerformanceEntregaSeriePoint[]): PerformanceRankingItem[] {
  const grouped = new Map<string, Omit<PerformanceRankingItem, 'pctNoPrazo' | 'entregasForaDoPrazo'>>();

  for (const point of points) {
    const key = groupLabel(point.responsavelRegiaoDestino, 'Regiao nao informada');
    const current = grouped.get(key) ?? {
      group: key,
      totalEntregas: 0,
      entregasNoPrazo: 0,
      entregasSemDados: 0,
    };

    current.totalEntregas += point.totalEntregas ?? 0;
    current.entregasNoPrazo += point.entregasNoPrazo ?? 0;
    current.entregasSemDados += point.entregasSemDados ?? 0;
    grouped.set(key, current);
  }

  return Array.from(grouped.values())
    .map((item) => ({
      ...item,
      entregasForaDoPrazo: Math.max(item.totalEntregas - item.entregasNoPrazo - item.entregasSemDados, 0),
      pctNoPrazo: percentual(item.entregasNoPrazo, item.totalEntregas),
    }))
    .sort((left, right) => left.pctNoPrazo - right.pctNoPrazo || right.totalEntregas - left.totalEntregas || left.group.localeCompare(right.group));
}

export function aggregateUtilizacaoRanking(points: UtilizacaoColetoresSeriePoint[]): UtilizacaoRankingItem[] {
  const grouped = new Map<string, Omit<UtilizacaoRankingItem, 'pctUtilizacao'>>();

  for (const point of points) {
    const key = groupLabel(point.filial, 'Filial nao informada');
    const current = grouped.get(key) ?? {
      group: key,
      ordensConferencia: 0,
      manifestosEmitidos: 0,
      manifestosDescarregamento: 0,
      totalManifestos: 0,
    };

    current.ordensConferencia += point.ordensConferencia ?? 0;
    current.manifestosEmitidos += point.manifestosEmitidos ?? 0;
    current.manifestosDescarregamento += point.manifestosDescarregamento ?? 0;
    current.totalManifestos += point.totalManifestos ?? 0;
    grouped.set(key, current);
  }

  return Array.from(grouped.values())
    .map((item) => ({
      ...item,
      pctUtilizacao: percentual(item.ordensConferencia, item.totalManifestos),
    }))
    .sort((left, right) => left.pctUtilizacao - right.pctUtilizacao || right.totalManifestos - left.totalManifestos || left.group.localeCompare(right.group));
}

export function aggregateCubagemRanking(points: CubagemMercadoriasSeriePoint[]): CubagemRankingItem[] {
  const grouped = new Map<string, Omit<CubagemRankingItem, 'pctCubagem' | 'fretesNaoCubados'>>();

  for (const point of points) {
    const key = groupLabel(point.filial, 'Filial nao informada');
    const current = grouped.get(key) ?? {
      group: key,
      totalFretes: 0,
      fretesCubados: 0,
    };

    current.totalFretes += point.totalFretes ?? 0;
    current.fretesCubados += point.fretesCubados ?? 0;
    grouped.set(key, current);
  }

  return Array.from(grouped.values())
    .map((item) => ({
      ...item,
      fretesNaoCubados: Math.max(item.totalFretes - item.fretesCubados, 0),
      pctCubagem: percentual(item.fretesCubados, item.totalFretes),
    }))
    .sort((left, right) => left.pctCubagem - right.pctCubagem || right.totalFretes - left.totalFretes || left.group.localeCompare(right.group));
}

export function aggregateIndenizacaoRanking(points: IndenizacaoMercadoriasSeriePoint[]): IndenizacaoRankingItem[] {
  const grouped = new Map<string, Omit<IndenizacaoRankingItem, 'pctIndenizacao'>>();

  for (const point of points) {
    const key = groupLabel(point.filial, 'Filial nao informada');
    const current = grouped.get(key) ?? {
      group: key,
      totalSinistros: 0,
      valorIndenizadoAbs: 0,
      faturamentoBase: 0,
    };

    current.totalSinistros += point.totalSinistros ?? 0;
    current.valorIndenizadoAbs += point.valorIndenizadoAbs ?? 0;
    current.faturamentoBase += point.faturamentoBase ?? 0;
    grouped.set(key, current);
  }

  return Array.from(grouped.values())
    .map((item) => ({
      ...item,
      valorIndenizadoAbs: Number(item.valorIndenizadoAbs.toFixed(2)),
      faturamentoBase: Number(item.faturamentoBase.toFixed(2)),
      pctIndenizacao: item.faturamentoBase > 0 ? Number(((item.valorIndenizadoAbs / item.faturamentoBase) * 100).toFixed(3)) : 0,
    }))
    .sort((left, right) => right.pctIndenizacao - left.pctIndenizacao || right.valorIndenizadoAbs - left.valorIndenizadoAbs || left.group.localeCompare(right.group));
}

export function aggregateHorariosRanking(points: HorariosCorteSeriePoint[]): HorariosRankingItem[] {
  const grouped = new Map<string, Omit<HorariosRankingItem, 'pctNoHorario' | 'saidasForaDoHorario'>>();

  for (const point of points) {
    const key = groupLabel(point.filial, 'Filial nao informada');
    const current = grouped.get(key) ?? {
      group: key,
      saidasNoHorario: 0,
      totalProgramado: 0,
    };

    current.saidasNoHorario += point.saidasNoHorario ?? 0;
    current.totalProgramado += point.totalProgramado ?? 0;
    grouped.set(key, current);
  }

  return Array.from(grouped.values())
    .map((item) => ({
      ...item,
      saidasForaDoHorario: Math.max(item.totalProgramado - item.saidasNoHorario, 0),
      pctNoHorario: percentual(item.saidasNoHorario, item.totalProgramado),
    }))
    .sort((left, right) => left.pctNoHorario - right.pctNoHorario || right.totalProgramado - left.totalProgramado || left.group.localeCompare(right.group));
}
