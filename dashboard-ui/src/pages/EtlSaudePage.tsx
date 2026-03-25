import type { EChartsOption } from 'echarts';
import ChartWrapper from '../components/charts/ChartWrapper';
import EtlSaudeKpiGrid from '../components/domain/etlSaude/EtlSaudeKpiGrid';
import DataTable, { type ColunaTabela } from '../components/shared/DataTable';
import DateRangePicker from '../components/shared/DateRangePicker';
import ExportButton from '../components/shared/ExportButton';
import FilterBar from '../components/shared/FilterBar';
import LastUpdated from '../components/shared/LastUpdated';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import { useFiltro } from '../contexts/FiltroContext';
import { useEtlSaudeGraficos, useEtlSaudeOverview, useEtlSaudeSerie, useEtlSaudeTabela } from '../hooks/queries/useEtlSaude';
import type { EtlExecucaoRow } from '../types/etlSaude';

export default function EtlSaudePage() {
  const { dataInicio, dataFim, setDataInicio, setDataFim, setDataRange, limparFiltros } = useFiltro();
  const filtro = { dataInicio, dataFim };

  const overview = useEtlSaudeOverview(filtro);
  const serie = useEtlSaudeSerie(filtro);
  const graficos = useEtlSaudeGraficos(filtro);
  const tabela = useEtlSaudeTabela(filtro, 120);

  const categorias = graficos.data?.categoriasErro ?? [];

  const serieOption: EChartsOption = {
    legend: { bottom: 0 },
    xAxis: { type: 'category', data: (serie.data ?? []).map((item) => item.date) },
    yAxis: { type: 'value' },
    series: [
      { name: 'Execucoes', type: 'bar', data: (serie.data ?? []).map((item) => item.execucoes) },
      { name: 'Erros', type: 'line', data: (serie.data ?? []).map((item) => item.erros) },
      { name: 'Volume', type: 'line', data: (serie.data ?? []).map((item) => item.volumeProcessado) },
    ],
  };

  const categoriasOption: EChartsOption = {
    xAxis: { type: 'category', data: categorias.map((item) => item.categoria) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: categorias.map((item) => item.total) }],
  };

  const colunas: ColunaTabela<EtlExecucaoRow>[] = [
    { chave: 'id', label: 'Execucao', fixo: true },
    { chave: 'data', label: 'Data' },
    { chave: 'inicio', label: 'Inicio' },
    { chave: 'fim', label: 'Fim' },
    { chave: 'duracaoSegundos', label: 'Duracao (s)' },
    { chave: 'totalRegistros', label: 'Volume' },
    { chave: 'status', label: 'Status', formato: (valor) => <StatusBadge status={String(valor)} /> },
    { chave: 'categoriaErro', label: 'Categoria Erro' },
  ];

  return (
    <div className="w-full">
      <div className="mb-5 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold leading-tight" style={{ color: 'var(--color-text)' }}>Saúde do ETL</h1>
          <p className="text-sm" style={{ color: 'var(--color-text-subtle)' }}>Execuções, volume processado e distribuição de erros.</p>
        </div>
        <LastUpdated dataExtracao={overview.data?.updatedAt ?? null} />
      </div>

      <FilterBar onClear={limparFiltros} dataInicio={dataInicio} dataFim={dataFim}>
        <DateRangePicker dataInicio={dataInicio} dataFim={dataFim} onDataInicioChange={setDataInicio} onDataFimChange={setDataFim} onRangeChange={setDataRange} />
      </FilterBar>

      {overview.isError && <MensagemErro mensagem="Erro ao carregar indicadores do ETL." />}
      {overview.data && <EtlSaudeKpiGrid overview={overview.data} />}

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper titulo="Timeline de Execuções" option={serieOption} isLoading={serie.isLoading} isEmpty={(serie.data ?? []).length === 0} />
        <ChartWrapper titulo="Categorias de Erro" option={categoriasOption} isLoading={graficos.isLoading} isEmpty={categorias.length === 0} />
      </div>

      <div className="mb-3 flex justify-end">
        <ExportButton dados={(tabela.data ?? []) as unknown as Record<string, unknown>[]} nomeArquivo="etl-saude" />
      </div>
      <DataTable titulo="Execucoes do ETL" dados={tabela.data ?? []} colunas={colunas} chaveLinha="id" isLoading={tabela.isLoading} />
    </div>
  );
}
