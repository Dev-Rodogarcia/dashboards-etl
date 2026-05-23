import clienteAxios from '../clienteAxios';
import { baixarCsv } from '../downloadCsv';
import { aplicarFiltrosTabelaParams } from '../tableFilters';
import { buscarTabelaPaginada } from '../tabelaPaginada';
import { montarQueryParams } from './queryParams';
import type { PaginacaoResponse } from '../../types/common';
import type { TrackingCharts, TrackingDashboard, TrackingFiltro, TrackingOverview, TrackingRawRow, TrackingTimelinePoint } from '../../types/tracking';
import type { TableApiFilters } from '../../types/tableFilters';

type TrackingApiRow = TrackingRawRow & { pesoTaxado?: number; valorNf?: number };

function normalizarTrackingRow(item: TrackingApiRow): TrackingRawRow {
  return {
    numeroMinuta: item.numeroMinuta,
    dataFrete: item.dataFrete,
    tipo: item.tipo,
    volumes: item.volumes,
    pesoTaxadoRaw: item.pesoTaxadoRaw ?? String(item.pesoTaxado ?? ''),
    valorNfRaw: item.valorNfRaw ?? String(item.valorNf ?? ''),
    valorFrete: item.valorFrete,
    filialEmissora: item.filialEmissora,
    filialOrigem: item.filialOrigem,
    filialAtual: item.filialAtual,
    filialDestino: item.filialDestino,
    regiaoOrigem: item.regiaoOrigem,
    regiaoDestino: item.regiaoDestino,
    classificacao: item.classificacao,
    statusCarga: item.statusCarga,
    previsaoEntrega: item.previsaoEntrega,
  };
}

export async function buscarTrackingOverview(filtro: TrackingFiltro): Promise<TrackingOverview> {
  const { data } = await clienteAxios.get<TrackingOverview>('/api/painel/tracking', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarTrackingDashboard(filtro: TrackingFiltro): Promise<TrackingDashboard> {
  const { data } = await clienteAxios.get<TrackingDashboard>('/api/painel/tracking/dashboard', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarTrackingSerie(filtro: TrackingFiltro): Promise<TrackingTimelinePoint[]> {
  const { data } = await clienteAxios.get<TrackingTimelinePoint[]>('/api/painel/tracking/serie', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarTrackingGraficos(filtro: TrackingFiltro): Promise<TrackingCharts> {
  const { data } = await clienteAxios.get<TrackingCharts>('/api/painel/tracking/graficos', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarTrackingTabela(
  filtro: TrackingFiltro,
  limite = 100
): Promise<TrackingRawRow[]> {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  const { data } = await clienteAxios.get<TrackingApiRow[]>('/api/painel/tracking/tabela', { params });
  return data.map(normalizarTrackingRow);
}

export async function buscarTrackingTabelaTotal(filtro: TrackingFiltro): Promise<number> {
  const { data } = await clienteAxios.get<{ total: number }>('/api/painel/tracking/tabela/total', {
    params: montarQueryParams(filtro),
  });
  return data.total;
}

export async function buscarTrackingTabelaPaginada(
  filtro: TrackingFiltro,
  pagina: number,
  tamanhoPagina: number,
  filtrosTabela?: TableApiFilters,
): Promise<PaginacaoResponse<TrackingRawRow>> {
  const resposta = await buscarTabelaPaginada<TrackingApiRow, TrackingFiltro>(
    '/api/painel/tracking/tabela/paginada',
    filtro,
    pagina,
    tamanhoPagina,
    filtrosTabela,
  );
  return {
    ...resposta,
    conteudo: resposta.conteudo.map(normalizarTrackingRow),
  };
}

export async function buscarTrackingDetalhesPaginada(
  filtro: TrackingFiltro,
  page: number,
  size: number,
  filtrosTabela?: TableApiFilters,
): Promise<PaginacaoResponse<TrackingRawRow>> {
  const params = montarQueryParams(filtro);
  aplicarFiltrosTabelaParams(params, filtrosTabela);
  params.set('page', String(page));
  params.set('size', String(size));

  const { data } = await clienteAxios.get<PaginacaoResponse<TrackingApiRow>>('/api/painel/tracking/detalhes', { params });
  return {
    ...data,
    conteudo: data.conteudo.map(normalizarTrackingRow),
  };
}

export async function exportarTrackingCsv(filtro: TrackingFiltro, filtrosTabela?: TableApiFilters): Promise<void> {
  await baixarCsv('/api/painel/tracking/exportacao', filtro, 'localizacao-cargas', filtrosTabela);
}
