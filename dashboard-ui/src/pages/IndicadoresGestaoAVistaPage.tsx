import { useMemo, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import { AlertCircle, BarChart3, Boxes, Clock3, FileUp, Gauge, PackageCheck, ShieldAlert, Truck } from 'lucide-react';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import type { ColunaTabela } from '../components/shared/DataTable';
import DateRangePicker from '../components/shared/DateRangePicker';
import FilterBar, { type ActiveFilter } from '../components/shared/FilterBar';
import LastUpdated from '../components/shared/LastUpdated';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import IndicadoresGestaoSection from '../components/indicadores-gestao/IndicadoresGestaoSection';
import IndicadoresGestaoSummaryCard from '../components/indicadores-gestao/IndicadoresGestaoSummaryCard';
import { useFiltro } from '../contexts/FiltroContext';
import { useFiliais } from '../hooks/queries/useDimensoes';
import {
  useCubagemMercadoriasOverview,
  useCubagemMercadoriasSerie,
  useCubagemMercadoriasTabela,
  useHorariosCorteOverview,
  useHorariosCorteSerie,
  useHorariosCorteTabela,
  useImportarHorariosCorte,
  useIndenizacaoMercadoriasOverview,
  useIndenizacaoMercadoriasSerie,
  useIndenizacaoMercadoriasTabela,
  usePerformanceEntregaOverview,
  usePerformanceEntregaSerie,
  usePerformanceEntregaTabela,
  useUtilizacaoColetoresOverview,
  useUtilizacaoColetoresSerie,
  useUtilizacaoColetoresTabela,
} from '../hooks/queries/useIndicadoresGestaoAVista';
import type {
  CubagemMercadoriasRow,
  HorarioCorteRow,
  HorariosCorteImportacaoMensagem,
  IndenizacaoMercadoriasRow,
  IndicadoresGestaoVistaFiltro,
  PerformanceEntregaRow,
  UtilizacaoColetoresRow,
} from '../types/indicadoresGestaoAVista';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { formatarData, formatarDataHora, formatarMoeda, formatarNumero, formatarPorcentagem } from '../utils/formatadores';
import {
  aggregateCubagemRanking,
  aggregateHorariosRanking,
  aggregateIndenizacaoRanking,
  aggregatePerformanceRanking,
  aggregateUtilizacaoRanking,
  avaliarMetaIndicador,
  type GoalMode,
} from '../utils/indicadoresGestaoVistaUi';
import { buildMetaComparisonOption, buildRankingOption } from '../utils/indicadoresGestaoVistaCharts';

type SectionId = 'performance' | 'coletores' | 'cubagem' | 'indenizacao' | 'horarios';

interface GoalConfig {
  threshold: number;
  mode: GoalMode;
  label: string;
}

const GOALS: Record<SectionId, GoalConfig> = {
  performance: { threshold: 95, mode: 'atLeast', label: 'Meta 95%' },
  coletores: { threshold: 80, mode: 'atLeast', label: 'Meta 80%' },
  cubagem: { threshold: 80, mode: 'atLeast', label: 'Meta 80%' },
  indenizacao: { threshold: 0.2, mode: 'atMost', label: 'Limite 0,2%' },
  horarios: { threshold: 90, mode: 'atLeast', label: 'Meta 90%' },
};

function StatusImportacao({ titulo, mensagens, cor }: { titulo: string; mensagens: HorariosCorteImportacaoMensagem[]; cor: 'warn' | 'error' }) {
  if (mensagens.length === 0) return null;
  const style = cor === 'warn'
    ? { border: '#facc15', bg: 'rgba(250, 204, 21, 0.12)', text: '#854d0e' }
    : { border: '#fca5a5', bg: 'rgba(239, 68, 68, 0.08)', text: '#991b1b' };
  return (
    <div className="rounded-xl border px-3 py-3" style={{ borderColor: style.border, backgroundColor: style.bg, color: style.text }}>
      <div className="mb-2 text-xs font-semibold uppercase tracking-wide">{titulo}</div>
      <div className="space-y-1 text-xs">
        {mensagens.slice(0, 6).map((mensagem, index) => (
          <div key={`${mensagem.linha}-${index}`}>
            Linha {mensagem.linha}{mensagem.linhaOuOperacao ? ` · ${mensagem.linhaOuOperacao}` : ''}: {mensagem.mensagem}
          </div>
        ))}
        {mensagens.length > 6 && <div>+{mensagens.length - 6} registro(s) adicional(is).</div>}
      </div>
    </div>
  );
}

function latestUpdatedAt(values: Array<string | null | undefined>) {
  return values
    .filter((value): value is string => Boolean(value))
    .map((value) => ({ value, timestamp: Date.parse(value) }))
    .filter((item) => !Number.isNaN(item.timestamp))
    .sort((a, b) => b.timestamp - a.timestamp)[0]?.value ?? null;
}

function calcularGap(value: number, threshold: number, mode: GoalMode): number {
  if (mode === 'atLeast') {
    return Math.max(threshold - value, 0);
  }
  return Math.max(value - threshold, 0);
}

function formatarGap(gap: number, mode: GoalMode, decimais = 1): string {
  if (gap <= 0) {
    return mode === 'atLeast' ? 'Meta atendida' : 'Dentro do limite';
  }
  const prefixo = mode === 'atLeast' ? 'Gap' : 'Acima do limite';
  return `${prefixo}: ${formatarNumero(gap, decimais)} p.p.`;
}

export default function IndicadoresGestaoAVistaPage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setDataRange, setFiltro, limparFiltros } = useFiltro();
  const filiais = useFiliais();
  const [expandedSection, setExpandedSection] = useState<SectionId | null>(null);
  const [arquivoSelecionado, setArquivoSelecionado] = useState<File | null>(null);
  const arquivoInputRef = useRef<HTMLInputElement | null>(null);
  const filtro: IndicadoresGestaoVistaFiltro = { dataInicio, dataFim, filiais: filtros.filiais };
  const activeFilters: ActiveFilter[] = [{ label: 'Filial base', count: filtros.filiais?.length ?? 0, onRemove: () => setFiltro('filiais', []) }];

  const performanceOverview = usePerformanceEntregaOverview(filtro);
  const performanceSerie = usePerformanceEntregaSerie(filtro);
  const performanceTabela = usePerformanceEntregaTabela(filtro, 150);
  const coletoresOverview = useUtilizacaoColetoresOverview(filtro);
  const coletoresSerie = useUtilizacaoColetoresSerie(filtro);
  const coletoresTabela = useUtilizacaoColetoresTabela(filtro, 180);
  const cubagemOverview = useCubagemMercadoriasOverview(filtro);
  const cubagemSerie = useCubagemMercadoriasSerie(filtro);
  const cubagemTabela = useCubagemMercadoriasTabela(filtro, 150);
  const indenizacaoOverview = useIndenizacaoMercadoriasOverview(filtro);
  const indenizacaoSerie = useIndenizacaoMercadoriasSerie(filtro);
  const indenizacaoTabela = useIndenizacaoMercadoriasTabela(filtro, 150);
  const horariosOverview = useHorariosCorteOverview(filtro);
  const horariosSerie = useHorariosCorteSerie(filtro);
  const horariosTabela = useHorariosCorteTabela(filtro, 200);
  const importacao = useImportarHorariosCorte();

  const updatedAt = latestUpdatedAt([
    performanceOverview.data?.updatedAt,
    coletoresOverview.data?.updatedAt,
    cubagemOverview.data?.updatedAt,
    indenizacaoOverview.data?.updatedAt,
    horariosOverview.data?.updatedAt,
  ]);
  const performanceRanking = useMemo(() => aggregatePerformanceRanking(performanceSerie.data ?? []), [performanceSerie.data]);
  const coletoresRanking = useMemo(() => aggregateUtilizacaoRanking(coletoresSerie.data ?? []), [coletoresSerie.data]);
  const cubagemRanking = useMemo(() => aggregateCubagemRanking(cubagemSerie.data ?? []), [cubagemSerie.data]);
  const indenizacaoRanking = useMemo(() => aggregateIndenizacaoRanking(indenizacaoSerie.data ?? []), [indenizacaoSerie.data]);
  const horariosRanking = useMemo(() => aggregateHorariosRanking(horariosSerie.data ?? []), [horariosSerie.data]);

  const performanceHasData = (performanceOverview.data?.totalEntregas ?? 0) > 0;
  const coletoresHasData = (coletoresOverview.data?.ordensConferencia ?? 0) > 0 || (coletoresOverview.data?.totalManifestos ?? 0) > 0;
  const cubagemHasData = (cubagemOverview.data?.totalFretes ?? 0) > 0;
  const indenizacaoHasData = (indenizacaoOverview.data?.faturamentoBase ?? 0) > 0 || (indenizacaoOverview.data?.totalSinistros ?? 0) > 0;
  const horariosHasData = (horariosOverview.data?.totalProgramado ?? 0) > 0;

  const performanceAssessment = avaliarMetaIndicador({
    value: performanceOverview.data?.pctNoPrazo ?? 0,
    threshold: GOALS.performance.threshold,
    mode: GOALS.performance.mode,
    hasData: performanceHasData,
    isLoading: performanceOverview.isLoading,
    isError: performanceOverview.isError,
  });
  const coletoresAssessment = avaliarMetaIndicador({
    value: coletoresOverview.data?.pctUtilizacao ?? 0,
    threshold: GOALS.coletores.threshold,
    mode: GOALS.coletores.mode,
    hasData: coletoresHasData,
    isLoading: coletoresOverview.isLoading,
    isError: coletoresOverview.isError,
  });
  const cubagemAssessment = avaliarMetaIndicador({
    value: cubagemOverview.data?.pctCubagem ?? 0,
    threshold: GOALS.cubagem.threshold,
    mode: GOALS.cubagem.mode,
    hasData: cubagemHasData,
    isLoading: cubagemOverview.isLoading,
    isError: cubagemOverview.isError,
  });
  const indenizacaoAssessment = avaliarMetaIndicador({
    value: indenizacaoOverview.data?.pctIndenizacao ?? 0,
    threshold: GOALS.indenizacao.threshold,
    mode: GOALS.indenizacao.mode,
    hasData: indenizacaoHasData,
    isLoading: indenizacaoOverview.isLoading,
    isError: indenizacaoOverview.isError,
  });
  const horariosAssessment = avaliarMetaIndicador({
    value: horariosOverview.data?.pctNoHorario ?? 0,
    threshold: GOALS.horarios.threshold,
    mode: GOALS.horarios.mode,
    hasData: horariosHasData,
    isLoading: horariosOverview.isLoading,
    isError: horariosOverview.isError,
  });

  const performanceGap = calcularGap(performanceOverview.data?.pctNoPrazo ?? 0, GOALS.performance.threshold, GOALS.performance.mode);
  const coletoresGap = calcularGap(coletoresOverview.data?.pctUtilizacao ?? 0, GOALS.coletores.threshold, GOALS.coletores.mode);
  const cubagemGap = calcularGap(cubagemOverview.data?.pctCubagem ?? 0, GOALS.cubagem.threshold, GOALS.cubagem.mode);
  const indenizacaoGap = calcularGap(indenizacaoOverview.data?.pctIndenizacao ?? 0, GOALS.indenizacao.threshold, GOALS.indenizacao.mode);
  const horariosGap = calcularGap(horariosOverview.data?.pctNoHorario ?? 0, GOALS.horarios.threshold, GOALS.horarios.mode);

  const performanceChartOption = useMemo(() => (
    performanceRanking.length <= 1
      ? buildMetaComparisonOption({
          label: performanceRanking[0]?.group ?? 'Periodo filtrado',
          value: performanceOverview.data?.pctNoPrazo ?? 0,
          threshold: GOALS.performance.threshold,
          mode: GOALS.performance.mode,
          thresholdLabel: GOALS.performance.label,
        })
      : buildRankingOption({
          items: performanceRanking,
          getLabel: (item) => item.group,
          getValue: (item) => item.pctNoPrazo,
          threshold: GOALS.performance.threshold,
          mode: GOALS.performance.mode,
          thresholdLabel: GOALS.performance.label,
          tooltipLines: (item) => [
            `Total: ${formatarNumero(item.totalEntregas)}`,
            `No prazo: ${formatarNumero(item.entregasNoPrazo)}`,
            `Fora do prazo: ${formatarNumero(item.entregasForaDoPrazo)}`,
            `Sem dado: ${formatarNumero(item.entregasSemDados)}`,
          ],
        })
  ), [performanceRanking, performanceOverview.data?.pctNoPrazo]);

  const coletoresChartOption = useMemo(() => (
    coletoresRanking.length <= 1
      ? buildMetaComparisonOption({
          label: coletoresRanking[0]?.group ?? 'Periodo filtrado',
          value: coletoresOverview.data?.pctUtilizacao ?? 0,
          threshold: GOALS.coletores.threshold,
          mode: GOALS.coletores.mode,
          thresholdLabel: GOALS.coletores.label,
        })
      : buildRankingOption({
          items: coletoresRanking,
          getLabel: (item) => item.group,
          getValue: (item) => item.pctUtilizacao,
          threshold: GOALS.coletores.threshold,
          mode: GOALS.coletores.mode,
          thresholdLabel: GOALS.coletores.label,
          tooltipLines: (item) => [
            `Ordens: ${formatarNumero(item.ordensConferencia)}`,
            `Emitidos: ${formatarNumero(item.manifestosEmitidos)}`,
            `Descarregamento: ${formatarNumero(item.manifestosDescarregamento)}`,
            `Total: ${formatarNumero(item.totalManifestos)}`,
          ],
          max: 140,
        })
  ), [coletoresRanking, coletoresOverview.data?.pctUtilizacao]);

  const cubagemChartOption = useMemo(() => (
    cubagemRanking.length <= 1
      ? buildMetaComparisonOption({
          label: cubagemRanking[0]?.group ?? 'Periodo filtrado',
          value: cubagemOverview.data?.pctCubagem ?? 0,
          threshold: GOALS.cubagem.threshold,
          mode: GOALS.cubagem.mode,
          thresholdLabel: GOALS.cubagem.label,
        })
      : buildRankingOption({
          items: cubagemRanking,
          getLabel: (item) => item.group,
          getValue: (item) => item.pctCubagem,
          threshold: GOALS.cubagem.threshold,
          mode: GOALS.cubagem.mode,
          thresholdLabel: GOALS.cubagem.label,
          tooltipLines: (item) => [
            `Fretes: ${formatarNumero(item.totalFretes)}`,
            `Cubados: ${formatarNumero(item.fretesCubados)}`,
            `Nao cubados: ${formatarNumero(item.fretesNaoCubados)}`,
          ],
        })
  ), [cubagemRanking, cubagemOverview.data?.pctCubagem]);

  const indenizacaoChartOption = useMemo(() => (
    indenizacaoRanking.length <= 1
      ? buildMetaComparisonOption({
          label: indenizacaoRanking[0]?.group ?? 'Periodo filtrado',
          value: indenizacaoOverview.data?.pctIndenizacao ?? 0,
          threshold: GOALS.indenizacao.threshold,
          mode: GOALS.indenizacao.mode,
          thresholdLabel: GOALS.indenizacao.label,
          valueFormatter: (value) => formatarPorcentagem(value, 2),
          axisFormatter: (value) => formatarPorcentagem(value, 2),
        })
      : buildRankingOption({
          items: indenizacaoRanking,
          getLabel: (item) => item.group,
          getValue: (item) => item.pctIndenizacao,
          threshold: GOALS.indenizacao.threshold,
          mode: GOALS.indenizacao.mode,
          thresholdLabel: GOALS.indenizacao.label,
          tooltipLines: (item) => [
            `Valor indenizado: ${formatarMoeda(item.valorIndenizadoAbs)}`,
            `Faturamento base: ${formatarMoeda(item.faturamentoBase)}`,
            `Sinistros: ${formatarNumero(item.totalSinistros)}`,
          ],
          valueFormatter: (value) => formatarPorcentagem(value, 2),
          axisFormatter: (value) => formatarPorcentagem(value, 2),
        })
  ), [indenizacaoRanking, indenizacaoOverview.data?.pctIndenizacao]);

  const horariosChartOption = useMemo(() => (
    horariosRanking.length <= 1
      ? buildMetaComparisonOption({
          label: horariosRanking[0]?.group ?? 'Periodo filtrado',
          value: horariosOverview.data?.pctNoHorario ?? 0,
          threshold: GOALS.horarios.threshold,
          mode: GOALS.horarios.mode,
          thresholdLabel: GOALS.horarios.label,
        })
      : buildRankingOption({
          items: horariosRanking,
          getLabel: (item) => item.group,
          getValue: (item) => item.pctNoHorario,
          threshold: GOALS.horarios.threshold,
          mode: GOALS.horarios.mode,
          thresholdLabel: GOALS.horarios.label,
          tooltipLines: (item) => [
            `Programado: ${formatarNumero(item.totalProgramado)}`,
            `No horario: ${formatarNumero(item.saidasNoHorario)}`,
            `Fora do horario: ${formatarNumero(item.saidasForaDoHorario)}`,
          ],
        })
  ), [horariosRanking, horariosOverview.data?.pctNoHorario]);

  const performanceColumns: ColunaTabela<PerformanceEntregaRow>[] = [
    { chave: 'numeroMinuta', label: 'Minuta', fixo: true },
    { chave: 'dataFrete', label: 'Data Frete', formato: (v) => v ? formatarData(String(v)) : '—' },
    { chave: 'filialEmissora', label: 'Filial Emissora' },
    { chave: 'responsavelRegiaoDestino', label: 'Resp. Região Destino', largura: '220px' },
    { chave: 'previsaoEntrega', label: 'Previsão', formato: (v) => v ? formatarData(String(v)) : '—' },
    { chave: 'dataFinalizacao', label: 'Finalização', formato: (v) => v ? formatarData(String(v)) : '—' },
    { chave: 'performanceDiferencaDias', label: 'Dif. Dias', formato: (v) => v == null ? '—' : formatarNumero(Number(v)) },
    { chave: 'performanceStatus', label: 'Status', formato: (v) => <StatusBadge status={String(v ?? 'SEM DADO')} /> },
  ];
  const coletoresColumns: ColunaTabela<UtilizacaoColetoresRow>[] = [
    { chave: 'date', label: 'Data', formato: (v) => v ? formatarData(String(v)) : '—' },
    { chave: 'filial', label: 'Filial', fixo: true },
    { chave: 'ordensConferencia', label: 'Ordens', formato: (v) => formatarNumero(Number(v ?? 0)) },
    { chave: 'manifestosEmitidos', label: 'Emitidos', formato: (v) => formatarNumero(Number(v ?? 0)) },
    { chave: 'manifestosDescarregamento', label: 'Descarreg.', formato: (v) => formatarNumero(Number(v ?? 0)) },
    { chave: 'totalManifestos', label: 'Total Manifestos', formato: (v) => formatarNumero(Number(v ?? 0)) },
    { chave: 'pctUtilizacao', label: '% Utilização', formato: (v) => formatarPorcentagem(Number(v ?? 0)) },
  ];
  const cubagemColumns: ColunaTabela<CubagemMercadoriasRow>[] = [
    { chave: 'numeroMinuta', label: 'Minuta', fixo: true },
    { chave: 'dataFrete', label: 'Data Frete', formato: (v) => v ? formatarData(String(v)) : '—' },
    { chave: 'filialEmissora', label: 'Filial Emissora' },
    { chave: 'pagador', label: 'Pagador', largura: '220px' },
    { chave: 'destino', label: 'Destino' },
    { chave: 'pesoTaxado', label: 'Peso Taxado', formato: (v) => formatarNumero(Number(v ?? 0), 2) },
    { chave: 'pesoReal', label: 'Peso Real', formato: (v) => formatarNumero(Number(v ?? 0), 2) },
    { chave: 'pesoCubado', label: 'Peso Cubado', formato: (v) => formatarNumero(Number(v ?? 0), 2) },
    { chave: 'totalM3', label: 'Total M3', formato: (v) => formatarNumero(Number(v ?? 0), 3) },
    { chave: 'cubado', label: 'Cubado', formato: (v) => <StatusBadge status={v ? 'SIM' : 'NAO'} /> },
  ];
  const indenizacaoColumns: ColunaTabela<IndenizacaoMercadoriasRow>[] = [
    { chave: 'numeroSinistro', label: 'Sinistro', fixo: true },
    { chave: 'dataAbertura', label: 'Data Abertura', formato: (v) => v ? formatarData(String(v)) : '—' },
    { chave: 'filial', label: 'Filial' },
    { chave: 'minuta', label: 'Minuta', formato: (v) => v == null ? '—' : formatarNumero(Number(v)) },
    { chave: 'resultadoFinalOriginal', label: 'Resultado Original', formato: (v) => formatarMoeda(Number(v ?? 0)) },
    { chave: 'resultadoFinalAbs', label: 'Valor Indenizado', formato: (v) => formatarMoeda(Number(v ?? 0)) },
    { chave: 'pctSobreFaturamentoFilial', label: '% Fat. Filial', formato: (v) => formatarPorcentagem(Number(v ?? 0), 2) },
    { chave: 'ocorrencia', label: 'Ocorrência', largura: '220px' },
    { chave: 'solucao', label: 'Solução', largura: '220px' },
  ];
  const horariosColumns: ColunaTabela<HorarioCorteRow>[] = [
    { chave: 'id', label: 'ID', fixo: true },
    { chave: 'data', label: 'Data', formato: (v) => v ? formatarData(String(v)) : '—' },
    { chave: 'filial', label: 'Filial' },
    { chave: 'linhaOuOperacao', label: 'Linha/Operação', largura: '220px' },
    { chave: 'inicio', label: 'Início' },
    { chave: 'manifestado', label: 'Manifestado' },
    { chave: 'smGerada', label: 'SM Gerada' },
    { chave: 'corte', label: 'Corte' },
    { chave: 'saiuNoHorario', label: 'Status', formato: (v) => <StatusBadge status={v === true ? 'NO PRAZO' : v === false ? 'FORA DO PRAZO' : 'SEM DADO'} /> },
    { chave: 'atrasoMinutos', label: 'Atraso (min)', formato: (v) => v == null ? '—' : formatarNumero(Number(v)) },
    { chave: 'observacao', label: 'Observação', largura: '260px' },
    { chave: 'nomeArquivo', label: 'Arquivo', largura: '220px' },
    { chave: 'importadoEm', label: 'Importado em', formato: (v) => v ? formatarDataHora(String(v)) : '—' },
    { chave: 'importadoPor', label: 'Importado por' },
  ];

  const handleArquivo = (event: ChangeEvent<HTMLInputElement>) => setArquivoSelecionado(event.target.files?.[0] ?? null);
  const handleImportar = async () => {
    if (!arquivoSelecionado || importacao.isPending) return;
    await importacao.mutateAsync(arquivoSelecionado);
    setArquivoSelecionado(null);
    if (arquivoInputRef.current) arquivoInputRef.current.value = '';
  };
  const toggleSection = (sectionId: SectionId) => setExpandedSection((current) => current === sectionId ? null : sectionId);

  const importBox = (
    <div className="mb-5 rounded-[20px] border p-4 shadow-sm" style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)' }}>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="space-y-2">
          <div className="flex items-center gap-2 text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
            <FileUp size={17} />
            Operação de importação
          </div>
          <div className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
            Última importação: {horariosOverview.data?.ultimaImportacaoEm ? formatarDataHora(horariosOverview.data.ultimaImportacaoEm) : '—'}
          </div>
          <div className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
            Arquivo mais recente: {horariosOverview.data?.ultimaImportacaoArquivo ?? '—'}
          </div>
          <div className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
            Regra: `SM Gerada` versus `Corte`, com ajuste de virada de dia pela coluna `Início`.
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <a href="/templates/horarios-corte-modelo.xlsx" className="rounded-xl border px-3 py-2 text-sm font-medium" style={{ borderColor: 'var(--color-border)', color: 'var(--color-text)' }}>
            Baixar modelo
          </a>
          <label className="cursor-pointer rounded-xl border px-3 py-2 text-sm font-medium" style={{ borderColor: 'var(--color-border)', color: 'var(--color-text)' }}>
            Selecionar planilha
            <input ref={arquivoInputRef} type="file" accept=".xlsx" className="hidden" onChange={handleArquivo} />
          </label>
          <button
            type="button"
            onClick={handleImportar}
            disabled={!arquivoSelecionado || importacao.isPending}
            className="rounded-xl px-4 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
            style={{ backgroundColor: 'var(--color-primary)' }}
          >
            {importacao.isPending ? 'Importando...' : 'Importar'}
          </button>
        </div>
      </div>

      <div className="mt-3 text-xs" style={{ color: 'var(--color-text-subtle)' }}>
        {arquivoSelecionado ? `Arquivo selecionado: ${arquivoSelecionado.name}` : 'Nenhum arquivo selecionado.'}
      </div>

      {importacao.isError && (
        <div className="mt-3">
          <MensagemErro mensagem={getApiErrorMessage(importacao.error, 'Falha ao importar a planilha de horários de corte.')} tipo={getTipoErro(importacao.error)} />
        </div>
      )}

      {importacao.data && (
        <div className="mt-3 space-y-3">
          <div className="rounded-xl border px-3 py-3" style={{ borderColor: 'var(--color-border)' }}>
            <div className="mb-1 flex items-center gap-2 text-sm font-semibold" style={{ color: 'var(--color-text)' }}>
              <AlertCircle size={15} />
              Resultado da importação
            </div>
            <div className="grid grid-cols-1 gap-2 text-xs md:grid-cols-3" style={{ color: 'var(--color-text-muted)' }}>
              <div>Arquivo: {importacao.data.arquivo}</div>
              <div>Importado em: {formatarDataHora(importacao.data.importadoEm)}</div>
              <div>Processadas: {formatarNumero(importacao.data.linhasProcessadas)}</div>
              <div>Importadas: {formatarNumero(importacao.data.linhasImportadas)}</div>
              <div>Substituídas: {formatarNumero(importacao.data.linhasSubstituidas)}</div>
              <div>Ignoradas: {formatarNumero(importacao.data.linhasIgnoradas)}</div>
            </div>
          </div>
          <StatusImportacao titulo="Avisos" mensagens={importacao.data.avisos} cor="warn" />
          <StatusImportacao titulo="Rejeições" mensagens={importacao.data.rejeicoes} cor="error" />
        </div>
      )}
    </div>
  );

  const performanceForaPrazo = Math.max((performanceOverview.data?.totalEntregas ?? 0) - (performanceOverview.data?.entregasNoPrazo ?? 0) - (performanceOverview.data?.entregasSemDados ?? 0), 0);
  const cubagemNaoCubados = Math.max((cubagemOverview.data?.totalFretes ?? 0) - (cubagemOverview.data?.fretesCubados ?? 0), 0);
  const horariosForaHorario = Math.max((horariosOverview.data?.totalProgramado ?? 0) - (horariosOverview.data?.saidasNoHorario ?? 0), 0);

  return (
    <div className="w-full">
      <div className="mb-5 flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold leading-tight" style={{ color: 'var(--color-text)' }}>Indicadores de Gestão à Vista</h1>
          <p className="text-sm" style={{ color: 'var(--color-text-subtle)' }}>Painel operacional diário com leitura rápida por filial e tabela analítica sob demanda.</p>
        </div>
        <LastUpdated dataExtracao={updatedAt} />
      </div>
      <FilterBar onClear={limparFiltros} activeFilters={activeFilters} dataInicio={dataInicio} dataFim={dataFim}>
        <DateRangePicker dataInicio={dataInicio} dataFim={dataFim} onDataInicioChange={setDataInicio} onDataFimChange={setDataFim} onRangeChange={setDataRange} />
        <AsyncMultiSelect label="Filial base" opcoes={filiais.data ?? []} selecionados={filtros.filiais ?? []} onChange={(valores) => setFiltro('filiais', valores)} isLoading={filiais.isLoading} />
      </FilterBar>

      <div className="mb-6 grid grid-cols-1 gap-4 xl:grid-cols-5">
        <IndicadoresGestaoSummaryCard
          title="Performance de Entrega"
          description="Pontualidade consolidada do período."
          value={formatarPorcentagem(performanceOverview.data?.pctNoPrazo ?? 0)}
          detail={`${formatarNumero(performanceOverview.data?.entregasNoPrazo ?? 0)} no prazo de ${formatarNumero(performanceOverview.data?.totalEntregas ?? 0)} · ${formatarGap(performanceGap, GOALS.performance.mode)}`}
          goalLabel={GOALS.performance.label}
          statusLabel={performanceAssessment.label}
          tone={performanceAssessment.tone}
          progressPct={performanceAssessment.progressPct}
          icon={<Truck size={18} />}
        />
        <IndicadoresGestaoSummaryCard
          title="Utilização dos Coletores"
          description="Aderência operacional da conferência."
          value={formatarPorcentagem(coletoresOverview.data?.pctUtilizacao ?? 0)}
          detail={`${formatarNumero(coletoresOverview.data?.ordensConferencia ?? 0)} ordens sobre ${formatarNumero(coletoresOverview.data?.totalManifestos ?? 0)} manifestos · ${formatarGap(coletoresGap, GOALS.coletores.mode)}`}
          goalLabel={GOALS.coletores.label}
          statusLabel={coletoresAssessment.label}
          tone={coletoresAssessment.tone}
          progressPct={coletoresAssessment.progressPct}
          icon={<Boxes size={18} />}
        />
        <IndicadoresGestaoSummaryCard
          title="Cubagem de Mercadorias"
          description="Cobertura operacional da cubagem."
          value={formatarPorcentagem(cubagemOverview.data?.pctCubagem ?? 0)}
          detail={`${formatarNumero(cubagemOverview.data?.fretesCubados ?? 0)} cubados de ${formatarNumero(cubagemOverview.data?.totalFretes ?? 0)} · ${formatarGap(cubagemGap, GOALS.cubagem.mode)}`}
          goalLabel={GOALS.cubagem.label}
          statusLabel={cubagemAssessment.label}
          tone={cubagemAssessment.tone}
          progressPct={cubagemAssessment.progressPct}
          icon={<PackageCheck size={18} />}
        />
        <IndicadoresGestaoSummaryCard
          title="Indenização de Mercadorias"
          description="Peso da indenização sobre o faturamento."
          value={formatarPorcentagem(indenizacaoOverview.data?.pctIndenizacao ?? 0, 2)}
          detail={`${formatarMoeda(indenizacaoOverview.data?.valorIndenizadoAbs ?? 0)} indenizados · ${formatarGap(indenizacaoGap, GOALS.indenizacao.mode, 2)}`}
          goalLabel={GOALS.indenizacao.label}
          statusLabel={indenizacaoAssessment.label}
          tone={indenizacaoAssessment.tone}
          progressPct={indenizacaoAssessment.progressPct}
          icon={<ShieldAlert size={18} />}
        />
        <IndicadoresGestaoSummaryCard
          title="Horários de Corte"
          description="Pontualidade das saídas programadas."
          value={formatarPorcentagem(horariosOverview.data?.pctNoHorario ?? 0)}
          detail={`${formatarNumero(horariosOverview.data?.saidasNoHorario ?? 0)} no horário de ${formatarNumero(horariosOverview.data?.totalProgramado ?? 0)} · ${formatarGap(horariosGap, GOALS.horarios.mode)}`}
          goalLabel={GOALS.horarios.label}
          statusLabel={horariosAssessment.label}
          tone={horariosAssessment.tone}
          progressPct={horariosAssessment.progressPct}
          icon={<Clock3 size={18} />}
        />
      </div>

      <IndicadoresGestaoSection
        title="Performance de Entrega"
        description="Piores regiões por pontualidade, usando a regra oficial de entrega no prazo."
        goalLabel={GOALS.performance.label}
        goalTone={performanceAssessment.tone}
        error={performanceOverview.error}
        kpis={[
          { label: 'Total de Entregas', value: formatarNumero(performanceOverview.data?.totalEntregas ?? 0), icon: <Truck size={16} />, progressPct: performanceAssessment.progressPct },
          { label: 'Entregas Fora do Prazo', value: formatarNumero(performanceForaPrazo), icon: <AlertCircle size={16} />, progressPct: performanceAssessment.progressPct },
          { label: 'Sem dado suficiente', value: formatarNumero(performanceOverview.data?.entregasSemDados ?? 0), icon: <AlertCircle size={16} />, progressPct: performanceAssessment.progressPct },
          { label: 'Gap vs meta 95%', value: formatarGap(performanceGap, GOALS.performance.mode), icon: <Gauge size={16} />, progressPct: performanceAssessment.progressPct },
        ]}
        chartTitle={performanceRanking.length <= 1 ? 'Comparativo contra meta' : 'Piores regiões por pontualidade'}
        chartOption={performanceChartOption}
        chartLoading={performanceSerie.isLoading}
        chartEmpty={performanceRanking.length === 0}
        chartError={performanceSerie.isError ? getApiErrorMessage(performanceSerie.error, 'Erro ao carregar gráfico.') : null}
        exportName="indicadores-gestao-a-vista-performance-entrega"
        tableTitle="Entregas Analíticas"
        tableData={performanceTabela.data ?? []}
        tableColumns={performanceColumns}
        rowKey="numeroMinuta"
        tableLoading={performanceTabela.isLoading}
        isExpanded={expandedSection === 'performance'}
        onToggleTable={() => toggleSection('performance')}
      />

      <IndicadoresGestaoSection
        title="Utilização dos Coletores"
        description="Filiais com menor utilização operacional de coletores no período."
        goalLabel={GOALS.coletores.label}
        goalTone={coletoresAssessment.tone}
        error={coletoresOverview.error}
        kpis={[
          { label: 'Ordens de Conferência', value: formatarNumero(coletoresOverview.data?.ordensConferencia ?? 0), icon: <Boxes size={16} />, progressPct: coletoresAssessment.progressPct },
          { label: 'Total de Manifestos', value: formatarNumero(coletoresOverview.data?.totalManifestos ?? 0), icon: <BarChart3 size={16} />, progressPct: coletoresAssessment.progressPct },
          { label: 'Manifestos de Descarreg.', value: formatarNumero(coletoresOverview.data?.manifestosDescarregamento ?? 0), icon: <Truck size={16} />, progressPct: coletoresAssessment.progressPct },
          { label: 'Gap vs meta 80%', value: formatarGap(coletoresGap, GOALS.coletores.mode), icon: <Gauge size={16} />, progressPct: coletoresAssessment.progressPct },
        ]}
        chartTitle={coletoresRanking.length <= 1 ? 'Comparativo contra meta' : 'Filiais com menor utilização'}
        chartOption={coletoresChartOption}
        chartLoading={coletoresSerie.isLoading}
        chartEmpty={coletoresRanking.length === 0}
        chartError={coletoresSerie.isError ? getApiErrorMessage(coletoresSerie.error, 'Erro ao carregar gráfico.') : null}
        exportName="indicadores-gestao-a-vista-utilizacao-coletores"
        tableTitle="Coletores por Data e Filial"
        tableData={coletoresTabela.data ?? []}
        tableColumns={coletoresColumns}
        rowKey="chave"
        tableLoading={coletoresTabela.isLoading}
        isExpanded={expandedSection === 'coletores'}
        onToggleTable={() => toggleSection('coletores')}
      />

      <IndicadoresGestaoSection
        title="Cubagem de Mercadorias"
        description="Filiais com menor cubagem, considerando a regra oficial por `Total M3` ou `Peso Cubado`."
        goalLabel={GOALS.cubagem.label}
        goalTone={cubagemAssessment.tone}
        error={cubagemOverview.error}
        alert={<div className="mb-4 rounded-xl border border-dashed px-3 py-3 text-xs" style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-muted)' }}>A exclusão por lista oficial de CNPJs ainda não está aplicada nesta versão.</div>}
        kpis={[
          { label: 'Fretes Cubados', value: formatarNumero(cubagemOverview.data?.fretesCubados ?? 0), icon: <PackageCheck size={16} />, progressPct: cubagemAssessment.progressPct },
          { label: 'Fretes Não Cubados', value: formatarNumero(cubagemNaoCubados), icon: <AlertCircle size={16} />, progressPct: cubagemAssessment.progressPct },
          { label: 'Fretes com Peso Real', value: formatarNumero(cubagemOverview.data?.fretesComPesoReal ?? 0), icon: <BarChart3 size={16} />, progressPct: cubagemAssessment.progressPct },
          { label: 'Gap vs meta 80%', value: formatarGap(cubagemGap, GOALS.cubagem.mode), icon: <Gauge size={16} />, progressPct: cubagemAssessment.progressPct },
        ]}
        chartTitle={cubagemRanking.length <= 1 ? 'Comparativo contra meta' : 'Filiais com menor cubagem'}
        chartOption={cubagemChartOption}
        chartLoading={cubagemSerie.isLoading}
        chartEmpty={cubagemRanking.length === 0}
        chartError={cubagemSerie.isError ? getApiErrorMessage(cubagemSerie.error, 'Erro ao carregar gráfico.') : null}
        exportName="indicadores-gestao-a-vista-cubagem-mercadorias"
        tableTitle="Cubagem Analítica"
        tableData={cubagemTabela.data ?? []}
        tableColumns={cubagemColumns}
        rowKey="numeroMinuta"
        tableLoading={cubagemTabela.isLoading}
        isExpanded={expandedSection === 'cubagem'}
        onToggleTable={() => toggleSection('cubagem')}
      />

      <IndicadoresGestaoSection
        title="Indenização de Mercadorias"
        description="Filiais com maior impacto percentual de indenização sobre o faturamento."
        goalLabel={GOALS.indenizacao.label}
        goalTone={indenizacaoAssessment.tone}
        error={indenizacaoOverview.error}
        kpis={[
          { label: 'Valor Indenizado', value: formatarMoeda(indenizacaoOverview.data?.valorIndenizadoAbs ?? 0), icon: <ShieldAlert size={16} />, progressPct: indenizacaoAssessment.progressPct },
          { label: 'Total de Sinistros', value: formatarNumero(indenizacaoOverview.data?.totalSinistros ?? 0), icon: <ShieldAlert size={16} />, progressPct: indenizacaoAssessment.progressPct },
          { label: 'Faturamento Base', value: formatarMoeda(indenizacaoOverview.data?.faturamentoBase ?? 0), icon: <BarChart3 size={16} />, progressPct: indenizacaoAssessment.progressPct },
          { label: 'Acima do limite 0,2%', value: formatarGap(indenizacaoGap, GOALS.indenizacao.mode, 2), icon: <Gauge size={16} />, progressPct: indenizacaoAssessment.progressPct },
        ]}
        chartTitle={indenizacaoRanking.length <= 1 ? 'Comparativo contra limite' : 'Filiais com maior impacto de indenização'}
        chartOption={indenizacaoChartOption}
        chartLoading={indenizacaoSerie.isLoading}
        chartEmpty={indenizacaoRanking.length === 0}
        chartError={indenizacaoSerie.isError ? getApiErrorMessage(indenizacaoSerie.error, 'Erro ao carregar gráfico.') : null}
        exportName="indicadores-gestao-a-vista-indenizacao-mercadorias"
        tableTitle="Sinistros Analíticos"
        tableData={indenizacaoTabela.data ?? []}
        tableColumns={indenizacaoColumns}
        rowKey="numeroSinistro"
        tableLoading={indenizacaoTabela.isLoading}
        isExpanded={expandedSection === 'indenizacao'}
        onToggleTable={() => toggleSection('indenizacao')}
      />

      <IndicadoresGestaoSection
        title="Horários de Corte"
        description="Filiais com menor pontualidade de saída a partir da planilha operacional manual."
        goalLabel={GOALS.horarios.label}
        goalTone={horariosAssessment.tone}
        error={horariosOverview.error}
        extra={importBox}
        kpis={[
          { label: 'Saídas no horário', value: formatarNumero(horariosOverview.data?.saidasNoHorario ?? 0), icon: <Truck size={16} />, progressPct: horariosAssessment.progressPct },
          { label: 'Saídas fora do horário', value: formatarNumero(horariosForaHorario), icon: <AlertCircle size={16} />, progressPct: horariosAssessment.progressPct },
          { label: 'Total programado', value: formatarNumero(horariosOverview.data?.totalProgramado ?? 0), icon: <BarChart3 size={16} />, progressPct: horariosAssessment.progressPct },
          { label: 'Gap vs meta 90%', value: formatarGap(horariosGap, GOALS.horarios.mode), icon: <Gauge size={16} />, progressPct: horariosAssessment.progressPct },
        ]}
        chartTitle={horariosRanking.length <= 1 ? 'Comparativo contra meta' : 'Filiais com menor pontualidade de saída'}
        chartOption={horariosChartOption}
        chartLoading={horariosSerie.isLoading}
        chartEmpty={horariosRanking.length === 0}
        chartError={horariosSerie.isError ? getApiErrorMessage(horariosSerie.error, 'Erro ao carregar gráfico.') : null}
        exportName="indicadores-gestao-a-vista-horarios-corte"
        tableTitle="Horários de Corte Analíticos"
        tableData={horariosTabela.data ?? []}
        tableColumns={horariosColumns}
        rowKey="id"
        tableLoading={horariosTabela.isLoading}
        isExpanded={expandedSection === 'horarios'}
        onToggleTable={() => toggleSection('horarios')}
      />
    </div>
  );
}
