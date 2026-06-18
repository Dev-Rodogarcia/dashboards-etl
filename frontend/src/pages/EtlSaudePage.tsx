import { useEffect, useMemo } from 'react';
import type { EChartsOption } from 'echarts';
import ChartWrapper from '../components/charts/ChartWrapper';
import { useEchartsTheme } from '../components/charts/useEchartsTheme';
import EtlSaudeKpiGrid from '../components/domain/etlSaude/EtlSaudeKpiGrid';
import DataTable, { type ColunaTabela } from '../components/shared/DataTable';
import DateRangePicker from '../components/shared/DateRangePicker';
import { DATE_RANGE_PRESETS } from '../components/shared/dateRangePresets';
import ExportButton from '../components/shared/ExportButton';
import FilterBar from '../components/shared/FilterBar';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import { exportarEtlSaudeCsv } from '../api/endpoints/etlSaudeServico';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { useFiltro } from '../contexts/FiltroContext';
import { usePageHeader } from '../contexts/PageHeaderContext';
import { useEtlSaudeOverview, useEtlSaudeSerie, useEtlSaudeTabela } from '../hooks/queries/useEtlSaude';
import { normalizarPeriodo } from '../utils/dateUtils';
import { buildBaseBarOption, buildBaseLineOption, getEchartsThemeTokens } from '../utils/echartsBuilders';
import { formatarDataHora, formatarNumero, formatarPorcentagem } from '../utils/formatadores';
import type { EtlLogExtracaoAuditoriaRow } from '../types/etlSaude';

type TooltipParam = {
  axisValue?: string | number;
  axisValueLabel?: string;
  marker?: string;
  seriesName?: string;
  value?: number | string | Array<number | string | null> | null;
};

function formatarDuracao(valor: unknown) {
  if (typeof valor !== 'number' || !Number.isFinite(valor)) {
    return '—';
  }

  const totalSegundos = Math.round(valor);
  const horas = Math.floor(totalSegundos / 3600);
  const minutos = Math.floor((totalSegundos % 3600) / 60);
  const segundos = totalSegundos % 60;

  if (horas > 0) {
    return `${horas}h ${minutos}min ${segundos}s`;
  }
  if (minutos > 0) {
    return `${minutos}min ${segundos}s`;
  }
  return `${segundos}s`;
}

function formatarInteiro(valor: unknown) {
  return typeof valor === 'number' && Number.isFinite(valor) ? formatarNumero(valor) : '—';
}

function normalizarTooltipParams(params: unknown): TooltipParam[] {
  return Array.isArray(params) ? params as TooltipParam[] : [params as TooltipParam];
}

function valorTooltip(param: TooltipParam): number {
  const valor = Array.isArray(param.value) ? param.value[param.value.length - 1] : param.value;
  const numero = Number(valor ?? 0);
  return Number.isFinite(numero) ? numero : 0;
}

function formatarTooltipVolume(params: unknown) {
  const itens = normalizarTooltipParams(params);
  const titulo = itens[0]?.axisValueLabel ?? itens[0]?.axisValue ?? '';
  const linhas = itens.map((item) => {
    const nomeSerie = item.seriesName ?? '';
    const valor = valorTooltip(item);
    const valorFormatado = nomeSerie === 'Duração Média'
      ? formatarDuracao(valor)
      : formatarNumero(valor);

    return `${item.marker ?? ''}${nomeSerie}: ${valorFormatado}`;
  });

  return [titulo, ...linhas].filter(Boolean).join('<br/>');
}

export default function EtlSaudePage() {
  const { dataInicio, dataFim, setDataInicio, setDataFim, setDataRange, limparFiltros } = useFiltro();
  const { isDark } = useEchartsTheme();
  const filtro = { dataInicio, dataFim };

  useEffect(() => {
    const preset180d = DATE_RANGE_PRESETS.find((preset) => preset.label === '180d');
    if (!preset180d) return;

    const range = preset180d.getRange();
    const periodo = normalizarPeriodo(range.dataInicio, range.dataFim);
    setDataRange(periodo.dataInicio, periodo.dataFim);
    // Esta excecao de periodo inicial deve rodar somente na montagem da pagina.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const overview = useEtlSaudeOverview(filtro);
  const serie = useEtlSaudeSerie(filtro);
  const tabela = useEtlSaudeTabela(filtro);

  usePageHeader({
    title: 'Saúde do ETL',
    description: 'Taxas de sucesso/falha, volume diário e auditoria crua das extrações.',
    updatedAt: overview.data?.updatedAt ?? null,
  });

  const serieDados = useMemo(() => serie.data ?? [], [serie.data]);
  const erroSerie = serie.isError ? getApiErrorMessage(serie.error, 'Erro ao carregar série do ETL.') : null;
  const auditoriaDados = tabela.data ?? [];
  const taxasDiarias = useMemo(() => serieDados.map((item) => {
    const totalExecucoes = Math.max(item.execucoes, 0);
    const falhas = Math.max(item.erros, 0);
    const sucessos = Math.max(totalExecucoes - falhas, 0);

    return {
      date: item.date,
      taxaSucesso: totalExecucoes > 0 ? (sucessos * 100) / totalExecucoes : 0,
      taxaFalha: totalExecucoes > 0 ? (falhas * 100) / totalExecucoes : 0,
    };
  }), [serieDados]);

  const taxasOption: EChartsOption = useMemo(() => {
    const tokens = getEchartsThemeTokens(isDark);

    return buildBaseLineOption(isDark, {
      legend: { bottom: 0 },
      tooltip: {
        trigger: 'axis',
        valueFormatter: (value) => formatarPorcentagem(Number(value ?? 0), 1),
      },
      grid: { top: 34, right: 36, bottom: 46, left: 42, containLabel: true },
      xAxis: { type: 'category', data: taxasDiarias.map((item) => item.date) },
      yAxis: {
        type: 'value',
        max: 100,
        axisLabel: { formatter: (value: number | string) => formatarPorcentagem(Number(value), 0) },
      },
      series: [
        {
          name: 'Sucesso',
          type: 'line',
          smooth: true,
          symbolSize: 7,
          data: taxasDiarias.map((item) => Number(item.taxaSucesso.toFixed(1))),
          itemStyle: { color: tokens.palette[2] },
          lineStyle: { color: tokens.palette[2], width: 2 },
        },
        {
          name: 'Falha',
          type: 'line',
          smooth: true,
          symbolSize: 7,
          data: taxasDiarias.map((item) => Number(item.taxaFalha.toFixed(1))),
          itemStyle: { color: tokens.palette[3] },
          lineStyle: { color: tokens.palette[3], width: 2 },
        },
      ],
    });
  }, [isDark, taxasDiarias]);

  const volumeOption: EChartsOption = useMemo(() => {
    const tokens = getEchartsThemeTokens(isDark);

    return buildBaseLineOption(isDark, buildBaseBarOption(isDark, {
      legend: { top: 0 },
      tooltip: {
        trigger: 'axis',
        formatter: formatarTooltipVolume,
      },
      grid: { top: 54, right: 62, bottom: 46, left: 42, containLabel: true },
      xAxis: { type: 'category', data: serieDados.map((item) => item.date) },
      yAxis: [
        { type: 'value', name: 'Registros' },
        {
          type: 'value',
          name: 'Duração',
          alignTicks: true,
          splitLine: { show: false },
          axisLabel: { formatter: (value: number | string) => formatarDuracao(Number(value)) },
        },
      ],
      series: [
        {
          name: 'Registros',
          type: 'bar',
          yAxisIndex: 0,
          data: serieDados.map((item) => item.volumeProcessado),
          itemStyle: { color: tokens.palette[0] },
        },
        {
          name: 'Duração Média',
          type: 'line',
          yAxisIndex: 1,
          smooth: true,
          symbolSize: 7,
          data: serieDados.map((item) => item.duracaoMedia),
          itemStyle: { color: tokens.palette[8] },
          lineStyle: { color: tokens.palette[8], width: 2.5 },
          emphasis: { focus: 'series' },
        },
      ],
    }));
  }, [isDark, serieDados]);

  const colunas: ColunaTabela<EtlLogExtracaoAuditoriaRow>[] = [
    {
      chave: 'id',
      label: 'ID',
      fixo: true,
      largura: '96px',
      formato: formatarInteiro,
    },
    { chave: 'entidade', label: 'Entidade', largura: '180px' },
    {
      chave: 'timestampInicio',
      label: 'Início',
      largura: '180px',
      formato: (valor) => (typeof valor === 'string' ? formatarDataHora(valor) : '—'),
    },
    {
      chave: 'timestampFim',
      label: 'Fim',
      largura: '180px',
      formato: (valor) => (typeof valor === 'string' ? formatarDataHora(valor) : '—'),
    },
    {
      chave: 'statusFinal',
      label: 'Status Final',
      largura: '150px',
      formato: (valor) => <StatusBadge status={typeof valor === 'string' ? valor : 'Sem status'} />,
    },
    { chave: 'registrosExtraidos', label: 'Registros Extraídos', largura: '170px', formato: formatarInteiro },
    { chave: 'paginasProcessadas', label: 'Páginas Processadas', largura: '170px', formato: formatarInteiro },
    { chave: 'noopCount', label: 'No-op', largura: '110px', formato: formatarInteiro },
    {
      chave: 'mensagem',
      label: 'Mensagem',
      largura: '520px',
      formato: (valor) => (
        <span className="block max-w-[520px] whitespace-normal leading-relaxed">
          {typeof valor === 'string' && valor.trim() ? valor : '—'}
        </span>
      ),
    },
  ];

  return (
    <div className="w-full">
      <FilterBar onClear={limparFiltros} dataInicio={dataInicio} dataFim={dataFim}>
        <DateRangePicker dataInicio={dataInicio} dataFim={dataFim} onDataInicioChange={setDataInicio} onDataFimChange={setDataFim} onRangeChange={setDataRange} />
      </FilterBar>

      {overview.isError && <MensagemErro mensagem={getApiErrorMessage(overview.error, 'Erro ao carregar indicadores do ETL.')} tipo={getTipoErro(overview.error)} />}
      {overview.data && <EtlSaudeKpiGrid overview={overview.data} />}

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper
          titulo="Taxas de Sucesso/Falha por Dia"
          chartKey="etlTaxasDiarias"
          option={taxasOption}
          isLoading={serie.isLoading}
          isEmpty={serieDados.length === 0}
          erro={erroSerie}
          altura={320}
        />
        <ChartWrapper
          titulo="Volumetria de Registros por Dia"
          chartKey="etlVolumeDiario"
          option={volumeOption}
          isLoading={serie.isLoading}
          isEmpty={serieDados.length === 0}
          erro={erroSerie}
          altura={320}
        />
      </div>

      <DataTable
        titulo="Auditoria crua de extrações"
        dados={auditoriaDados}
        colunas={colunas}
        chaveLinha="id"
        isLoading={tabela.isLoading}
        error={tabela.error}
        errorFallbackMessage="Erro ao carregar auditoria crua do ETL."
        acoesCabecalho={(
          <ExportButton
            nomeArquivo="etl-saude"
            onExport={!tabela.isLoading && !tabela.error ? () => exportarEtlSaudeCsv(filtro) : undefined}
          />
        )}
      />
    </div>
  );
}
