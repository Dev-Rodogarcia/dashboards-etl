import type { EChartsOption } from 'echarts';
import { Link } from 'react-router-dom';
import ChartWrapper from '../components/charts/ChartWrapper';
import FaturasKpiGrid from '../components/domain/faturas/FaturasKpiGrid';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import DataTable, { type ColunaTabela } from '../components/shared/DataTable';
import DateRangePicker from '../components/shared/DateRangePicker';
import ExportButton from '../components/shared/ExportButton';
import FilterBar, { type ActiveFilter } from '../components/shared/FilterBar';
import LastUpdated from '../components/shared/LastUpdated';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { useFiltro } from '../contexts/FiltroContext';
import { useClientes, useFiliais } from '../hooks/queries/useDimensoes';
import {
  useFaturasAging,
  useFaturasMensal,
  useFaturasOverview,
  useFaturasReconciliacao,
  useFaturasStatusProcesso,
  useFaturasTabela,
  useFaturasTopClientes,
} from '../hooks/queries/useFaturas';
import type { FaturaReconciliacaoRow, FaturaResumoRow, FaturasFiltro } from '../types/faturas';
import { CORES } from '../utils/chartColors';
import { formatarMoeda } from '../utils/formatadores';

export default function FaturasPage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setDataRange, setFiltro, limparFiltros } = useFiltro();
  const filiais = useFiliais();
  const clientes = useClientes();

  const filtro: FaturasFiltro = {
    dataInicio,
    dataFim,
    filiais: filtros.filiais,
    pagadores: filtros.pagadores,
    statusProcesso: filtros.statusProcesso,
    pago: filtros.pago,
  };

  const activeFilters: ActiveFilter[] = [
    { label: 'Filiais', count: filtros.filiais?.length ?? 0, onRemove: () => setFiltro('filiais', []) },
    { label: 'Pagadores', count: filtros.pagadores?.length ?? 0, onRemove: () => setFiltro('pagadores', []) },
    { label: 'Status Processo', count: filtros.statusProcesso?.length ?? 0, onRemove: () => setFiltro('statusProcesso', []) },
    { label: 'Pago', count: filtros.pago?.length ?? 0, onRemove: () => setFiltro('pago', []) },
  ];

  const overview = useFaturasOverview(filtro);
  const mensal = useFaturasMensal(filtro);
  const aging = useFaturasAging(filtro);
  const topClientes = useFaturasTopClientes(filtro);
  const statusProcesso = useFaturasStatusProcesso(filtro);
  const reconciliacao = useFaturasReconciliacao(filtro, 80);
  const tabela = useFaturasTabela(filtro, 120);
  const hasFinancialData = overview.data?.hasFinancialData ?? true;

  const mensalOption: EChartsOption = {
    legend: { bottom: 0 },
    xAxis: { type: 'category', data: (mensal.data ?? []).map((item) => item.month) },
    yAxis: { type: 'value' },
    series: [
      { name: 'Faturado', type: 'bar', data: (mensal.data ?? []).map((item) => item.faturado), itemStyle: { color: CORES.primaria } },
      { name: 'Pago', type: 'line', data: (mensal.data ?? []).map((item) => item.pago), itemStyle: { color: CORES.sucesso } },
      { name: 'Saldo', type: 'line', data: (mensal.data ?? []).map((item) => item.saldoAberto), itemStyle: { color: CORES.aviso } },
    ],
  };

  const agingOption: EChartsOption = {
    xAxis: { type: 'category', data: (aging.data ?? []).map((item) => item.faixa) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: (aging.data ?? []).map((item) => item.valor), itemStyle: { color: CORES.perigo } }],
  };

  const topClientesOption: EChartsOption = {
    grid: { left: 150 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: (topClientes.data ?? []).map((item) => item.cliente).reverse() },
    series: [{ type: 'bar', data: (topClientes.data ?? []).map((item) => item.faturado).reverse(), itemStyle: { color: CORES.secundaria } }],
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

  const colunasResumo: ColunaTabela<FaturaResumoRow>[] = [
    { chave: 'documento', label: 'Documento', fixo: true },
    { chave: 'emissao', label: 'Emissao' },
    { chave: 'vencimento', label: 'Vencimento' },
    { chave: 'filial', label: 'Filial' },
    { chave: 'clientePagador', label: 'Cliente', largura: '220px' },
    { chave: 'valorOperacional', label: 'Operacional', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'valorFinanceiro', label: 'Financeiro', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'valorPago', label: 'Pago', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'valorAberto', label: 'Aberto', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'statusProcesso', label: 'Processo', formato: (valor) => <StatusBadge status={String(valor)} /> },
  ];

  const colunasReconciliacao: ColunaTabela<FaturaReconciliacaoRow>[] = [
    { chave: 'documento', label: 'Documento', fixo: true },
    { chave: 'emissao', label: 'Emissao' },
    { chave: 'clientePagador', label: 'Cliente', largura: '220px' },
    { chave: 'valorOperacional', label: 'Operacional', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'valorFinanceiro', label: 'Financeiro', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'status', label: 'Status', formato: (valor) => <StatusBadge status={String(valor)} /> },
  ];

  return (
    <div className="w-full">
      <div className="mb-5 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold leading-tight" style={{ color: 'var(--color-text)' }}>Faturas</h1>
          <p className="text-sm" style={{ color: 'var(--color-text-subtle)' }}>Recebiveis, aging e reconciliacao entre operacao e financeiro.</p>
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
        <AsyncMultiSelect
          label="Pago"
          opcoes={['Pago', 'Nao Pago']}
          selecionados={filtros.pago ?? []}
          onChange={(valores) => setFiltro('pago', valores)}
        />
      </FilterBar>

      {overview.isError && <MensagemErro mensagem={getApiErrorMessage(overview.error, 'Erro ao carregar indicadores de faturas.')} tipo={getTipoErro(overview.error)} />}
      {overview.data && <FaturasKpiGrid overview={overview.data} />}

      {!overview.isLoading && overview.data && !hasFinancialData ? (
        <div
          className="rounded-[20px] border p-5 text-sm shadow-sm"
          style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)', color: 'var(--color-text-muted)' }}
        >
          Nenhum título financeiro foi encontrado para o período selecionado. A visão operacional permanece disponível em{' '}
          <Link to="/faturas-por-cliente" className="font-medium underline" style={{ color: 'var(--color-primary)' }}>
            Faturas por Cliente
          </Link>
          .
        </div>
      ) : (
        <>
          <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
            <ChartWrapper titulo="Faturado x Pago por Mes" option={mensalOption} isLoading={mensal.isLoading} isEmpty={(mensal.data ?? []).length === 0} />
            <ChartWrapper titulo="Aging de Titulos" option={agingOption} isLoading={aging.isLoading} isEmpty={(aging.data ?? []).length === 0} />
          </div>

          <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
            <ChartWrapper titulo="Top Clientes por Faturamento" option={topClientesOption} isLoading={topClientes.isLoading} isEmpty={(topClientes.data ?? []).length === 0} />
            <ChartWrapper titulo="Status do Processo" option={statusOption} isLoading={statusProcesso.isLoading} isEmpty={(statusProcesso.data ?? []).length === 0} />
          </div>

          <div className="mb-3 flex justify-end">
            <ExportButton dados={(tabela.data ?? []) as unknown as Record<string, unknown>[]} nomeArquivo="faturas" />
          </div>
          <DataTable titulo="Titulos e Processos" dados={tabela.data ?? []} colunas={colunasResumo} chaveLinha="uniqueId" isLoading={tabela.isLoading} />

          <div className="mt-6">
            <DataTable titulo="Reconciliacao Operacional x Financeiro" dados={reconciliacao.data ?? []} colunas={colunasReconciliacao} chaveLinha="uniqueId" isLoading={reconciliacao.isLoading} />
          </div>
        </>
      )}
    </div>
  );
}
