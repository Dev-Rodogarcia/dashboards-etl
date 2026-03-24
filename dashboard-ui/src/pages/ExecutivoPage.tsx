import type { EChartsOption } from 'echarts';
import ChartWrapper from '../components/charts/ChartWrapper';
import ExecutivoKpiGrid from '../components/domain/executivo/ExecutivoKpiGrid';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import DateRangePicker from '../components/shared/DateRangePicker';
import FilterBar from '../components/shared/FilterBar';
import LastUpdated from '../components/shared/LastUpdated';
import MensagemErro from '../components/ui/MensagemErro';
import { useFiltro } from '../contexts/FiltroContext';
import { useFiliais } from '../hooks/queries/useDimensoes';
import { useExecutivoOverview, useExecutivoSerie } from '../hooks/queries/useExecutivo';

export default function ExecutivoPage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setFiltro, limparFiltros } = useFiltro();
  const filiais = useFiliais();

  const filtro = {
    dataInicio,
    dataFim,
    filiais: filtros.filiais,
  };

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
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[#21478A]">Executivo</h1>
          <p className="text-sm text-gray-500">Visão consolidada da operação, financeiro e backlog.</p>
        </div>
        <LastUpdated dataExtracao={overview.data?.updatedAt ?? null} />
      </div>

      <FilterBar onClear={limparFiltros}>
        <DateRangePicker dataInicio={dataInicio} dataFim={dataFim} onDataInicioChange={setDataInicio} onDataFimChange={setDataFim} />
        <AsyncMultiSelect label="Filiais" opcoes={filiais.data ?? []} selecionados={filtros.filiais ?? []} onChange={(valores) => setFiltro('filiais', valores)} isLoading={filiais.isLoading} />
      </FilterBar>

      {overview.isError && <MensagemErro mensagem="Erro ao carregar visão executiva." />}
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
