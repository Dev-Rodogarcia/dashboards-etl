import { useCallback, useMemo, useState } from 'react';
import type { EChartsOption } from 'echarts';
import { ChevronDown, ChevronRight, ChevronUp } from 'lucide-react';
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
import { useColetasCidadesOrigem, useColetasGraficos, useColetasOverview, useColetasSerie, useColetasTabelaPaginada } from '../hooks/queries/useColetas';
import { useAnalyticalTableFilters } from '../hooks/useAnalyticalTableFilters';
import { useTabelaPaginadaState } from '../hooks/useTabelaPaginadaState';
import type { ColetaResumoRow, ColetasFiltro } from '../types/coletas';
import { CORES } from '../utils/chartColors';
import { formatarMoeda, formatarPeso } from '../utils/formatadores';
import { combinarStatusOptions } from '../utils/tableStatusOptions';

const AGING_BUCKETS = ['0-2 dias', '3-5 dias', '6-10 dias', '11+ dias'] as const;
const ORIGEM_LIMITES = [5, 10, 15] as const;
const EMPTY_ARRAY: never[] = [];

export default function ColetasPage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setDataRange, setFiltro, limparFiltros } = useFiltro();
  const [regiaoSelecionada, setRegiaoSelecionada] = useState<string | null>(null);
  const [regiaoEmFoco, setRegiaoEmFoco] = useState<string | null>(null);
  const [origemLimite, setOrigemLimite] = useState<(typeof ORIGEM_LIMITES)[number]>(10);
  const filiais = useFiliais();
  const clientes = useClientes();
  const usuarios = useUsuarios();

  const filtro: ColetasFiltro = useMemo(() => ({
    dataInicio,
    dataFim,
    filiais: filtros.filiais,
    clientes: filtros.clientes,
    status: filtros.status,
    regioes: filtros.regioes,
    usuarios: filtros.usuarios,
  }), [dataFim, dataInicio, filtros.clientes, filtros.filiais, filtros.regioes, filtros.status, filtros.usuarios]);

  const activeFilters: ActiveFilter[] = useMemo(() => [
    { label: 'Filiais', count: filtros.filiais?.length ?? 0, onRemove: () => setFiltro('filiais', []) },
    { label: 'Clientes', count: filtros.clientes?.length ?? 0, onRemove: () => setFiltro('clientes', []) },
    { label: 'Usuários', count: filtros.usuarios?.length ?? 0, onRemove: () => setFiltro('usuarios', []) },
  ], [filtros.clientes, filtros.filiais, filtros.usuarios, setFiltro]);

  const overview = useColetasOverview(filtro);
  const serie = useColetasSerie(filtro);
  const graficos = useColetasGraficos(filtro);
  const cidadesOrigem = useColetasCidadesOrigem(filtro, regiaoSelecionada);
  const filtrosTabela = useAnalyticalTableFilters();
  const paginacaoResetKey = useMemo(() => JSON.stringify({ filtro, tabela: filtrosTabela.resetKey }), [filtro, filtrosTabela.resetKey]);
  const paginacaoTabela = useTabelaPaginadaState(paginacaoResetKey);
  const tabela = useColetasTabelaPaginada(filtro, paginacaoTabela.pagina, paginacaoTabela.tamanhoPagina, filtrosTabela.apiFilters);

  usePageHeader({
    title: 'Coletas',
    description: 'SLA operacional, distribuição por status e coletas em aberto.',
    updatedAt: overview.data?.updatedAt ?? null,
  });

  const statusData = graficos.data?.statusDistribuicao ?? EMPTY_ARRAY;
  const slaPorFilial = graficos.data?.slaPorFilial ?? EMPTY_ARRAY;
  const regioesOrigem = graficos.data?.regioesOrigem ?? EMPTY_ARRAY;
  const cidadesOrigemData = cidadesOrigem.data ?? EMPTY_ARRAY;
  const serieData = serie.data ?? EMPTY_ARRAY;
  const tabelaConteudo = tabela.data?.conteudo ?? EMPTY_ARRAY;
  const usuariosNomes = useMemo(() => (usuarios.data ?? EMPTY_ARRAY).map((item) => item.nome), [usuarios.data]);
  const regioesOrigemOrdenadas = useMemo(() => regioesOrigem
    .map((item) => ({ nome: item.regiao, totalColetas: item.totalColetas, pesoTaxado: item.pesoTaxado }))
    .sort((a, b) => b.totalColetas - a.totalColetas), [regioesOrigem]);
  const cidadesOrigemOrdenadas = useMemo(() => cidadesOrigemData
    .map((item) => ({ nome: item.cidade, totalColetas: item.totalColetas, pesoTaxado: item.pesoTaxado }))
    .sort((a, b) => b.totalColetas - a.totalColetas), [cidadesOrigemData]);
  const origemData = useMemo(() => {
    const origemDataCompleta = regiaoSelecionada ? cidadesOrigemOrdenadas : regioesOrigemOrdenadas;
    return origemDataCompleta.slice(0, origemLimite);
  }, [cidadesOrigemOrdenadas, origemLimite, regiaoSelecionada, regioesOrigemOrdenadas]);
  const regiaoDestinoDrilldown = useMemo(() => {
    const regiaoEmFocoValida = regiaoEmFoco && regioesOrigemOrdenadas.some((item) => item.nome === regiaoEmFoco);
    return regiaoSelecionada ?? (regiaoEmFocoValida ? regiaoEmFoco : regioesOrigemOrdenadas[0]?.nome ?? null);
  }, [regiaoEmFoco, regiaoSelecionada, regioesOrigemOrdenadas]);
  const podeDrillDownOrigem = !regiaoSelecionada && Boolean(regiaoDestinoDrilldown);
  const agingMap = useMemo(() => new Map((graficos.data?.agingAbertas ?? EMPTY_ARRAY).map((item) => [item.faixa, item.total])), [graficos.data?.agingAbertas]);
  const aging = useMemo(() => AGING_BUCKETS.map((faixa) => ({ faixa, total: agingMap.get(faixa) ?? 0 })), [agingMap]);
  const statusTabelaOptions = useMemo(() => combinarStatusOptions(
    statusData.map((item) => item.status),
    tabelaConteudo.map((item) => item.status),
    filtros.status,
  ), [filtros.status, statusData, tabelaConteudo]);

  const statusOption: EChartsOption = useMemo(() => ({
    grid: { top: 24, right: 18, bottom: 18, left: 34, containLabel: true },
    xAxis: {
      type: 'category',
      data: statusData.map((item) => item.status),
      axisLabel: {
        interval: 0,
        formatter: (value: string) => value.length > 12 ? value.slice(0, 12) + '…' : value,
      },
    },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: statusData.map((item) => item.total),
      barMaxWidth: 42,
      itemStyle: { color: CORES.primaria, borderRadius: [4, 4, 0, 0] },
    }],
  }), [statusData]);

  const slaOption: EChartsOption = useMemo(() => ({
    grid: { top: 20, right: 18, bottom: 24, left: 10, containLabel: true },
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
    series: [{
      type: 'bar',
      data: slaPorFilial.map((item) => item.slaPct).reverse(),
      barMaxWidth: 16,
      itemStyle: { color: CORES.sucesso, borderRadius: [0, 4, 4, 0] },
    }],
  }), [slaPorFilial]);

  const origemOption: EChartsOption = useMemo(() => ({
    legend: { show: false },
    grid: { top: 28, right: 54, bottom: 34, left: 42, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: unknown) => {
        const entries = params as { marker?: string; seriesName: string; name: string; value: number }[];
        const primeira = entries[0];
        return [
          primeira?.name ?? '',
          ...entries.map((item) => {
            const valor = item.seriesName === 'Peso Taxado'
              ? formatarPeso(Number(item.value ?? 0))
              : Number(item.value ?? 0).toLocaleString('pt-BR');
            return `${item.marker ?? ''}${item.seriesName}: ${valor}`;
          }),
        ].join('<br/>');
      },
    },
    xAxis: {
      type: 'category',
      data: origemData.map((item) => item.nome),
      axisLabel: {
        interval: 0,
        rotate: 24,
        formatter: (value: string) => value.length > 14 ? value.slice(0, 14) + '…' : value,
      },
    },
    yAxis: [
      { type: 'value', name: 'Coletas' },
      {
        type: 'value',
        name: 'Peso',
        position: 'right',
        axisLabel: {
          formatter: (value: number) => formatarPeso(value).replace(' ', ''),
        },
      },
    ],
    series: [
      {
        name: 'Coletas',
        type: 'bar',
        data: origemData.map((item) => item.totalColetas),
        barMaxWidth: 42,
        cursor: regiaoSelecionada ? 'default' : 'pointer',
        itemStyle: {
          color: regiaoSelecionada ? CORES.sucesso : CORES.primaria,
          borderRadius: [4, 4, 0, 0],
        },
        emphasis: {
          itemStyle: { color: regiaoSelecionada ? CORES.sucesso : '#2f5fb3' },
        },
      },
      {
        name: 'Peso Taxado',
        type: 'line',
        yAxisIndex: 1,
        data: origemData.map((item) => item.pesoTaxado),
        smooth: true,
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: { color: CORES.secundaria },
        lineStyle: { color: CORES.secundaria, width: 2 },
      },
    ],
  }), [origemData, regiaoSelecionada]);

  const handleOrigemClick = useCallback((params: unknown) => {
    if (regiaoSelecionada) {
      return;
    }
    const nome = (params as { name?: string }).name;
    if (nome) {
      setRegiaoEmFoco(nome);
      setRegiaoSelecionada(nome);
    }
  }, [regiaoSelecionada]);

  const handleOrigemMouseOver = useCallback((params: unknown) => {
    if (regiaoSelecionada) {
      return;
    }
    const nome = (params as { name?: string }).name;
    if (nome) {
      setRegiaoEmFoco(nome);
    }
  }, [regiaoSelecionada]);

  const origemEvents = useMemo(() => ({
    click: handleOrigemClick,
    mouseover: handleOrigemMouseOver,
  }), [handleOrigemClick, handleOrigemMouseOver]);

  const fazerDrillUpOrigem = useCallback(() => {
    if (!regiaoSelecionada) {
      return;
    }
    setRegiaoEmFoco(regiaoSelecionada);
    setRegiaoSelecionada(null);
  }, [regiaoSelecionada]);

  const fazerDrillDownOrigem = useCallback(() => {
    if (!podeDrillDownOrigem || !regiaoDestinoDrilldown) {
      return;
    }
    setRegiaoEmFoco(regiaoDestinoDrilldown);
    setRegiaoSelecionada(regiaoDestinoDrilldown);
  }, [podeDrillDownOrigem, regiaoDestinoDrilldown]);

  const origemActions = useMemo(() => (
    <div className="flex flex-wrap items-center justify-end gap-2">
      <select
        aria-label="Quantidade exibida"
        value={origemLimite}
        onChange={(event) => setOrigemLimite(Number(event.target.value) as (typeof ORIGEM_LIMITES)[number])}
        className="h-8 rounded-md border bg-transparent px-2 text-[11px] font-semibold outline-none"
        style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-muted)' }}
      >
        {ORIGEM_LIMITES.map((limite) => (
          <option key={limite} value={limite}>
            Top {limite}
          </option>
        ))}
      </select>
      <div className="flex min-w-0 items-center gap-1 rounded-lg border px-1 py-0.5" style={{ borderColor: 'var(--color-border)' }}>
        <button
          type="button"
          title="Drill up"
          aria-label="Drill up para regiões"
          disabled={!regiaoSelecionada}
          onClick={fazerDrillUpOrigem}
          className="flex h-7 w-7 items-center justify-center rounded-md transition disabled:cursor-not-allowed disabled:opacity-35"
          style={{ color: regiaoSelecionada ? CORES.primaria : 'var(--color-text-muted)' }}
        >
          <ChevronUp size={14} />
        </button>
        <button
          type="button"
          onClick={fazerDrillUpOrigem}
          className="rounded-md px-2 py-1 text-[11px] font-semibold transition"
          style={{
            backgroundColor: !regiaoSelecionada ? `${CORES.primaria}1F` : 'transparent',
            color: !regiaoSelecionada ? CORES.primaria : 'var(--color-text-muted)',
          }}
        >
          Regiões
        </button>
        <ChevronRight size={12} style={{ color: 'var(--color-text-subtle)' }} />
        <button
          type="button"
          title={regiaoSelecionada ? `Cidades de ${regiaoSelecionada}` : regiaoDestinoDrilldown ? `Drill down para ${regiaoDestinoDrilldown}` : 'Sem regiões para drill down'}
          disabled={!podeDrillDownOrigem && !regiaoSelecionada}
          onClick={() => {
            if (!regiaoSelecionada) {
              fazerDrillDownOrigem();
            }
          }}
          className="max-w-36 truncate rounded-md px-2 py-1 text-[11px] font-semibold transition disabled:cursor-not-allowed disabled:opacity-55"
          style={{
            backgroundColor: regiaoSelecionada ? `${CORES.sucesso}1F` : 'transparent',
            color: regiaoSelecionada || podeDrillDownOrigem ? CORES.sucesso : 'var(--color-text-muted)',
          }}
        >
          Cidades
        </button>
        <button
          type="button"
          title={regiaoDestinoDrilldown ? `Drill down para ${regiaoDestinoDrilldown}` : 'Sem regiões para drill down'}
          aria-label="Drill down para cidades"
          disabled={!podeDrillDownOrigem}
          onClick={fazerDrillDownOrigem}
          className="flex h-7 w-7 items-center justify-center rounded-md transition disabled:cursor-not-allowed disabled:opacity-35"
          style={{ color: podeDrillDownOrigem ? CORES.sucesso : 'var(--color-text-muted)' }}
        >
          <ChevronDown size={14} />
        </button>
      </div>
    </div>
  ), [fazerDrillDownOrigem, fazerDrillUpOrigem, origemLimite, podeDrillDownOrigem, regiaoDestinoDrilldown, regiaoSelecionada]);

  const agingOption: EChartsOption = useMemo(() => ({
    grid: { top: 22, right: 18, bottom: 32, left: 34, containLabel: true },
    xAxis: { type: 'category', data: aging.map((item) => item.faixa) },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: aging.map((item) => item.total),
      barMaxWidth: 72,
      itemStyle: { color: CORES.aviso, borderRadius: [4, 4, 0, 0] },
    }],
  }), [aging]);

  const colunas: ColunaTabelaAnalitica<ColetaResumoRow>[] = useMemo(() => [
    { chave: 'id', label: 'ID', fixo: true, filtroTabela: 'codigo' },
    { chave: 'coleta', label: 'Coleta' },
    { chave: 'solicitacao', label: 'Solicitação' },
    { chave: 'status', label: 'Status', filtroTabela: 'status', formato: (valor) => <StatusBadge status={String(valor)} /> },
    { chave: 'filial', label: 'Filial' },
    { chave: 'cliente', label: 'Cliente', largura: '220px', filtroTabela: 'razaoSocial' },
    { chave: 'regiaoColeta', label: 'Região', filtroTabela: 'origem' },
    { chave: 'volumes', label: 'Volumes' },
    { chave: 'pesoTaxado', label: 'Peso', formato: (valor) => formatarPeso(Number(valor ?? 0)) },
    { chave: 'valorNf', label: 'Valor NF', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'numeroTentativas', label: 'Tentativas' },
  ], []);

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
          opcoes={filiais.data ?? EMPTY_ARRAY}
          selecionados={filtros.filiais ?? []}
          onChange={(valores) => setFiltro('filiais', valores)}
          isLoading={filiais.isLoading}
        />
        <AsyncMultiSelect
          label="Clientes"
          opcoes={clientes.data ?? EMPTY_ARRAY}
          selecionados={filtros.clientes ?? []}
          onChange={(valores) => setFiltro('clientes', valores)}
          isLoading={clientes.isLoading}
        />
        <AsyncMultiSelect
          label="Usuários"
          opcoes={usuariosNomes}
          selecionados={filtros.usuarios ?? []}
          onChange={(valores) => setFiltro('usuarios', valores)}
          isLoading={usuarios.isLoading}
        />
      </FilterBar>

      {overview.isError && <MensagemErro mensagem={getApiErrorMessage(overview.error, 'Erro ao carregar indicadores de coletas.')} tipo={getTipoErro(overview.error)} />}
      {overview.data && <ColetasKpiGrid overview={overview.data} />}

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-3">
        <ColetasTrend dados={serieData} isLoading={serie.isLoading} />
        <ChartWrapper titulo="Distribuição por Status" option={statusOption} isLoading={graficos.isLoading} isEmpty={statusData.length === 0} />
        <ChartWrapper titulo="SLA por Filial" option={slaOption} isLoading={graficos.isLoading} isEmpty={slaPorFilial.length === 0} />
      </div>

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper
          titulo="Coletas por Região de Origem e Cidade"
          option={origemOption}
          actions={origemActions}
          onEvents={origemEvents}
          isLoading={regiaoSelecionada ? cidadesOrigem.isLoading : graficos.isLoading}
          isEmpty={origemData.length === 0}
          emptyMessage={regiaoSelecionada ? 'Nenhuma cidade encontrada para a região selecionada.' : 'Nenhuma região encontrada para o período selecionado.'}
        />
        <ChartWrapper titulo="Coletas em aberto" option={agingOption} isLoading={graficos.isLoading} isEmpty={false} altura={300} />
      </div>

      <div className="mb-3 flex justify-end">
        <ExportButton nomeArquivo="coletas" onExport={() => exportarColetasCsv(filtro, filtrosTabela.apiFilters)} />
      </div>
      <AnalyticalDataTable
        titulo="Coletas Analíticas"
        dados={tabelaConteudo}
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
        error={tabela.error}
        errorFallbackMessage="Erro ao carregar coletas analíticas."
        totalRegistros={tabela.data?.totalElementos}
        paginaAtual={paginacaoTabela.pagina}
        tamanhoPagina={paginacaoTabela.tamanhoPagina}
        onPaginaChange={paginacaoTabela.setPagina}
        onTamanhoPaginaChange={paginacaoTabela.setTamanhoPagina}
      />
    </div>
  );
}
