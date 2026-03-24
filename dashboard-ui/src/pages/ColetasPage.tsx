import type { EChartsOption } from 'echarts';
import ColetasKpiGrid from '../components/domain/coletas/ColetasKpiGrid';
import ColetasTrend from '../components/domain/coletas/ColetasTrend';
import ChartWrapper from '../components/charts/ChartWrapper';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import DataTable, { type ColunaTabela } from '../components/shared/DataTable';
import DateRangePicker from '../components/shared/DateRangePicker';
import ExportButton from '../components/shared/ExportButton';
import FilterBar from '../components/shared/FilterBar';
import LastUpdated from '../components/shared/LastUpdated';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import { useFiltro } from '../contexts/FiltroContext';
import { useClientes, useFiliais, useUsuarios } from '../hooks/queries/useDimensoes';
import { useColetasGraficos, useColetasOverview, useColetasSerie, useColetasTabela } from '../hooks/queries/useColetas';
import type { ColetaResumoRow, ColetasFiltro } from '../types/coletas';
import { CORES } from '../utils/chartColors';
import { formatarMoeda, formatarPeso } from '../utils/formatadores';

export default function ColetasPage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setFiltro, limparFiltros } = useFiltro();
  const filiais = useFiliais();
  const clientes = useClientes();
  const usuarios = useUsuarios();

  const filtro: ColetasFiltro = {
    dataInicio,
    dataFim,
    filiais: filtros.filiais,
    clientes: filtros.clientes,
    status: filtros.status,
    regioes: filtros.regioes,
    usuarios: filtros.usuarios,
  };

  const overview = useColetasOverview(filtro);
  const serie = useColetasSerie(filtro);
  const graficos = useColetasGraficos(filtro);
  const tabela = useColetasTabela(filtro, 120);

  const statusData = graficos.data?.statusDistribuicao ?? [];
  const slaPorFilial = graficos.data?.slaPorFilial ?? [];
  const regiaoVolume = graficos.data?.regiaoVolume ?? [];
  const aging = graficos.data?.agingAbertas ?? [];

  const statusOption: EChartsOption = {
    xAxis: { type: 'category', data: statusData.map((item) => item.status) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: statusData.map((item) => item.total), itemStyle: { color: CORES.primaria } }],
  };

  const slaOption: EChartsOption = {
    grid: { left: 120 },
    xAxis: { type: 'value', max: 100 },
    yAxis: { type: 'category', data: slaPorFilial.map((item) => item.filial).reverse() },
    series: [{ type: 'bar', data: slaPorFilial.map((item) => item.slaPct).reverse(), itemStyle: { color: CORES.sucesso } }],
  };

  const regiaoOption: EChartsOption = {
    legend: { bottom: 0 },
    xAxis: { type: 'category', data: regiaoVolume.map((item) => item.regiao) },
    yAxis: [{ type: 'value', name: 'Coletas' }, { type: 'value', name: 'Peso', position: 'right' }],
    series: [
      { name: 'Coletas', type: 'bar', data: regiaoVolume.map((item) => item.totalColetas), itemStyle: { color: CORES.primaria } },
      { name: 'Peso Taxado', type: 'line', yAxisIndex: 1, data: regiaoVolume.map((item) => item.pesoTaxado), itemStyle: { color: CORES.secundaria } },
    ],
  };

  const agingOption: EChartsOption = {
    xAxis: { type: 'category', data: aging.map((item) => item.faixa) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: aging.map((item) => item.total), itemStyle: { color: CORES.aviso } }],
  };

  const colunas: ColunaTabela<ColetaResumoRow>[] = [
    { chave: 'id', label: 'ID', fixo: true },
    { chave: 'coleta', label: 'Coleta' },
    { chave: 'solicitacao', label: 'Solicitacao' },
    { chave: 'status', label: 'Status', formato: (valor) => <StatusBadge status={String(valor)} /> },
    { chave: 'filial', label: 'Filial' },
    { chave: 'cliente', label: 'Cliente', largura: '220px' },
    { chave: 'regiaoColeta', label: 'Regiao' },
    { chave: 'volumes', label: 'Volumes' },
    { chave: 'pesoTaxado', label: 'Peso', formato: (valor) => formatarPeso(Number(valor ?? 0)) },
    { chave: 'valorNf', label: 'Valor NF', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'numeroTentativas', label: 'Tentativas' },
  ];

  return (
    <div className="w-full">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[#21478A]">Coletas</h1>
          <p className="text-sm text-gray-500">SLA operacional, distribuicao por status e aging de abertas.</p>
        </div>
        <LastUpdated dataExtracao={overview.data?.updatedAt ?? null} />
      </div>

      <FilterBar onClear={limparFiltros}>
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
          label="Clientes"
          opcoes={clientes.data ?? []}
          selecionados={filtros.clientes ?? []}
          onChange={(valores) => setFiltro('clientes', valores)}
          isLoading={clientes.isLoading}
        />
        <AsyncMultiSelect
          label="Usuarios"
          opcoes={(usuarios.data ?? []).map((item) => item.nome)}
          selecionados={filtros.usuarios ?? []}
          onChange={(valores) => setFiltro('usuarios', valores)}
          isLoading={usuarios.isLoading}
        />
      </FilterBar>

      {overview.isError && <MensagemErro mensagem="Erro ao carregar indicadores de coletas." />}
      {overview.data && <ColetasKpiGrid overview={overview.data} />}

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-3">
        <ColetasTrend dados={serie.data ?? []} isLoading={serie.isLoading} />
        <ChartWrapper titulo="Distribuicao por Status" option={statusOption} isLoading={graficos.isLoading} isEmpty={statusData.length === 0} />
        <ChartWrapper titulo="SLA por Filial" option={slaOption} isLoading={graficos.isLoading} isEmpty={slaPorFilial.length === 0} />
      </div>

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper titulo="Regiao x Volume/Peso" option={regiaoOption} isLoading={graficos.isLoading} isEmpty={regiaoVolume.length === 0} />
        <ChartWrapper titulo="Aging de Coletas Abertas" option={agingOption} isLoading={graficos.isLoading} isEmpty={aging.length === 0} altura={300} />
      </div>

      <div className="mb-3 flex justify-end">
        <ExportButton dados={(tabela.data ?? []) as unknown as Record<string, unknown>[]} nomeArquivo="coletas" />
      </div>
      <DataTable titulo="Coletas Analiticas" dados={tabela.data ?? []} colunas={colunas} chaveLinha="id" isLoading={tabela.isLoading} />
    </div>
  );
}
