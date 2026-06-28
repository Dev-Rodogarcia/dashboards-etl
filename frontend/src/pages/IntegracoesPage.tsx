import { useCallback, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Eye } from 'lucide-react';
import AnalyticalDataTable, {
  type ColunaTabelaAnalitica,
  type SortDirection,
} from '../components/shared/AnalyticalDataTable';
import CanhotoImagemModal from '../components/domain/integracoes/CanhotoImagemModal';
import KpiCard from '../components/shared/KpiCard';
import TooltipKpi from '../components/shared/TooltipKpi';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import {
  buscarIntegracoesAuditoria,
  type IntegracoesEscopo,
  type IntegracaoMetricaConsolidada,
  type IntegracaoPendencia,
} from '../api/endpoints/integracoesServico';
import { usePageHeader } from '../contexts/PageHeaderContext';
import { useAnalyticalTableFilters } from '../hooks/useAnalyticalTableFilters';
import { useTabelaPaginadaState } from '../hooks/useTabelaPaginadaState';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { formatarDataHora, formatarNumero, formatarPorcentagem } from '../utils/formatadores';
import { OPERATIONAL_QUERY_POLLING_OPTIONS } from '../utils/pollingUtils';
import { combinarStatusOptions } from '../utils/tableStatusOptions';

const QUERY_KEY = ['integracoes'];
const STATUS_PADRAO = ['SUCESSO', 'ERRO_DESTINO', 'PENDENTE_FOTO'];
const EMPTY_METRICAS: IntegracaoMetricaConsolidada[] = [];
const EMPTY_PENDENCIAS: IntegracaoPendencia[] = [];
const ABAS_INTEGRACOES: { valor: IntegracoesEscopo; label: string }[] = [
  { valor: 'PENDENCIAS', label: 'Pendências' },
  { valor: 'SUCESSO', label: 'Integrados com Sucesso' },
];

interface IntegracoesTableSort {
  field: keyof IntegracaoPendencia & string;
  direction: SortDirection;
}

function formatarPercentual(valor: number | null | undefined) {
  return typeof valor === 'number' && Number.isFinite(valor) ? formatarPorcentagem(valor, 2) : '-';
}

function tomPercentual(valor: number | null | undefined) {
  if (typeof valor !== 'number' || !Number.isFinite(valor)) return 'neutral';
  if (valor >= 95) return 'positive';
  if (valor >= 80) return 'warning';
  return 'negative';
}

function formatarInteiro(valor: unknown) {
  const numero = Number(valor);
  return Number.isFinite(numero) ? formatarNumero(numero) : '-';
}

function formatarData(valor: unknown) {
  return typeof valor === 'string' && valor.trim() ? formatarDataHora(valor) : '-';
}

function renderStatus(valor: unknown) {
  return valor ? <StatusBadge status={String(valor)} /> : '-';
}

function numeroSeguro(valor: unknown) {
  const numero = Number(valor);
  return Number.isFinite(numero) ? numero : 0;
}

function calcularVolumeTotal(metricas: IntegracaoMetricaConsolidada[]) {
  return metricas.reduce((total, item) => total + numeroSeguro(item.totalRegistros), 0);
}

function calcularTaxaSucessoIntegracao(metricas: IntegracaoMetricaConsolidada[]) {
  const volumeTotal = calcularVolumeTotal(metricas);
  if (volumeTotal <= 0) {
    return 0;
  }

  const sucessoPonderado = metricas.reduce(
    (total, item) => total + numeroSeguro(item.totalRegistros) * numeroSeguro(item.percentualXmlSucesso),
    0,
  );
  return sucessoPonderado / volumeTotal;
}

function temIndicadorImagem(item: IntegracaoPendencia) {
  return Boolean(
    item.canhotoReferencia?.trim()
      || item.possuiImagem
      || item.possuiImagemCanhoto
      || item.possuiImagemPayload
      || item.imagemDisponivel,
  );
}

function podeVisualizarCanhoto(item: IntegracaoPendencia) {
  return temIndicadorImagem(item) && Boolean(item.canhotoReferencia?.trim());
}

function criarColunas(
  onVerCanhoto: (item: IntegracaoPendencia) => void,
): ColunaTabelaAnalitica<IntegracaoPendencia>[] {
  return [
    { chave: 'sistemaDestino', label: 'Sistema Destino', fixo: true, largura: '160px' },
    { chave: 'numeroNf', label: 'NF', largura: '120px', formato: formatarInteiro, filtroTabela: 'codigo' },
    { chave: 'serieNf', label: 'Série', largura: '100px' },
    {
      chave: 'chaveNfe',
      label: 'Chave NF-e',
      largura: '360px',
      formato: (valor) => (
        <span className="block max-w-[360px] truncate" title={typeof valor === 'string' ? valor : undefined}>
          {typeof valor === 'string' && valor.trim() ? valor : '-'}
        </span>
      ),
    },
    { chave: 'statusDados', label: 'Status XML', largura: '160px', filtroTabela: 'status', formato: renderStatus },
    { chave: 'statusCanhoto', label: 'Status Comprovante', largura: '190px', filtroTabela: 'status', formato: renderStatus },
    { chave: 'dataProcessamento', label: 'Data de Processamento', largura: '210px', formato: formatarData },
    {
      chave: 'id',
      label: 'Canhoto',
      largura: '112px',
      ordenavel: false,
      filtravel: false,
      formato: (_valor, row) => {
        const habilitado = podeVisualizarCanhoto(row);

        return (
          <button
            type="button"
            onClick={() => {
              if (habilitado) {
                onVerCanhoto(row);
              }
            }}
            disabled={!habilitado}
            className="inline-flex h-8 w-8 items-center justify-center rounded-lg border transition-colors disabled:cursor-not-allowed disabled:opacity-45 hover:border-[var(--color-primary)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
            style={{
              backgroundColor: 'var(--color-bg)',
              borderColor: 'var(--color-border)',
              color: 'var(--color-text)',
            }}
            aria-label={habilitado ? 'Ver canhoto' : 'Canhoto indisponivel'}
            title={habilitado ? 'Ver canhoto' : 'Canhoto indisponivel'}
          >
            <Eye size={15} aria-hidden="true" />
          </button>
        );
      },
    },
  ];
}

export default function IntegracoesPage() {
  const [pendenciaCanhoto, setPendenciaCanhoto] = useState<IntegracaoPendencia | null>(null);
  const [abaSelecionada, setAbaSelecionada] = useState<IntegracoesEscopo>('PENDENCIAS');
  const [tableSort, setTableSort] = useState<IntegracoesTableSort | null>(null);
  const filtrosTabela = useAnalyticalTableFilters();
  const paginacaoTabela = useTabelaPaginadaState(`${filtrosTabela.resetKey}:${abaSelecionada}`);

  const integracoes = useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: [
      ...QUERY_KEY,
      paginacaoTabela.pagina,
      paginacaoTabela.tamanhoPagina,
      filtrosTabela.apiFilters,
      tableSort,
      abaSelecionada,
    ],
    queryFn: () => buscarIntegracoesAuditoria(
      paginacaoTabela.pagina,
      paginacaoTabela.tamanhoPagina,
      filtrosTabela.apiFilters,
      tableSort?.field,
      tableSort?.direction,
      abaSelecionada,
    ),
    staleTime: 60 * 1000,
    retry: 1,
  });

  const pendenciasResumo = useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: [...QUERY_KEY, 'pendencias-resumo', filtrosTabela.apiFilters],
    queryFn: () => buscarIntegracoesAuditoria(1, 1, filtrosTabela.apiFilters, undefined, undefined, 'PENDENCIAS'),
    enabled: abaSelecionada === 'SUCESSO',
    staleTime: 60 * 1000,
    retry: 1,
  });

  const abrirCanhoto = useCallback((item: IntegracaoPendencia) => {
    if (!podeVisualizarCanhoto(item)) {
      return;
    }

    setPendenciaCanhoto(item);
  }, []);

  const fecharCanhoto = useCallback(() => {
    setPendenciaCanhoto(null);
  }, []);

  const colunas = useMemo(() => criarColunas(abrirCanhoto), [abrirCanhoto]);

  usePageHeader({
    title: 'Integrações',
    description: 'Auditoria operacional de XML e comprovantes enviados para clientes.',
    updatedAt: integracoes.data?.geradoEm ?? null,
  });

  const metricas = integracoes.data?.metricasConsolidadas ?? EMPTY_METRICAS;
  const volumeTotal = calcularVolumeTotal(metricas);
  const taxaSucessoIntegracao = calcularTaxaSucessoIntegracao(metricas);
  const totalPendencias = abaSelecionada === 'PENDENCIAS'
    ? integracoes.data?.pendencias.paginacao.totalElementos
    : pendenciasResumo.data?.pendencias.paginacao.totalElementos;
  const pendencias = integracoes.data?.pendencias.itens ?? EMPTY_PENDENCIAS;
  const tituloTabela = abaSelecionada === 'PENDENCIAS'
    ? 'Pendências operacionais'
    : 'Integrados com sucesso';

  const statusOptions = useMemo(
    () => combinarStatusOptions(
      STATUS_PADRAO,
      pendencias.map((item) => item.statusDados),
      pendencias.map((item) => item.statusCanhoto),
      filtrosTabela.filters.status,
    ),
    [filtrosTabela.filters.status, pendencias],
  );

  return (
    <div className="w-full">
      {integracoes.isError && (
        <MensagemErro
          mensagem={getApiErrorMessage(integracoes.error, 'Erro ao carregar auditoria de integrações.')}
          tipo={getTipoErro(integracoes.error)}
        />
      )}

      <div className="mb-4 grid grid-cols-1 gap-3 md:grid-cols-3">
        <TooltipKpi kpiName="taxaSucessoIntegracao">
          <KpiCard
            label="Volume Total"
            valor={formatarInteiro(volumeTotal)}
            metaLabel="Origem"
            metaValue="Satélite"
          />
        </TooltipKpi>
        <TooltipKpi kpiName="taxaSucessoIntegracao">
          <KpiCard
            label="Taxa de Sucesso"
            valor={formatarPercentual(taxaSucessoIntegracao)}
            tone={tomPercentual(taxaSucessoIntegracao)}
            metaLabel="Base"
            metaValue="XML"
            progressPct={taxaSucessoIntegracao}
          />
        </TooltipKpi>
        <TooltipKpi kpiName="taxaSucessoIntegracao">
          <KpiCard
            label="Pendências"
            valor={formatarInteiro(totalPendencias)}
            tone={numeroSeguro(totalPendencias) > 0 ? 'warning' : 'positive'}
            metaLabel="Escopo"
            metaValue="PENDENCIAS"
          />
        </TooltipKpi>
      </div>

      <div
        role="tablist"
        aria-label="Escopo da auditoria de integrações"
        className="mb-4 inline-flex max-w-full overflow-hidden rounded-lg border p-1"
        style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}
      >
        {ABAS_INTEGRACOES.map((aba) => {
          const ativa = abaSelecionada === aba.valor;
          return (
            <button
              key={aba.valor}
              type="button"
              role="tab"
              aria-selected={ativa}
              onClick={() => {
                setAbaSelecionada(aba.valor);
                setPendenciaCanhoto(null);
              }}
              className="min-h-9 min-w-[132px] whitespace-nowrap rounded-md px-3 text-sm font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
              style={{
                backgroundColor: ativa ? 'var(--color-primary)' : 'transparent',
                color: ativa ? 'white' : 'var(--color-text)',
              }}
            >
              {aba.label}
            </button>
          );
        })}
      </div>

      <AnalyticalDataTable
        titulo={tituloTabela}
        dados={pendencias}
        colunas={colunas}
        chaveLinha="id"
        filtros={filtrosTabela.filters}
        hiddenActiveCount={filtrosTabela.hiddenActiveCount}
        hasAnyFilter={filtrosTabela.hasAnyFilter}
        onTextFilterChange={filtrosTabela.setTextFilter}
        onMultiFilterChange={filtrosTabela.setMultiFilter}
        onColumnFilterChange={filtrosTabela.setColumnFilter}
        onClearFilters={filtrosTabela.clearTableFilters}
        statusOptions={statusOptions}
        isLoading={integracoes.isLoading}
        isFetching={integracoes.isFetching}
        error={integracoes.error}
        errorFallbackMessage={`Erro ao carregar ${tituloTabela.toLowerCase()}.`}
        totalRegistros={integracoes.data?.pendencias.paginacao.totalElementos}
        paginaAtual={paginacaoTabela.pagina}
        tamanhoPagina={paginacaoTabela.tamanhoPagina}
        onPaginaChange={paginacaoTabela.setPagina}
        onTamanhoPaginaChange={paginacaoTabela.setTamanhoPagina}
        sortField={tableSort?.field}
        sortDirection={tableSort?.direction}
        onSortChange={(field, direction) => setTableSort({ field, direction })}
      />

      <CanhotoImagemModal
        pendencia={pendenciaCanhoto}
        onClose={fecharCanhoto}
      />
    </div>
  );
}
