import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import AnalyticalDataTable, { type ColunaTabelaAnalitica } from '../components/shared/AnalyticalDataTable';
import KpiCard from '../components/shared/KpiCard';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import {
  buscarIntegracoesAuditoria,
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

function buscarMetrica(metricas: IntegracaoMetricaConsolidada[], sistemaDestino: string) {
  return metricas.find((item) => item.sistemaDestino.toUpperCase() === sistemaDestino);
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

const colunas: ColunaTabelaAnalitica<IntegracaoPendencia>[] = [
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
];

export default function IntegracoesPage() {
  const filtrosTabela = useAnalyticalTableFilters();
  const paginacaoTabela = useTabelaPaginadaState(filtrosTabela.resetKey);

  const integracoes = useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: [...QUERY_KEY, paginacaoTabela.pagina, paginacaoTabela.tamanhoPagina, filtrosTabela.apiFilters],
    queryFn: () => buscarIntegracoesAuditoria(
      paginacaoTabela.pagina,
      paginacaoTabela.tamanhoPagina,
      filtrosTabela.apiFilters,
    ),
    placeholderData: (previousData) => previousData,
    staleTime: 60 * 1000,
    retry: 1,
  });

  usePageHeader({
    title: 'Integrações',
    description: 'Auditoria operacional de XML e comprovantes enviados para clientes.',
    updatedAt: integracoes.data?.geradoEm ?? null,
  });

  const metricas = integracoes.data?.metricasConsolidadas ?? EMPTY_METRICAS;
  const vedacit = buscarMetrica(metricas, 'VEDACIT');
  const ppg = buscarMetrica(metricas, 'PPG');
  const pendencias = integracoes.data?.pendencias.itens ?? EMPTY_PENDENCIAS;

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
        <KpiCard
          label="VEDACIT XML"
          valor={formatarPercentual(vedacit?.percentualXmlSucesso)}
          tone={tomPercentual(vedacit?.percentualXmlSucesso)}
          metaLabel="Registros"
          metaValue={formatarInteiro(vedacit?.totalRegistros)}
          progressPct={vedacit?.percentualXmlSucesso ?? null}
        />
        <KpiCard
          label="VEDACIT Comprovante"
          valor={formatarPercentual(vedacit?.percentualCanhotoSucesso)}
          tone={tomPercentual(vedacit?.percentualCanhotoSucesso)}
          metaLabel="Registros"
          metaValue={formatarInteiro(vedacit?.totalRegistros)}
          progressPct={vedacit?.percentualCanhotoSucesso ?? null}
        />
        <KpiCard
          label="PPG Comprovante"
          valor={formatarPercentual(ppg?.percentualCanhotoSucesso)}
          tone={tomPercentual(ppg?.percentualCanhotoSucesso)}
          metaLabel="Registros"
          metaValue={formatarInteiro(ppg?.totalRegistros)}
          progressPct={ppg?.percentualCanhotoSucesso ?? null}
        />
      </div>

      <AnalyticalDataTable
        titulo="Pendências operacionais"
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
        errorFallbackMessage="Erro ao carregar pendências operacionais."
        totalRegistros={integracoes.data?.pendencias.paginacao.totalElementos}
        paginaAtual={paginacaoTabela.pagina}
        tamanhoPagina={paginacaoTabela.tamanhoPagina}
        onPaginaChange={paginacaoTabela.setPagina}
        onTamanhoPaginaChange={paginacaoTabela.setTamanhoPagina}
      />
    </div>
  );
}
