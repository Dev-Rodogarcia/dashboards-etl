import clienteAxios from '../clienteAxios';

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

export async function buscarFiliais(): Promise<string[]> {
  const { data } = await clienteAxios.get<string[]>('/api/dimensoes/filiais');
  return data;
}

export async function buscarClientes(): Promise<string[]> {
  const { data } = await clienteAxios.get<string[]>('/api/dimensoes/clientes');
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
