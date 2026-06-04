import { useCallback, useMemo } from 'react';
import type { EChartsOption } from 'echarts';
import { useSearchParams } from 'react-router-dom';
import ChartWrapper from '../components/charts/ChartWrapper';
import ManifestosGaugeCard from '../components/domain/manifestos/ManifestosGaugeCard';
import ManifestosKpiGrid from '../components/domain/manifestos/ManifestosKpiGrid';
import ManifestosTrend from '../components/domain/manifestos/ManifestosTrend';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import AnalyticalDataTable, { type ColunaTabelaAnalitica } from '../components/shared/AnalyticalDataTable';
import DateRangePicker from '../components/shared/DateRangePicker';
import ExportButton from '../components/shared/ExportButton';
import FilterBar, { type ActiveFilter } from '../components/shared/FilterBar';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import { exportarManifestosCsv } from '../api/endpoints/manifestosServico';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { useFiltro } from '../contexts/FiltroContext';
import { usePageHeader } from '../contexts/PageHeaderContext';
import { useFiliais, useMotoristas, useVeiculos } from '../hooks/queries/useDimensoes';
import { useManifestosPerformance, useManifestosTabelaPaginada } from '../hooks/queries/useManifestos';
import { useAnalyticalTableFilters } from '../hooks/useAnalyticalTableFilters';
import { useTabelaPaginadaState } from '../hooks/useTabelaPaginadaState';
import type { ManifestoResumoRow, ManifestosFiltro, ManifestosTempoNivel } from '../types/manifestos';
import { CORES } from '../utils/chartColors';
import { formatarMoeda, formatarNumero, formatarPeso } from '../utils/formatadores';
import { combinarStatusOptions } from '../utils/tableStatusOptions';

const NIVEL_PARAM = 'manifestosNivel';
const ANO_PARAM = 'manifestosAno';
const MES_PARAM = 'manifestosMes';
const CORES_GAUGE_MANIFESTOS = {
  remuneracao: '#1d4ed8',
  aproveitamento: '#059669',
  efetividade: '#ea580c',
} as const;
const EMPTY_ARRAY: never[] = [];

function normalizarNivel(valor: string | null): ManifestosTempoNivel {
  return valor === 'ano' || valor === 'mes' || valor === 'dia' ? valor : 'dia';
}

function numeroParam(valor: string | null): number | null {
  if (!valor) return null;
  const parsed = Number.parseInt(valor, 10);
  return Number.isFinite(parsed) ? parsed : null;
}

export default function ManifestosPage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setDataRange, setFiltro, limparFiltros } = useFiltro();
  const [searchParams, setSearchParams] = useSearchParams();
  const filiais = useFiliais();
  const motoristas = useMotoristas();
  const veiculos = useVeiculos();
  const nivelTemporal = normalizarNivel(searchParams.get(NIVEL_PARAM));
  const anoTemporal = numeroParam(searchParams.get(ANO_PARAM));
  const mesTemporal = numeroParam(searchParams.get(MES_PARAM));

  const filtro: ManifestosFiltro = useMemo(() => ({
    dataInicio,
    dataFim,
    filiais: filtros.filiais,
    status: filtros.status,
    motoristas: filtros.motoristas,
    veiculos: filtros.veiculos,
    tiposCarga: filtros.tiposCarga,
    tiposContrato: filtros.tiposContrato,
    tipoMotorista: filtros.tipoMotorista,
  }), [
    dataFim,
    dataInicio,
    filtros.filiais,
    filtros.motoristas,
    filtros.status,
    filtros.tipoMotorista,
    filtros.tiposCarga,
    filtros.tiposContrato,
    filtros.veiculos,
  ]);

  const activeFilters: ActiveFilter[] = useMemo(() => [
    { label: 'Filiais', count: filtros.filiais?.length ?? 0, onRemove: () => setFiltro('filiais', []) },
    { label: 'Motoristas', count: filtros.motoristas?.length ?? 0, onRemove: () => setFiltro('motoristas', []) },
    { label: 'Veículos', count: filtros.veiculos?.length ?? 0, onRemove: () => setFiltro('veiculos', []) },
  ], [filtros.filiais, filtros.motoristas, filtros.veiculos, setFiltro]);

  const performance = useManifestosPerformance(filtro, nivelTemporal, anoTemporal, mesTemporal);
  const filtrosTabela = useAnalyticalTableFilters();
  const paginacaoResetKey = useMemo(() => JSON.stringify({ filtro, tabela: filtrosTabela.resetKey }), [filtro, filtrosTabela.resetKey]);
  const paginacaoTabela = useTabelaPaginadaState(paginacaoResetKey);
  const tabela = useManifestosTabelaPaginada(filtro, paginacaoTabela.pagina, paginacaoTabela.tamanhoPagina, filtrosTabela.apiFilters);

  usePageHeader({
    title: 'Manifestos - Performance de Veículos',
    description: 'Custos, ocupação de carga e performance por motorista.',
    updatedAt: performance.data?.updatedAt ?? null,
  });

  const dadosPerformance = performance.data;
  const statusSazonal = useMemo(() => dadosPerformance?.statusSazonal ?? EMPTY_ARRAY, [dadosPerformance?.statusSazonal]);
  const custosMotorista = useMemo(() => dadosPerformance?.custosMotorista ?? EMPTY_ARRAY, [dadosPerformance?.custosMotorista]);
  const tiposVeiculo = useMemo(() => dadosPerformance?.tiposVeiculo ?? EMPTY_ARRAY, [dadosPerformance?.tiposVeiculo]);
  const tabelaConteudo = tabela.data?.conteudo ?? EMPTY_ARRAY;
  const veiculoPlacas = useMemo(() => (veiculos.data ?? EMPTY_ARRAY).map((item) => item.placa), [veiculos.data]);
  const statusTabelaOptions = useMemo(() => combinarStatusOptions(
    ['encerrado', 'em trânsito', 'pendente'],
    tabelaConteudo.map((item) => item.status),
    filtros.status,
  ), [filtros.status, tabelaConteudo]);

  const alterarNivelTemporal = useCallback((nivel: ManifestosTempoNivel) => {
    const next = new URLSearchParams(searchParams);
    next.set(NIVEL_PARAM, nivel);
    next.delete(ANO_PARAM);
    next.delete(MES_PARAM);
    setSearchParams(next, { replace: true });
  }, [searchParams, setSearchParams]);

  const drillTemporal = useCallback((data: string) => {
    const [ano, mes] = data.split('-');
    const next = new URLSearchParams(searchParams);
    if (nivelTemporal === 'ano') {
      next.set(NIVEL_PARAM, 'mes');
      next.set(ANO_PARAM, ano);
      next.delete(MES_PARAM);
    } else if (nivelTemporal === 'mes') {
      next.set(NIVEL_PARAM, 'dia');
      next.set(ANO_PARAM, ano);
      next.set(MES_PARAM, mes);
    }
    setSearchParams(next, { replace: true });
  }, [nivelTemporal, searchParams, setSearchParams]);

  const statusTrend = useMemo(() => statusSazonal.map((item) => ({
    date: item.data,
    encerrado: item.encerrado,
    emTransito: item.emTransito,
    pendente: item.pendente,
  })), [statusSazonal]);

  const custosMotoristaOption: EChartsOption = useMemo(() => ({
    tooltip: {
      trigger: 'item',
      formatter: (params: unknown) => {
        const item = params as { name?: string; value?: number; percent?: number };
        return `${item.name ?? ''}<br/>${formatarMoeda(Number(item.value ?? 0))}<br/>${formatarNumero(Number(item.percent ?? 0), 1)}%`;
      },
    },
    legend: {
      bottom: 0,
      type: 'scroll',
    },
    series: [
      {
        type: 'pie',
        radius: ['42%', '70%'],
        center: ['50%', '45%'],
        data: custosMotorista.map((item) => ({ name: item.tipo, value: item.custo })),
      },
    ],
  }), [custosMotorista]);

  const tiposVeiculoOption: EChartsOption = useMemo(() => ({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { top: 34, right: 10, bottom: 4, left: 44, containLabel: true },
    xAxis: {
      type: 'category',
      data: tiposVeiculo.map((item) => item.tipo),
      axisLabel: {
        interval: 0,
        rotate: tiposVeiculo.length > 5 ? 35 : 0,
        formatter: (value: string) => value.length > 16 ? `${value.slice(0, 16)}...` : value,
      },
    },
    yAxis: { type: 'value', name: 'Qtd' },
    series: [
      {
        type: 'bar',
        data: tiposVeiculo.map((item) => item.quantidade),
        itemStyle: { color: CORES.primaria },
        barMaxWidth: 42,
      },
    ],
  }), [tiposVeiculo]);

  const colunas: ColunaTabelaAnalitica<ManifestoResumoRow>[] = useMemo(() => [
    { chave: 'numero', label: 'Manifesto', fixo: true, filtroTabela: 'codigo' },
    { chave: 'status', label: 'Status', filtroTabela: 'status', formato: (valor) => <StatusBadge status={String(valor)} /> },
    { chave: 'filial', label: 'Filial' },
    { chave: 'motorista', label: 'Motorista' },
    { chave: 'veiculoPlaca', label: 'Veículo', filtroTabela: 'placa' },
    { chave: 'dataCriacao', label: 'Criação' },
    { chave: 'totalPesoTaxado', label: 'Peso', formato: (valor) => formatarPeso(Number(valor ?? 0)) },
    { chave: 'totalM3', label: 'M3', formato: (valor) => formatarNumero(Number(valor ?? 0), 2) },
    { chave: 'custoTotal', label: 'Custo', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'valorFrete', label: 'Valor Frete', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'kmTotal', label: 'KM', formato: (valor) => formatarNumero(Number(valor ?? 0), 0) },
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
          label="Motoristas"
          opcoes={motoristas.data ?? EMPTY_ARRAY}
          selecionados={filtros.motoristas ?? []}
          onChange={(valores) => setFiltro('motoristas', valores)}
          isLoading={motoristas.isLoading}
        />
        <AsyncMultiSelect
          label="Veículos"
          opcoes={veiculoPlacas}
          selecionados={filtros.veiculos ?? []}
          onChange={(valores) => setFiltro('veiculos', valores)}
          isLoading={veiculos.isLoading}
        />
      </FilterBar>

      {performance.isError && <MensagemErro mensagem={getApiErrorMessage(performance.error, 'Erro ao carregar indicadores de manifestos.')} tipo={getTipoErro(performance.error)} />}
      <ManifestosKpiGrid kpis={dadosPerformance?.kpis} isLoading={performance.isLoading} />

      <div className="mb-6 grid grid-cols-1 gap-4 xl:grid-cols-3">
        <ManifestosGaugeCard
          titulo="Remuneração (Custo x Receita Transportada)"
          metric={dadosPerformance?.remuneracao}
          isLoading={performance.isLoading}
          corDestaque={CORES_GAUGE_MANIFESTOS.remuneracao}
        />
        <ManifestosGaugeCard
          titulo="Aproveitamento (Peso Transportado x Capacidade do Veículo)"
          metric={dadosPerformance?.aproveitamento}
          isLoading={performance.isLoading}
          corDestaque={CORES_GAUGE_MANIFESTOS.aproveitamento}
        />
        <ManifestosGaugeCard
          titulo="Efetividade (quantidade de serviços x quantidade de serviços finalizados)"
          metric={dadosPerformance?.efetividade}
          isLoading={performance.isLoading}
          corDestaque={CORES_GAUGE_MANIFESTOS.efetividade}
        />
      </div>

      <div className="mb-6 grid grid-cols-1 gap-4 xl:grid-cols-3">
        <ManifestosTrend
          dados={statusTrend}
          nivel={nivelTemporal}
          onNivelChange={alterarNivelTemporal}
          onPointClick={drillTemporal}
          isLoading={performance.isLoading}
        />
        <ChartWrapper
          titulo="Custos por Tipo de Motorista"
          option={custosMotoristaOption}
          isLoading={performance.isLoading}
          isEmpty={custosMotorista.length === 0}
        />
        <ChartWrapper
          titulo="Tipo de Veículos Utilizados"
          option={tiposVeiculoOption}
          isLoading={performance.isLoading}
          isEmpty={tiposVeiculo.length === 0}
        />
      </div>

      <div className="mb-3 flex justify-end">
        <ExportButton nomeArquivo="manifestos" onExport={() => exportarManifestosCsv(filtro, filtrosTabela.apiFilters)} />
      </div>
      <AnalyticalDataTable
        titulo="Manifestos Analíticos"
        dados={tabelaConteudo}
        colunas={colunas}
        chaveLinha="identificadorUnico"
        filtros={filtrosTabela.filters}
        hiddenActiveCount={filtrosTabela.hiddenActiveCount}
        hasAnyFilter={filtrosTabela.hasAnyFilter}
        onTextFilterChange={filtrosTabela.setTextFilter}
        onMultiFilterChange={filtrosTabela.setMultiFilter}
        onColumnFilterChange={filtrosTabela.setColumnFilter}
        onClearFilters={filtrosTabela.clearTableFilters}
        statusOptions={statusTabelaOptions}
        isLoading={tabela.isLoading}
        error={tabela.error}
        errorFallbackMessage="Erro ao carregar manifestos analíticos."
        totalRegistros={tabela.data?.totalElementos}
        paginaAtual={paginacaoTabela.pagina}
        tamanhoPagina={paginacaoTabela.tamanhoPagina}
        onPaginaChange={paginacaoTabela.setPagina}
        onTamanhoPaginaChange={paginacaoTabela.setTamanhoPagina}
      />
    </div>
  );
}
