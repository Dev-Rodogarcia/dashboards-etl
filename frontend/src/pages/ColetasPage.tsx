import type { EChartsOption } from 'echarts';
import ColetasKpiGrid from '../components/domain/coletas/ColetasKpiGrid';
import ColetasTrend from '../components/domain/coletas/ColetasTrend';
import ChartWrapper from '../components/charts/ChartWrapper';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import AnalyticalDataTable, { type ColunaTabelaAnalitica } from '../components/shared/AnalyticalDataTable';
import DateRangePicker from '../components/shared/DateRangePicker';
import ExportButton from '../components/shared/ExportButton';
import FilterBar, { type ActiveFilter } from '../components/shared/FilterBar';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import { exportarColetasCsv } from '../api/endpoints/coletasServico';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { useFiltro } from '../contexts/FiltroContext';
import { usePageHeader } from '../contexts/PageHeaderContext';
import { useClientes, useFiliais, useUsuarios } from '../hooks/queries/useDimensoes';
import { useColetasGraficos, useColetasOverview, useColetasSerie, useColetasTabelaPaginada } from '../hooks/queries/useColetas';
import { useAnalyticalTableFilters } from '../hooks/useAnalyticalTableFilters';
import { useTabelaPaginadaState } from '../hooks/useTabelaPaginadaState';
import type { ColetaResumoRow, ColetasFiltro } from '../types/coletas';
import { CORES } from '../utils/chartColors';
import { formatarMoeda, formatarPeso } from '../utils/formatadores';
import { combinarStatusOptions } from '../utils/tableStatusOptions';

export default function ColetasPage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setDataRange, setFiltro, limparFiltros } = useFiltro();
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

  const activeFilters: ActiveFilter[] = [
    { label: 'Filiais', count: filtros.filiais?.length ?? 0, onRemove: () => setFiltro('filiais', []) },
    { label: 'Clientes', count: filtros.clientes?.length ?? 0, onRemove: () => setFiltro('clientes', []) },
    { label: 'Usuarios', count: filtros.usuarios?.length ?? 0, onRemove: () => setFiltro('usuarios', []) },
  ];

  const overview = useColetasOverview(filtro);
  const serie = useColetasSerie(filtro);
  const graficos = useColetasGraficos(filtro);
  const filtrosTabela = useAnalyticalTableFilters();
  const paginacaoTabela = useTabelaPaginadaState(JSON.stringify({ filtro, tabela: filtrosTabela.resetKey }));
  const tabela = useColetasTabelaPaginada(filtro, paginacaoTabela.pagina, paginacaoTabela.tamanhoPagina, filtrosTabela.apiFilters);

  usePageHeader({
    title: 'Coletas',
    description: 'SLA operacional, distribuicao por status e aging de abertas.',
    updatedAt: overview.data?.updatedAt ?? null,
  });

  const statusData = graficos.data?.statusDistribuicao ?? [];
  const slaPorFilial = graficos.data?.slaPorFilial ?? [];
  const regiaoVolume = graficos.data?.regiaoVolume ?? [];
  const aging = graficos.data?.agingAbertas ?? [];
  const statusTabelaOptions = combinarStatusOptions(
    statusData.map((item) => item.status),
    (tabela.data?.conteudo ?? []).map((item) => item.status),
    filtros.status,
  );

  const statusOption: EChartsOption = {
    xAxis: { type: 'category', data: statusData.map((item) => item.status) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: statusData.map((item) => item.total), itemStyle: { color: CORES.primaria } }],
  };

  const slaOption: EChartsOption = {
    grid: { left: 10, containLabel: true },
    xAxis: { type: 'value', max: 100 },
    yAxis: {
      type: 'category',
      data: slaPorFilial.map((item) => item.filial).reverse(),
      axisLabel: {
        formatter: (value: string) => value.length > 18 ? value.slice(0, 18) + '…' : value,
      },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: unknown) => {
        const p = (params as { name: string; value: number }[])[0];
        return `${p.name}<br/>${p.value.toFixed(1)}%`;
      },
    },
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

  const colunas: ColunaTabelaAnalitica<ColetaResumoRow>[] = [
    { chave: 'id', label: 'ID', fixo: true, filtroTabela: 'codigo' },
    { chave: 'coleta', label: 'Coleta' },
    { chave: 'solicitacao', label: 'Solicitacao' },
    { chave: 'status', label: 'Status', filtroTabela: 'status', formato: (valor) => <StatusBadge status={String(valor)} /> },
    { chave: 'filial', label: 'Filial' },
    { chave: 'cliente', label: 'Cliente', largura: '220px', filtroTabela: 'razaoSocial' },
    { chave: 'regiaoColeta', label: 'Regiao', filtroTabela: 'origem' },
    { chave: 'volumes', label: 'Volumes' },
    { chave: 'pesoTaxado', label: 'Peso', formato: (valor) => formatarPeso(Number(valor ?? 0)) },
    { chave: 'valorNf', label: 'Valor NF', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'numeroTentativas', label: 'Tentativas' },
  ];

  return (
    <div className="w-full">
      <FilterBar onClear={limparFiltros} activeFilters={activeFilters} dataInicio={dataInicio} dataFim={dataFim}>
        <DateRangePicker
          dataInicio={dataInicio}
          dataFim={dataFim}
          onDataInicioChange={setDataInicio}
          onDataFimChange={setDataFim}
          onRangeChange={setDataRange}
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

      {overview.isError && <MensagemErro mensagem={getApiErrorMessage(overview.error, 'Erro ao carregar indicadores de coletas.')} tipo={getTipoErro(overview.error)} />}
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
        <ExportButton nomeArquivo="coletas" onExport={() => exportarColetasCsv(filtro, filtrosTabela.apiFilters)} />
      </div>
      <AnalyticalDataTable
        titulo="Coletas Analiticas"
        dados={tabela.data?.conteudo ?? []}
        colunas={colunas}
        chaveLinha="id"
        filtros={filtrosTabela.filters}
        hiddenActiveCount={filtrosTabela.hiddenActiveCount}
        hasAnyFilter={filtrosTabela.hasAnyFilter}
        onTextFilterChange={filtrosTabela.setTextFilter}
        onMultiFilterChange={filtrosTabela.setMultiFilter}
        onColumnFilterChange={filtrosTabela.setColumnFilter}
        onClearFilters={filtrosTabela.clearTableFilters}
        statusOptions={statusTabelaOptions}
        statusOptionsLoading={graficos.isLoading}
        isLoading={tabela.isLoading}
        totalRegistros={tabela.data?.totalElementos}
        paginaAtual={paginacaoTabela.pagina}
        tamanhoPagina={paginacaoTabela.tamanhoPagina}
        onPaginaChange={paginacaoTabela.setPagina}
        onTamanhoPaginaChange={paginacaoTabela.setTamanhoPagina}
      />
    </div>
  );
}
