import clienteAxios from '../clienteAxios';
import { baixarExcel } from '../downloadExcel';
import { buscarTabelaPaginada } from '../tabelaPaginada';
import { montarQueryParams } from './queryParams';
import type { PaginacaoResponse } from '../../types/common';
import type {
  CubagemMercadoriasOverview,
  CubagemMercadoriasRow,
  CubagemMercadoriasSeriePoint,
  HorarioCorteRow,
  HorariosCorteImportacaoResultado,
  HorariosCorteOverview,
  HorariosCorteSeriePoint,
  IndenizacaoMercadoriasOverview,
  IndenizacaoMercadoriasRow,
  IndenizacaoMercadoriasSeriePoint,
  IndicadoresGestaoVistaFiltro,
  PerformanceEntregaOverview,
  PerformanceEntregaRow,
  PerformanceEntregaSeriePoint,
  UtilizacaoColetoresOverview,
  UtilizacaoColetoresRow,
  UtilizacaoColetoresSeriePoint,
} from '../../types/indicadoresGestaoAVista';

const BASE = '/api/painel/indicadores-gestao-a-vista';

function withLimit(filtro: IndicadoresGestaoVistaFiltro, limite: number) {
  const params = montarQueryParams(filtro);
  params.set('limite', String(limite));
  return params;
}

export async function buscarPerformanceEntregaOverview(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<PerformanceEntregaOverview> {
  const { data } = await clienteAxios.get<PerformanceEntregaOverview>(`${BASE}/performance-entrega/overview`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarPerformanceEntregaSerie(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<PerformanceEntregaSeriePoint[]> {
  const { data } = await clienteAxios.get<PerformanceEntregaSeriePoint[]>(`${BASE}/performance-entrega/serie`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarPerformanceEntregaTabela(
  filtro: IndicadoresGestaoVistaFiltro,
  limite = 100,
): Promise<PerformanceEntregaRow[]> {
  const { data } = await clienteAxios.get<PerformanceEntregaRow[]>(`${BASE}/performance-entrega/tabela`, {
    params: withLimit(filtro, limite),
  });
  return data;
}

export async function buscarPerformanceEntregaTabelaPaginada(
  filtro: IndicadoresGestaoVistaFiltro,
  pagina: number,
  tamanhoPagina: number,
): Promise<PaginacaoResponse<PerformanceEntregaRow>> {
  return buscarTabelaPaginada(`${BASE}/performance-entrega/tabela/paginada`, filtro, pagina, tamanhoPagina);
}

export async function exportarPerformanceEntregaExcel(filtro: IndicadoresGestaoVistaFiltro): Promise<void> {
  await baixarExcel(`${BASE}/performance-entrega/exportacao`, filtro, 'indicadores-performance-entrega');
}

export async function buscarUtilizacaoColetoresOverview(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<UtilizacaoColetoresOverview> {
  const { data } = await clienteAxios.get<UtilizacaoColetoresOverview>(`${BASE}/utilizacao-coletores/overview`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarUtilizacaoColetoresSerie(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<UtilizacaoColetoresSeriePoint[]> {
  const { data } = await clienteAxios.get<UtilizacaoColetoresSeriePoint[]>(`${BASE}/utilizacao-coletores/serie`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarUtilizacaoColetoresTabela(
  filtro: IndicadoresGestaoVistaFiltro,
  limite = 100,
): Promise<UtilizacaoColetoresRow[]> {
  const { data } = await clienteAxios.get<UtilizacaoColetoresRow[]>(`${BASE}/utilizacao-coletores/tabela`, {
    params: withLimit(filtro, limite),
  });
  return data;
}

export async function buscarUtilizacaoColetoresTabelaPaginada(
  filtro: IndicadoresGestaoVistaFiltro,
  pagina: number,
  tamanhoPagina: number,
): Promise<PaginacaoResponse<UtilizacaoColetoresRow>> {
  return buscarTabelaPaginada(`${BASE}/utilizacao-coletores/tabela/paginada`, filtro, pagina, tamanhoPagina);
}

export async function exportarUtilizacaoColetoresExcel(filtro: IndicadoresGestaoVistaFiltro): Promise<void> {
  await baixarExcel(`${BASE}/utilizacao-coletores/exportacao`, filtro, 'indicadores-utilizacao-coletores');
}

export async function buscarCubagemMercadoriasOverview(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<CubagemMercadoriasOverview> {
  const { data } = await clienteAxios.get<CubagemMercadoriasOverview>(`${BASE}/cubagem-mercadorias/overview`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarCubagemMercadoriasSerie(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<CubagemMercadoriasSeriePoint[]> {
  const { data } = await clienteAxios.get<CubagemMercadoriasSeriePoint[]>(`${BASE}/cubagem-mercadorias/serie`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarCubagemMercadoriasTabela(
  filtro: IndicadoresGestaoVistaFiltro,
  limite = 100,
): Promise<CubagemMercadoriasRow[]> {
  const { data } = await clienteAxios.get<CubagemMercadoriasRow[]>(`${BASE}/cubagem-mercadorias/tabela`, {
    params: withLimit(filtro, limite),
  });
  return data;
}

export async function buscarCubagemMercadoriasTabelaPaginada(
  filtro: IndicadoresGestaoVistaFiltro,
  pagina: number,
  tamanhoPagina: number,
): Promise<PaginacaoResponse<CubagemMercadoriasRow>> {
  return buscarTabelaPaginada(`${BASE}/cubagem-mercadorias/tabela/paginada`, filtro, pagina, tamanhoPagina);
}

export async function exportarCubagemMercadoriasExcel(filtro: IndicadoresGestaoVistaFiltro): Promise<void> {
  await baixarExcel(`${BASE}/cubagem-mercadorias/exportacao`, filtro, 'indicadores-cubagem-mercadorias');
}

export async function buscarIndenizacaoMercadoriasOverview(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<IndenizacaoMercadoriasOverview> {
  const { data } = await clienteAxios.get<IndenizacaoMercadoriasOverview>(`${BASE}/indenizacao-mercadorias/overview`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarIndenizacaoMercadoriasSerie(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<IndenizacaoMercadoriasSeriePoint[]> {
  const { data } = await clienteAxios.get<IndenizacaoMercadoriasSeriePoint[]>(`${BASE}/indenizacao-mercadorias/serie`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarIndenizacaoMercadoriasTabela(
  filtro: IndicadoresGestaoVistaFiltro,
  limite = 100,
): Promise<IndenizacaoMercadoriasRow[]> {
  const { data } = await clienteAxios.get<IndenizacaoMercadoriasRow[]>(`${BASE}/indenizacao-mercadorias/tabela`, {
    params: withLimit(filtro, limite),
  });
  return data;
}

export async function buscarIndenizacaoMercadoriasTabelaPaginada(
  filtro: IndicadoresGestaoVistaFiltro,
  pagina: number,
  tamanhoPagina: number,
): Promise<PaginacaoResponse<IndenizacaoMercadoriasRow>> {
  return buscarTabelaPaginada(`${BASE}/indenizacao-mercadorias/tabela/paginada`, filtro, pagina, tamanhoPagina);
}

export async function exportarIndenizacaoMercadoriasExcel(filtro: IndicadoresGestaoVistaFiltro): Promise<void> {
  await baixarExcel(`${BASE}/indenizacao-mercadorias/exportacao`, filtro, 'indicadores-indenizacao-mercadorias');
}

export async function buscarHorariosCorteOverview(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<HorariosCorteOverview> {
  const { data } = await clienteAxios.get<HorariosCorteOverview>(`${BASE}/horarios-corte/overview`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarHorariosCorteSerie(
  filtro: IndicadoresGestaoVistaFiltro,
): Promise<HorariosCorteSeriePoint[]> {
  const { data } = await clienteAxios.get<HorariosCorteSeriePoint[]>(`${BASE}/horarios-corte/serie`, {
    params: montarQueryParams(filtro),
  });
  return data;
}

export async function buscarHorariosCorteTabela(
  filtro: IndicadoresGestaoVistaFiltro,
  limite = 100,
): Promise<HorarioCorteRow[]> {
  const { data } = await clienteAxios.get<HorarioCorteRow[]>(`${BASE}/horarios-corte/tabela`, {
    params: withLimit(filtro, limite),
  });
  return data;
}

export async function buscarHorariosCorteTabelaPaginada(
  filtro: IndicadoresGestaoVistaFiltro,
  pagina: number,
  tamanhoPagina: number,
): Promise<PaginacaoResponse<HorarioCorteRow>> {
  return buscarTabelaPaginada(`${BASE}/horarios-corte/tabela/paginada`, filtro, pagina, tamanhoPagina);
}

export async function exportarHorariosCorteExcel(filtro: IndicadoresGestaoVistaFiltro): Promise<void> {
  await baixarExcel(`${BASE}/horarios-corte/exportacao`, filtro, 'indicadores-horarios-corte');
}

export async function importarHorariosCorte(
  arquivo: File,
): Promise<HorariosCorteImportacaoResultado> {
  const formData = new FormData();
  formData.append('arquivo', arquivo);

  const { data } = await clienteAxios.post<HorariosCorteImportacaoResultado>(
    `${BASE}/horarios-corte/importacao`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    },
  );
  return data;
}
