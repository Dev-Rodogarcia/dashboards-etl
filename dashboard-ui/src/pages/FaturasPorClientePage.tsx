import type { EChartsOption } from 'echarts';
import ChartWrapper from '../components/charts/ChartWrapper';
import FaturasPorClienteKpiGrid from '../components/domain/faturasPorCliente/FaturasPorClienteKpiGrid';
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
  useFaturasPorClienteAging,
  useFaturasPorClienteMensal,
  useFaturasPorClienteOverview,
  useFaturasPorClienteStatusProcesso,
  useFaturasPorClienteTabela,
  useFaturasPorClienteTopClientes,
} from '../hooks/queries/useFaturasPorCliente';
import type { FaturaPorClienteResumoRow, FaturasPorClienteFiltro } from '../types/faturasPorCliente';
import { CORES } from '../utils/chartColors';
import { formatarMoeda } from '../utils/formatadores';

export default function FaturasPorClientePage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setDataRange, setFiltro, limparFiltros } = useFiltro();
  const filiais = useFiliais();
  const clientes = useClientes();

  const filtro: FaturasPorClienteFiltro = {
    dataInicio,
    dataFim,
    filiais: filtros.filiais,
    pagadores: filtros.pagadores,
    statusProcesso: filtros.statusProcesso,
  };

  const activeFilters: ActiveFilter[] = [
    { label: 'Filiais', count: filtros.filiais?.length ?? 0, onRemove: () => setFiltro('filiais', []) },
    { label: 'Pagadores', count: filtros.pagadores?.length ?? 0, onRemove: () => setFiltro('pagadores', []) },
    { label: 'Status Processo', count: filtros.statusProcesso?.length ?? 0, onRemove: () => setFiltro('statusProcesso', []) },
  ];

  const overview = useFaturasPorClienteOverview(filtro);
  const mensal = useFaturasPorClienteMensal(filtro);
  const aging = useFaturasPorClienteAging(filtro);
  const topClientes = useFaturasPorClienteTopClientes(filtro);
  const statusProcesso = useFaturasPorClienteStatusProcesso(filtro);
  const tabela = useFaturasPorClienteTabela(filtro, 120);

  const mensalOption: EChartsOption = {
    legend: { bottom: 0 },
    xAxis: { type: 'category', data: (mensal.data ?? []).map((item) => item.month) },
    yAxis: [
      { type: 'value', name: 'Valor' },
      { type: 'value', name: 'Registros' },
    ],
    series: [
      {
        name: 'Valor Faturado',
        type: 'bar',
        data: (mensal.data ?? []).map((item) => item.valorFaturado),
        itemStyle: { color: CORES.primaria },
      },
      {
        name: 'Registros Faturados',
        type: 'line',
        yAxisIndex: 1,
        data: (mensal.data ?? []).map((item) => item.registrosFaturados),
        itemStyle: { color: CORES.secundaria },
      },
    ],
  };

  const agingOption: EChartsOption = {
    xAxis: { type: 'category', data: (aging.data ?? []).map((item) => item.faixa) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: (aging.data ?? []).map((item) => item.valor), itemStyle: { color: CORES.aviso } }],
  };

  const topClientesOption: EChartsOption = {
    grid: { left: 10, containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: (topClientes.data ?? []).map((item) => item.cliente).reverse() },
    series: [
      {
        type: 'bar',
        data: (topClientes.data ?? []).map((item) => item.valorFaturado).reverse(),
        itemStyle: { color: CORES.sucesso },
      },
    ],
  };

  const statusOption: EChartsOption = {
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['38%', '68%'],
        data: (statusProcesso.data ?? []).map((item) => ({ name: item.statusProcesso, value: item.total })),
      },
    ],
  };

  const colunasResumo: ColunaTabela<FaturaPorClienteResumoRow>[] = [
    { chave: 'idUnico', label: 'ID Único', fixo: true, largura: '180px' },
    { chave: 'documentoFatura', label: 'Documento', largura: '140px' },
    { chave: 'emissao', label: 'Emissão' },
    { chave: 'vencimento', label: 'Vencimento' },
    { chave: 'baixa', label: 'Baixa' },
    { chave: 'filial', label: 'Filial' },
    { chave: 'clientePagador', label: 'Cliente', largura: '220px' },
    { chave: 'numeroCte', label: 'CT-e' },
    { chave: 'valorFaturado', label: 'Valor Faturado', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'statusProcesso', label: 'Status Processo', formato: (valor) => <StatusBadge status={String(valor)} /> },
  ];

  return (
    <div className="w-full">
      <div className="mb-5 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold leading-tight" style={{ color: 'var(--color-text)' }}>Faturas por Cliente</h1>
          <p className="text-sm" style={{ color: 'var(--color-text-subtle)' }}>Visão operacional de faturamento por cliente baseada em `ID Único`.</p>
        </div>
        <LastUpdated dataExtracao={overview.data?.updatedAt ?? null} />
      </div>

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
          label="Pagadores"
          opcoes={clientes.data ?? []}
          selecionados={filtros.pagadores ?? []}
          onChange={(valores) => setFiltro('pagadores', valores)}
          isLoading={clientes.isLoading}
        />
        <AsyncMultiSelect
          label="Status Processo"
          opcoes={['Faturado', 'Aguardando Faturamento']}
          selecionados={filtros.statusProcesso ?? []}
          onChange={(valores) => setFiltro('statusProcesso', valores)}
        />
      </FilterBar>

      {overview.isError && <MensagemErro mensagem="Erro ao carregar indicadores de faturas por cliente." />}
      {overview.data && <FaturasPorClienteKpiGrid overview={overview.data} />}

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper titulo="Valor Faturado por Mês" option={mensalOption} isLoading={mensal.isLoading} isEmpty={(mensal.data ?? []).length === 0} />
        <ChartWrapper titulo="Aging Operacional" option={agingOption} isLoading={aging.isLoading} isEmpty={(aging.data ?? []).length === 0} />
      </div>

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper titulo="Top Clientes por Valor Faturado" option={topClientesOption} isLoading={topClientes.isLoading} isEmpty={(topClientes.data ?? []).length === 0} />
        <ChartWrapper titulo="Status do Processo" option={statusOption} isLoading={statusProcesso.isLoading} isEmpty={(statusProcesso.data ?? []).length === 0} />
      </div>

      <div className="mb-3 flex justify-end">
        <ExportButton dados={(tabela.data ?? []) as unknown as Record<string, unknown>[]} nomeArquivo="faturas-por-cliente" />
      </div>
      <DataTable
        titulo="Faturas por Cliente"
        dados={tabela.data ?? []}
        colunas={colunasResumo}
        chaveLinha="idUnico"
        isLoading={tabela.isLoading}
      />
    </div>
  );
}
