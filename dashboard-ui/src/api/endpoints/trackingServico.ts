import clienteAxios from '../clienteAxios';
import { montarQueryParams } from './queryParams';
import type { TrackingCharts, TrackingFiltro, TrackingOverview, TrackingRawRow, TrackingTimelinePoint } from '../../types/tracking';

export async function buscarTrackingOverview(filtro: TrackingFiltro): Promise<TrackingOverview> {
  const { data } = await clienteAxios.get<TrackingOverview>('/api/painel/tracking', {
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
  const { data } = await clienteAxios.get<Array<TrackingRawRow & { pesoTaxado?: number; valorNf?: number }>>('/api/painel/tracking/tabela', { params });
  return data.map((item) => ({
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
  }));
}
