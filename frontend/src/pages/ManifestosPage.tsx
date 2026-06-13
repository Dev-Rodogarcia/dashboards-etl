import { useCallback, useMemo, useState } from 'react';
import type { EChartsOption } from 'echarts';
import { Settings } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import ChartWrapper from '../components/charts/ChartWrapper';
import { useEchartsTheme } from '../components/charts/useEchartsTheme';
import ManifestosCustoEvolutionCard from '../components/domain/manifestos/ManifestosCustoEvolutionCard';
import ManifestosCostGoalsPanel from '../components/domain/manifestos/ManifestosCostGoalsPanel';
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
import { KpiDictionary } from '../constants/kpiDictionary';
import { exportarManifestosCsv } from '../api/endpoints/manifestosServico';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { useFiltro } from '../contexts/FiltroContext';
import { usePageHeader } from '../contexts/PageHeaderContext';
import { useFiliais, useMotoristas, useVeiculos } from '../hooks/queries/useDimensoes';
import { useManifestosPerformance, useManifestosTabelaPaginada } from '../hooks/queries/useManifestos';
import { useAnalyticalTableFilters } from '../hooks/useAnalyticalTableFilters';
import { usePermissions } from '../hooks/usePermissions';
import { useTabelaPaginadaState } from '../hooks/useTabelaPaginadaState';
import type { ManifestoResumoRow, ManifestosFiltro, ManifestosTempoNivel } from '../types/manifestos';
import { buildBaseBarOption, buildBaseDonutOption } from '../utils/echartsBuilders';
import { formatarMoeda, formatarNumero, formatarPeso } from '../utils/formatadores';
import { combinarStatusOptions } from '../utils/tableStatusOptions';

const NIVEL_PARAM = 'manifestosNivel';
const ANO_PARAM = 'manifestosAno';
const MES_PARAM = 'manifestosMes';
const CORES_GAUGE_MANIFESTOS = {
  remuneracao: 'var(--color-primary)',
  aproveitamento: 'var(--color-positive-fill)',
  efetividade: 'var(--color-warning-fill)',
} as const;
const EMPTY_ARRAY: never[] = [];
const KPI_BADGE_BASE_CLASS = 'inline-flex min-w-[68px] items-center justify-center rounded-md px-2 py-1 text-xs font-semibold tabular-nums';

type ManifestoTabelaRow = ManifestoResumoRow & {
  percentualRemuneracao: number | null;
  percentualAproveitamento: number | null;
  percentualEfetividade: number | null;
};

function calcularPercentual(
  numerador: number | null | undefined,
  denominador: number | null | undefined,
): number | null {
  if (
    numerador == null
    || denominador == null
    || !Number.isFinite(numerador)
    || !Number.isFinite(denominador)
    || denominador <= 0
  ) {
    return null;
  }

  return (numerador / denominador) * 100;
}

function renderPercentualBadge(percentual: number | null, classeCor: string) {
  if (percentual == null) {
    return '—';
  }

  return (
    <span className={`${KPI_BADGE_BASE_CLASS} ${classeCor}`}>
      {formatarNumero(percentual, 1)}%
    </span>
  );
}

function classeRemuneracao(percentual: number): string {
  if (percentual <= 20) return 'bg-green-500/10 text-green-500';
  if (percentual <= 30) return 'bg-yellow-500/10 text-yellow-600';
  return 'bg-red-500/10 text-red-500';
}

function classeAproveitamento(percentual: number): string {
  if (percentual > 80) return 'bg-green-500/10 text-green-500';
  if (percentual >= 40) return 'bg-yellow-500/10 text-yellow-600';
  return 'bg-red-500/10 text-red-500';
}

function classeEfetividade(percentual: number): string {
  if (percentual > 70) return 'bg-green-500/10 text-green-500';
  if (percentual >= 60) return 'bg-yellow-500/10 text-yellow-600';
  return 'bg-red-500/10 text-red-500';
}

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
  const [isMetasPanelOpen, setIsMetasPanelOpen] = useState(false);
  const [searchParams, setSearchParams] = useSearchParams();
  const { isDark } = useEchartsTheme();
  const { canAccess } = usePermissions();
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

  const canManageManifestosGoals = canAccess('can_manage_kpi_goals');
  const dadosPerformance = performance.data;
  const statusSazonal = useMemo(() => dadosPerformance?.statusSazonal ?? EMPTY_ARRAY, [dadosPerformance?.statusSazonal]);
  const custosMotorista = useMemo(() => dadosPerformance?.custosMotorista ?? EMPTY_ARRAY, [dadosPerformance?.custosMotorista]);
  const tiposVeiculo = useMemo(() => dadosPerformance?.tiposVeiculo ?? EMPTY_ARRAY, [dadosPerformance?.tiposVeiculo]);
  const tabelaConteudo = useMemo<ManifestoTabelaRow[]>(
    () => (tabela.data?.conteudo ?? EMPTY_ARRAY).map((row) => ({
      ...row,
      percentualRemuneracao: calcularPercentual(row.custoTotal, row.receitaTotalTransportada),
      percentualAproveitamento: calcularPercentual(row.totalPesoTaxado, row.capacidadeKg),
      percentualEfetividade: calcularPercentual(row.itensFinalizados, row.itensTotal),
    })),
    [tabela.data?.conteudo],
  );
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

  const custosMotoristaOption: EChartsOption = useMemo(() => buildBaseDonutOption(isDark, {
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
        name: 'Custos',
        type: 'pie',
        data: custosMotorista.map((item) => ({ name: item.tipo, value: item.custo })),
      },
    ],
  }), [custosMotorista, isDark]);

  const tiposVeiculoOption: EChartsOption = useMemo(() => buildBaseBarOption(isDark, {
    tooltip: { trigger: 'axis' },
    legend: { show: false },
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
        name: 'Veículos',
        type: 'bar',
        data: tiposVeiculo.map((item) => item.quantidade),
      },
    ],
  }), [isDark, tiposVeiculo]);

  const colunas: ColunaTabelaAnalitica<ManifestoTabelaRow>[] = useMemo(() => [
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
    {
      chave: 'percentualRemuneracao',
      label: '% Remuneração',
      tooltip: KpiDictionary.manifestos.remuneracao.geral.calculo,
      formato: (_, row) => {
        const percentual = calcularPercentual(row.custoTotal, row.receitaTotalTransportada);
        return renderPercentualBadge(percentual, percentual == null ? '' : classeRemuneracao(percentual));
      },
    },
    {
      chave: 'percentualAproveitamento',
      label: '% Aproveitamento',
      tooltip: KpiDictionary.manifestos.aproveitamento.geral.calculo,
      formato: (_, row) => {
        const percentual = calcularPercentual(row.totalPesoTaxado, row.capacidadeKg);
        return renderPercentualBadge(percentual, percentual == null ? '' : classeAproveitamento(percentual));
      },
    },
    {
      chave: 'percentualEfetividade',
      label: '% Efetividade',
      tooltip: KpiDictionary.manifestos.efetividade.geral.calculo,
      formato: (_, row) => {
        const percentual = calcularPercentual(row.itensFinalizados, row.itensTotal);
        return renderPercentualBadge(percentual, percentual == null ? '' : classeEfetividade(percentual));
      },
    },
  ], []);

  return (
    <div className="w-full">
      <FilterBar
        onClear={limparFiltros}
        activeFilters={activeFilters}
        dataInicio={dataInicio}
        dataFim={dataFim}
        actions={canManageManifestosGoals ? (
          <button
            type="button"
            onClick={() => setIsMetasPanelOpen((current) => !current)}
            className="inline-flex shrink-0 items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-sm font-medium transition-all duration-150 hover:bg-[var(--color-bg)] active:scale-[0.97] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
            style={{ color: 'var(--color-text)' }}
          >
            <Settings size={14} aria-hidden="true" />
            Gerenciar Metas
          </button>
        ) : null}
      >
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

      {canManageManifestosGoals ? (
        <ManifestosCostGoalsPanel open={isMetasPanelOpen} />
      ) : null}

      {performance.isError && <MensagemErro mensagem={getApiErrorMessage(performance.error, 'Erro ao carregar indicadores de manifestos.')} tipo={getTipoErro(performance.error)} />}
      <ManifestosKpiGrid kpis={dadosPerformance?.kpis} isLoading={performance.isLoading} />

      <div className="mb-6 grid grid-cols-1 items-stretch gap-4">
        <div className="min-h-[43rem] lg:min-h-[34rem] xl:h-[25rem] xl:min-h-0">
          <ManifestosCustoEvolutionCard
            dados={dadosPerformance?.custosEvolucao}
            isLoading={performance.isLoading}
          />
        </div>
      </div>

      <div className="mb-6 grid grid-cols-1 gap-4 xl:grid-cols-3">
        <ManifestosGaugeCard
          titulo="Remuneração (Custo x Receita Transportada)"
          metric={dadosPerformance?.remuneracao}
          isLoading={performance.isLoading}
          corDestaque={CORES_GAUGE_MANIFESTOS.remuneracao}
          definitions={KpiDictionary.manifestos.remuneracao}
        />
        <ManifestosGaugeCard
          titulo="Aproveitamento (Peso Transportado x Capacidade do Veículo)"
          metric={dadosPerformance?.aproveitamento}
          isLoading={performance.isLoading}
          corDestaque={CORES_GAUGE_MANIFESTOS.aproveitamento}
          definitions={KpiDictionary.manifestos.aproveitamento}
        />
        <ManifestosGaugeCard
          titulo="Efetividade (quantidade de serviços x quantidade de serviços finalizados)"
          metric={dadosPerformance?.efetividade}
          isLoading={performance.isLoading}
          corDestaque={CORES_GAUGE_MANIFESTOS.efetividade}
          definitions={KpiDictionary.manifestos.efetividade}
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
        isFetching={tabela.isFetching}
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
