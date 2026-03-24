import type { EChartsOption } from 'echarts';
import FretesClienteRanking from '../components/domain/fretes/FretesClienteRanking';
import FretesDocumentMix from '../components/domain/fretes/FretesDocumentMix';
import FretesKpiGrid from '../components/domain/fretes/FretesKpiGrid';
import FretesReceitaTrend from '../components/domain/fretes/FretesReceitaTrend';
import ChartWrapper from '../components/charts/ChartWrapper';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import DataTable, { type ColunaTabela } from '../components/shared/DataTable';
import DateRangePicker from '../components/shared/DateRangePicker';
import ExportButton from '../components/shared/ExportButton';
import FilterBar, { type ActiveFilter } from '../components/shared/FilterBar';
import LastUpdated from '../components/shared/LastUpdated';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import { useFiltro } from '../contexts/FiltroContext';
import { useClientes, useFiliais } from '../hooks/queries/useDimensoes';
import {
  useFretesGraficos,
  useFretesMixDocumental,
  useFretesOverview,
  useFretesSerie,
  useFretesTabela,
  useFretesTopClientes,
} from '../hooks/queries/useFretes';
import type { FreteResumoRow, FretesFiltro } from '../types/fretes';
import { CORES } from '../utils/chartColors';
import { formatarMoeda, formatarPeso } from '../utils/formatadores';

export default function FretesPage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setFiltro, limparFiltros } = useFiltro();
  const filiais = useFiliais();
  const clientes = useClientes();

  const filtro: FretesFiltro = {
    dataInicio,
    dataFim,
    filiais: filtros.filiais,
    status: filtros.status,
    pagadores: filtros.pagadores,
  };

  const activeFilters: ActiveFilter[] = [
    { label: 'Filiais',   count: filtros.filiais?.length   ?? 0, onRemove: () => setFiltro('filiais', []) },
    { label: 'Pagadores', count: filtros.pagadores?.length ?? 0, onRemove: () => setFiltro('pagadores', []) },
    { label: 'Status',    count: filtros.status?.length    ?? 0, onRemove: () => setFiltro('status', []) },
  ];

  const overview = useFretesOverview(filtro);
  const serie = useFretesSerie(filtro);
  const graficos = useFretesGraficos(filtro);
  const topClientes = useFretesTopClientes(filtro);
  const mixDoc = useFretesMixDocumental(filtro);
  const tabela = useFretesTabela(filtro, 120);

  const previsaoEntries = graficos.data?.previsaoPorStatus ?? [];
  const origemDestinoEntries = graficos.data?.topRotasPorReceita ?? [];

  const previsaoOption: EChartsOption = {
    xAxis: { type: 'category', data: previsaoEntries.map((item) => item.status) },
    yAxis: { type: 'value' },
    legend: { bottom: 0 },
    series: [
      {
        name: 'Vencidos',
        type: 'bar',
        stack: 'prazo',
        data: previsaoEntries.map((item) => item.vencidos),
        itemStyle: { color: CORES.perigo },
      },
      {
        name: 'No prazo',
        type: 'bar',
        stack: 'prazo',
        data: previsaoEntries.map((item) => item.noPrazo),
        itemStyle: { color: CORES.sucesso },
      },
    ],
  };

  const origemDestinoOption: EChartsOption = {
    grid: { left: 130 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: origemDestinoEntries.map((item) => `${item.origemUf} -> ${item.destinoUf}`).reverse() },
    series: [
      {
        name: 'Receita',
        type: 'bar',
        data: origemDestinoEntries.map((item) => item.receita).reverse(),
        itemStyle: { color: CORES.secundaria },
      },
    ],
    tooltip: {
      trigger: 'axis',
    },
  };

  const colunas: ColunaTabela<FreteResumoRow>[] = [
    { chave: 'id', label: 'ID', fixo: true },
    { chave: 'dataFrete', label: 'Data' },
    { chave: 'status', label: 'Status', formato: (valor) => <StatusBadge status={String(valor)} /> },
    { chave: 'filial', label: 'Filial' },
    { chave: 'pagador', label: 'Pagador', largura: '220px' },
    { chave: 'documentoTipo', label: 'Documento' },
    { chave: 'valorFrete', label: 'Valor Frete', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'valorTotalServico', label: 'Receita', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'pesoTaxado', label: 'Peso', formato: (valor) => formatarPeso(Number(valor ?? 0)) },
    { chave: 'volumes', label: 'Volumes' },
    { chave: 'origemUf', label: 'UF Origem' },
    { chave: 'destinoUf', label: 'UF Destino' },
    { chave: 'previsaoEntrega', label: 'Previsao' },
  ];

  return (
    <div className="w-full">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[#21478A]">Fretes</h1>
          <p className="text-sm text-gray-500">Receita operacional, mix documental e carteira ativa por rota.</p>
        </div>
        <LastUpdated dataExtracao={overview.data?.updatedAt ?? null} />
      </div>

      <FilterBar onClear={limparFiltros} activeFilters={activeFilters} dataInicio={dataInicio} dataFim={dataFim}>
        <DateRangePicker
          dataInicio={dataInicio}
          dataFim={dataFim}
          onDataInicioChange={setDataInicio}
          onDataFimChange={setDataFim}
        />
        <AsyncMultiSelect
          label="Filiais"
          opcoes={filiais.data ?? []}
          selecionados={filtros.filiais ?? []}
          onChange={(valores) => setFiltro('filiais', valores)}
          isLoading={filiais.isLoading}
        />
        <AsyncMultiSelect
          label="Pagadores"
          opcoes={clientes.data ?? []}
          selecionados={filtros.pagadores ?? []}
          onChange={(valores) => setFiltro('pagadores', valores)}
          isLoading={clientes.isLoading}
        />
        <AsyncMultiSelect
          label="Status"
          opcoes={Array.from(new Set((tabela.data ?? []).map((item) => item.status))).filter(Boolean)}
          selecionados={filtros.status ?? []}
          onChange={(valores) => setFiltro('status', valores)}
        />
      </FilterBar>

      {overview.isError && <MensagemErro mensagem="Erro ao carregar indicadores de fretes." />}
      {overview.data && <FretesKpiGrid overview={overview.data} />}

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-3">
        <ChartWrapper titulo="Previsao de Entrega por Status" option={previsaoOption} isLoading={graficos.isLoading} isEmpty={previsaoEntries.length === 0} />
        <FretesReceitaTrend dados={serie.data ?? []} isLoading={serie.isLoading} />
        <FretesDocumentMix dados={mixDoc.data ?? []} isLoading={mixDoc.isLoading} />
      </div>

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <FretesClienteRanking dados={topClientes.data ?? []} isLoading={topClientes.isLoading} />
        <ChartWrapper titulo="Top Rotas por Receita" option={origemDestinoOption} isLoading={graficos.isLoading} isEmpty={origemDestinoEntries.length === 0} altura={360} />
      </div>

      <div className="mb-3 flex justify-end">
        <ExportButton dados={(tabela.data ?? []) as unknown as Record<string, unknown>[]} nomeArquivo="fretes" />
      </div>
      <DataTable
        titulo="Fretes Analiticos"
        dados={tabela.data ?? []}
        colunas={colunas}
        chaveLinha="id"
        isLoading={tabela.isLoading}
      />
    </div>
  );
}
