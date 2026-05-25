import clienteAxios from '../clienteAxios';
import { montarQueryParams } from './queryParams';
import type { FaturamentoFiltro } from '../../types/faturamento';
import type { FretesFiltro } from '../../types/fretes';

export interface VeiculoDim {
  placa: string;
  tipoVeiculo: string;
  proprietario: string;
}

export interface PlanoContasDim {
  descricao: string;
  classificacao: string;
}

export interface UsuarioDim {
  userId: string;
  nome: string;
}

export interface PagadorDim {
  nome: string;
  documento: string | null;
}

export async function buscarFiliais(): Promise<string[]> {
  const { data } = await clienteAxios.get<string[]>('/api/dimensoes/filiais');
  return data;
}

export async function buscarClientes(): Promise<string[]> {
  const { data } = await clienteAxios.get<string[]>('/api/dimensoes/clientes');
  return data;
}

export async function buscarPagadores(busca?: string): Promise<PagadorDim[]> {
  const params = new URLSearchParams();
  const termo = busca?.trim();
  if (termo) {
    params.set('busca', termo);
  }
  params.set('limite', '50');

  const { data } = await clienteAxios.get<PagadorDim[]>('/api/dimensoes/pagadores', { params });
  return data;
}

export async function buscarFaturasPorClienteClientesCnpj(): Promise<string[]> {
  const { data } = await clienteAxios.get<string[]>('/api/dimensoes/faturas-por-cliente/clientes-cnpj');
  return data;
}

export async function buscarMotoristas(): Promise<string[]> {
  const { data } = await clienteAxios.get<string[]>('/api/dimensoes/motoristas');
  return data;
}

export async function buscarVeiculos(): Promise<VeiculoDim[]> {
  const { data } = await clienteAxios.get<VeiculoDim[]>('/api/dimensoes/veiculos');
  return data;
}

export async function buscarPlanoContas(): Promise<PlanoContasDim[]> {
  const { data } = await clienteAxios.get<PlanoContasDim[]>('/api/dimensoes/planocontas');
  return data;
}

export async function buscarUsuarios(): Promise<UsuarioDim[]> {
  const { data } = await clienteAxios.get<UsuarioDim[]>('/api/dimensoes/usuarios');
  return data;
}

export async function buscarFretesStatus(filtro: FretesFiltro): Promise<string[]> {
  const { data } = await clienteAxios.get<string[]>('/api/dimensoes/fretes/status', {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarFaturamentoStatus(filtro: FaturamentoFiltro): Promise<string[]> {
  return buscarFretesStatus(filtro);
}
