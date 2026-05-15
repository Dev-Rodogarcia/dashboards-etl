import type { EChartsOption } from 'echarts';
import ChartWrapper from '../components/charts/ChartWrapper';
import ExecutivoKpiGrid from '../components/domain/executivo/ExecutivoKpiGrid';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import DateRangePicker from '../components/shared/DateRangePicker';
import FilterBar, { type ActiveFilter } from '../components/shared/FilterBar';
import MensagemErro from '../components/ui/MensagemErro';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { useFiltro } from '../contexts/FiltroContext';
import { usePageHeader } from '../contexts/PageHeaderContext';
import { useFiliais } from '../hooks/queries/useDimensoes';
import { useExecutivoOverview, useExecutivoSerie } from '../hooks/queries/useExecutivo';
import { formatarMoeda } from '../utils/formatadores';

function formatarMoedaEixo(valor: number | string) {
  const numero = Number(valor);
  if (!Number.isFinite(numero)) return '';
  if (Math.abs(numero) >= 1_000_000) return `${formatarMoeda(numero / 1_000_000)} mi`;
  if (Math.abs(numero) >= 1_000) return `${formatarMoeda(numero / 1_000)} mil`;
  return formatarMoeda(numero);
}

export default function ExecutivoPage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setDataRange, setFiltro, limparFiltros } = useFiltro();
  const filiais = useFiliais();

  const filtro = {
    dataInicio,
    dataFim,
    filiais: filtros.filiais,
  };

  const activeFilters: ActiveFilter[] = [
    { label: 'Filiais', count: filtros.filiais?.length ?? 0, onRemove: () => setFiltro('filiais', []) },
  ];

  const overview = useExecutivoOverview(filtro);
  const serie = useExecutivoSerie(filtro);

  usePageHeader({
    title: 'Executivo',
    description: 'Visão consolidada da operação, financeiro e backlog.',
    updatedAt: overview.data?.updatedAt ?? null,
  });

  const serieDados = serie.data ?? [];
  const erroSerie = serie.isError ? getApiErrorMessage(serie.error, 'Erro ao carregar série executiva.') : null;

  const financeiroOption: EChartsOption = {
    legend: { bottom: 0 },
    tooltip: { trigger: 'axis' },
    grid: { top: 34, right: 20, bottom: 46, left: 68 },
    xAxis: { type: 'category', data: serieDados.map((item) => item.month) },
    yAxis: { type: 'value', axisLabel: { formatter: formatarMoedaEixo } },
    series: [
      { name: 'Receita Operacional', type: 'line', smooth: true, symbolSize: 7, data: serieDados.map((item) => item.receitaOperacional) },
      { name: 'Valor Faturado', type: 'line', smooth: true, symbolSize: 7, data: serieDados.map((item) => item.valorFaturado) },
      { name: 'Saldo a Receber', type: 'line', smooth: true, symbolSize: 7, data: serieDados.map((item) => item.saldoAReceber) },
      { name: 'Saldo a Pagar', type: 'line', smooth: true, symbolSize: 7, data: serieDados.map((item) => item.saldoAPagar) },
    ],
  };

  const backlogOption: EChartsOption = {
    tooltip: { trigger: 'axis' },
    grid: { top: 26, right: 18, bottom: 32, left: 42 },
    xAxis: { type: 'category', data: serieDados.map((item) => item.month) },
    yAxis: { type: 'value' },
    series: [
      {
        name: 'Backlog Coletas',
        type: 'bar',
        barMaxWidth: 30,
        data: serieDados.map((item) => item.backlogColetas),
      },
    ],
  };

  return (
    <div className="w-full">
      <FilterBar onClear={limparFiltros} activeFilters={activeFilters} dataInicio={dataInicio} dataFim={dataFim}>
        <DateRangePicker dataInicio={dataInicio} dataFim={dataFim} onDataInicioChange={setDataInicio} onDataFimChange={setDataFim} onRangeChange={setDataRange} />
        <AsyncMultiSelect label="Filiais" opcoes={filiais.data ?? []} selecionados={filtros.filiais ?? []} onChange={(valores) => setFiltro('filiais', valores)} isLoading={filiais.isLoading} />
      </FilterBar>

      {overview.isError && <MensagemErro mensagem={getApiErrorMessage(overview.error, 'Erro ao carregar visão executiva.')} tipo={getTipoErro(overview.error)} />}
      {overview.isLoading && (
        <div className="mb-6 flex h-24 items-center justify-center">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-t-transparent" style={{ borderColor: 'var(--color-primary)', borderTopColor: 'transparent' }} />
        </div>
      )}
      {!overview.isLoading && overview.data && <ExecutivoKpiGrid overview={overview.data} />}
      {!overview.isLoading && !overview.data && !overview.isError && (
        <div className="mb-6 flex h-24 items-center justify-center rounded-[20px] border text-sm" style={{ color: 'var(--color-text-muted)', backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}>
          Nenhum dado disponível para o período selecionado.
        </div>
      )}

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper
          titulo="Tendência Financeira"
          option={financeiroOption}
          isLoading={serie.isLoading}
          isEmpty={serieDados.length === 0}
          erro={erroSerie}
          altura={360}
        />
        <ChartWrapper
          titulo="Backlog Mensal"
          option={backlogOption}
          isLoading={serie.isLoading}
          isEmpty={serieDados.length === 0}
          erro={erroSerie}
          altura={360}
        />
      </div>
    </div>
  );
}
