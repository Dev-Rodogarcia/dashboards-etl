import clienteAxios from '../clienteAxios';
import { montarQueryParams } from './queryParams';
import type {
  FaturaPorClienteResumoRow,
  FaturasPorClienteAgingBucket,
  FaturasPorClienteFiltro,
  FaturasPorClienteMensalTrend,
  FaturasPorClienteOverview,
  FaturasPorClienteStatusProcesso,
  FaturasPorClienteTopCliente,
} from '../../types/faturasPorCliente';

export async function buscarFaturasPorClienteOverview(
  filtro: FaturasPorClienteFiltro
): Promise<FaturasPorClienteOverview> {
  const { data } = await clienteAxios.get<FaturasPorClienteOverview>('/api/painel/faturas-por-cliente', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFaturasPorClienteMensal(
  filtro: FaturasPorClienteFiltro
): Promise<FaturasPorClienteMensalTrend[]> {
  const { data } = await clienteAxios.get<FaturasPorClienteMensalTrend[]>('/api/painel/faturas-por-cliente/mensal', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFaturasPorClienteAging(
  filtro: FaturasPorClienteFiltro
): Promise<FaturasPorClienteAgingBucket[]> {
  const { data } = await clienteAxios.get<FaturasPorClienteAgingBucket[]>('/api/painel/faturas-por-cliente/aging', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFaturasPorClienteTopClientes(
  filtro: FaturasPorClienteFiltro,
  limite = 10
): Promise<FaturasPorClienteTopCliente[]> {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  const { data } = await clienteAxios.get<FaturasPorClienteTopCliente[]>('/api/painel/faturas-por-cliente/top-clientes', {
    params,
  });
  return data;
}

export async function buscarFaturasPorClienteStatusProcesso(
  filtro: FaturasPorClienteFiltro
): Promise<FaturasPorClienteStatusProcesso[]> {
  const { data } = await clienteAxios.get<FaturasPorClienteStatusProcesso[]>('/api/painel/faturas-por-cliente/status-processo', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFaturasPorClienteTabela(
  filtro: FaturasPorClienteFiltro,
  limite = 100
): Promise<FaturaPorClienteResumoRow[]> {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  const { data } = await clienteAxios.get<FaturaPorClienteResumoRow[]>('/api/painel/faturas-por-cliente/tabela', {
    params,
  });
  return data;
}
