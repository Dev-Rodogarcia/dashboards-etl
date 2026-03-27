import type { EChartsOption } from 'echarts';
import ChartWrapper from '../components/charts/ChartWrapper';
import ExecutivoKpiGrid from '../components/domain/executivo/ExecutivoKpiGrid';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import DateRangePicker from '../components/shared/DateRangePicker';
import FilterBar, { type ActiveFilter } from '../components/shared/FilterBar';
import LastUpdated from '../components/shared/LastUpdated';
import MensagemErro from '../components/ui/MensagemErro';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { useFiltro } from '../contexts/FiltroContext';
import { useFiliais } from '../hooks/queries/useDimensoes';
import { useExecutivoOverview, useExecutivoSerie } from '../hooks/queries/useExecutivo';

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

  const option: EChartsOption = {
    legend: { bottom: 0 },
    xAxis: { type: 'category', data: (serie.data ?? []).map((item) => item.month) },
    yAxis: { type: 'value' },
    series: [
      { name: 'Receita Operacional', type: 'line', data: (serie.data ?? []).map((item) => item.receitaOperacional) },
      { name: 'Valor Faturado', type: 'line', data: (serie.data ?? []).map((item) => item.valorFaturado) },
      { name: 'Saldo a Receber', type: 'line', data: (serie.data ?? []).map((item) => item.saldoAReceber) },
      { name: 'Saldo a Pagar', type: 'line', data: (serie.data ?? []).map((item) => item.saldoAPagar) },
    ],
  };

  return (
    <div className="w-full">
      <div className="mb-5 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold leading-tight" style={{ color: 'var(--color-text)' }}>Executivo</h1>
          <p className="text-sm" style={{ color: 'var(--color-text-subtle)' }}>Visão consolidada da operação, financeiro e backlog.</p>
        </div>
        <LastUpdated dataExtracao={overview.data?.updatedAt ?? null} />
      </div>

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

      <ChartWrapper titulo="Tendencia Consolidada" option={option} isLoading={serie.isLoading} isEmpty={(serie.data ?? []).length === 0} altura={360} />
    </div>
  );
}
