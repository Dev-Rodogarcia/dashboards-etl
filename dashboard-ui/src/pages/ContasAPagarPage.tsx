import type { EChartsOption } from 'echarts';
import ChartWrapper from '../components/charts/ChartWrapper';
import ContasAPagarKpiGrid from '../components/domain/contasAPagar/ContasAPagarKpiGrid';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import DataTable, { type ColunaTabela } from '../components/shared/DataTable';
import DateRangePicker from '../components/shared/DateRangePicker';
import ExportButton from '../components/shared/ExportButton';
import FilterBar from '../components/shared/FilterBar';
import LastUpdated from '../components/shared/LastUpdated';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import { useFiltro } from '../contexts/FiltroContext';
import { useFiliais, usePlanoContas } from '../hooks/queries/useDimensoes';
import { useContasAPagarGraficos, useContasAPagarOverview, useContasAPagarSerie, useContasAPagarTabela } from '../hooks/queries/useContasAPagar';
import type { ContaPagarResumoRow, ContasAPagarFiltro } from '../types/contasAPagar';
import { CORES } from '../utils/chartColors';
import { formatarMoeda } from '../utils/formatadores';

export default function ContasAPagarPage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setFiltro, limparFiltros } = useFiltro();
  const filiais = useFiliais();
  const planoContas = usePlanoContas();

  const filtro: ContasAPagarFiltro = {
    dataInicio,
    dataFim,
    filiais: filtros.filiais,
    classificacoes: filtros.classificacoes,
    pago: filtros.pago,
    conciliado: filtros.conciliado,
  };

  const overview = useContasAPagarOverview(filtro);
  const serie = useContasAPagarSerie(filtro);
  const graficos = useContasAPagarGraficos(filtro);
  const tabela = useContasAPagarTabela(filtro, 120);

  const rankingFornecedor = graficos.data?.topFornecedores ?? [];
  const centroCusto = graficos.data?.centroCusto ?? [];
  const conciliacao = graficos.data?.conciliacao ?? [];

  const serieOption: EChartsOption = {
    legend: { bottom: 0 },
    xAxis: { type: 'category', data: (serie.data ?? []).map((item) => item.month) },
    yAxis: { type: 'value' },
    series: [
      { name: 'Pago', type: 'bar', stack: 'contas', data: (serie.data ?? []).map((item) => item.pago), itemStyle: { color: CORES.sucesso } },
      { name: 'Aberto', type: 'bar', stack: 'contas', data: (serie.data ?? []).map((item) => item.aberto), itemStyle: { color: CORES.aviso } },
    ],
  };

  const fornecedorOption: EChartsOption = {
    grid: { left: 150 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: rankingFornecedor.map((item) => item.fornecedor).reverse() },
    series: [{ type: 'bar', data: rankingFornecedor.map((item) => item.valor).reverse(), itemStyle: { color: CORES.primaria } }],
  };

  const centroOption: EChartsOption = {
    xAxis: { type: 'category', data: centroCusto.map((item) => item.centroCusto) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: centroCusto.map((item) => item.valor), itemStyle: { color: CORES.secundaria } }],
  };

  const conciliacaoOption: EChartsOption = {
    series: [{ type: 'pie', radius: ['40%', '68%'], data: conciliacao.map((item) => ({ name: item.status, value: item.valor })) }],
  };

  const colunas: ColunaTabela<ContaPagarResumoRow>[] = [
    { chave: 'lancamentoNumero', label: 'Lancamento', fixo: true },
    { chave: 'emissao', label: 'Emissao' },
    { chave: 'filial', label: 'Filial' },
    { chave: 'fornecedor', label: 'Fornecedor', largura: '220px' },
    { chave: 'classificacao', label: 'Classificacao' },
    { chave: 'centroCusto', label: 'Centro Custo' },
    { chave: 'valorAPagar', label: 'Valor a Pagar', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'valorPago', label: 'Valor Pago', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'statusPagamento', label: 'Pagamento', formato: (valor) => <StatusBadge status={String(valor)} /> },
  ];

  return (
    <div className="w-full">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[#21478A]">Contas a Pagar</h1>
          <p className="text-sm text-gray-500">Fluxo mensal, fornecedores relevantes e conciliação financeira.</p>
        </div>
        <LastUpdated dataExtracao={overview.data?.updatedAt ?? null} />
      </div>

      <FilterBar onClear={limparFiltros}>
        <DateRangePicker dataInicio={dataInicio} dataFim={dataFim} onDataInicioChange={setDataInicio} onDataFimChange={setDataFim} />
        <AsyncMultiSelect label="Filiais" opcoes={filiais.data ?? []} selecionados={filtros.filiais ?? []} onChange={(valores) => setFiltro('filiais', valores)} isLoading={filiais.isLoading} />
        <AsyncMultiSelect label="Plano Contas" opcoes={(planoContas.data ?? []).map((item) => item.classificacao)} selecionados={filtros.classificacoes ?? []} onChange={(valores) => setFiltro('classificacoes', valores)} isLoading={planoContas.isLoading} />
        <AsyncMultiSelect label="Pago" opcoes={['PAGO', 'Sim', 'Nao']} selecionados={filtros.pago ?? []} onChange={(valores) => setFiltro('pago', valores)} />
      </FilterBar>

      {overview.isError && <MensagemErro mensagem="Erro ao carregar indicadores de contas a pagar." />}
      {overview.data && <ContasAPagarKpiGrid overview={overview.data} />}

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper titulo="Pagos x Abertos por Mes" option={serieOption} isLoading={serie.isLoading} isEmpty={(serie.data ?? []).length === 0} />
        <ChartWrapper titulo="Top Fornecedores" option={fornecedorOption} isLoading={graficos.isLoading} isEmpty={rankingFornecedor.length === 0} />
      </div>

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper titulo="Centro de Custo" option={centroOption} isLoading={graficos.isLoading} isEmpty={centroCusto.length === 0} />
        <ChartWrapper titulo="Conciliação" option={conciliacaoOption} isLoading={graficos.isLoading} isEmpty={conciliacao.length === 0} />
      </div>

      <div className="mb-3 flex justify-end">
        <ExportButton dados={(tabela.data ?? []) as unknown as Record<string, unknown>[]} nomeArquivo="contas-a-pagar" />
      </div>
      <DataTable titulo="Lançamentos Analiticos" dados={tabela.data ?? []} colunas={colunas} chaveLinha="lancamentoNumero" isLoading={tabela.isLoading} />
    </div>
  );
}
