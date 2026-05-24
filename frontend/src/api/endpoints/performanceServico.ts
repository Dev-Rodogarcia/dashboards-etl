import clienteAxios from '../clienteAxios';
import { buscarTabelaPaginada } from '../tabelaPaginada';
import { montarQueryParams } from './queryParams';
import type { PaginacaoResponse } from '../../types/common';
import type {
  PerformanceAgingPoint,
  PerformanceDrilldownParams,
  PerformanceDrilldownPoint,
  PerformanceEntregaRow,
  PerformanceFiltro,
  PerformanceHistoricoPoint,
  PerformanceOverview,
  PerformanceSerieTemporalPoint,
  PerformanceStatusDistribuicao,
  PerformanceTempoNivel,
} from '../../types/performance';

export const PERFORMANCE_API_BASE_PATH = '/api/painel/performance';

function paramsComFiltro(filtro: PerformanceFiltro): URLSearchParams {
  return montarQueryParams(filtro);
}

function paramsComDrilldown(
  filtro: PerformanceFiltro,
  drilldown: PerformanceDrilldownParams,
): URLSearchParams {
  const params = paramsComFiltro(filtro);
  params.set('nivel', drilldown.nivel);

  if (drilldown.responsavel?.trim()) {
    params.set('responsavel', drilldown.responsavel.trim());
  }

  if (drilldown.regiaoDestino?.trim()) {
    params.set('regiaoDestino', drilldown.regiaoDestino.trim());
  }

  return params;
}

export function createPerformanceServico(basePath = PERFORMANCE_API_BASE_PATH) {
  return {
    async buscarOverview(filtro: PerformanceFiltro): Promise<PerformanceOverview> {
      const { data } = await clienteAxios.get<PerformanceOverview>(`${basePath}/overview`, {
        params: paramsComFiltro(filtro),
      });
      return data;
    },

    async buscarSerieTemporal(
      filtro: PerformanceFiltro,
      nivel: PerformanceTempoNivel,
      ano?: number | null,
      mes?: number | null,
    ): Promise<PerformanceSerieTemporalPoint[]> {
      const params = paramsComFiltro(filtro);
      params.set('nivel', nivel);
      if (ano) {
        params.set('ano', String(ano));
      }
      if (mes) {
        params.set('mes', String(mes));
      }

      const { data } = await clienteAxios.get<PerformanceSerieTemporalPoint[]>(`${basePath}/serie-temporal`, {
        params,
      });
      return data;
    },

    async buscarStatus(filtro: PerformanceFiltro): Promise<PerformanceStatusDistribuicao[]> {
      const { data } = await clienteAxios.get<PerformanceStatusDistribuicao[]>(`${basePath}/status`, {
        params: paramsComFiltro(filtro),
      });
      return data;
    },

    async buscarHistorico(filtro: PerformanceFiltro): Promise<PerformanceHistoricoPoint[]> {
      const { data } = await clienteAxios.get<PerformanceHistoricoPoint[]>(`${basePath}/historico`, {
        params: paramsComFiltro(filtro),
      });
      return data;
    },

    async buscarDrilldown(
      filtro: PerformanceFiltro,
      drilldown: PerformanceDrilldownParams,
    ): Promise<PerformanceDrilldownPoint[]> {
      const { data } = await clienteAxios.get<PerformanceDrilldownPoint[]>(`${basePath}/drilldown`, {
        params: paramsComDrilldown(filtro, drilldown),
      });
      return data;
    },

    async buscarAging(filtro: PerformanceFiltro): Promise<PerformanceAgingPoint[]> {
      const { data } = await clienteAxios.get<PerformanceAgingPoint[]>(`${basePath}/aging`, {
        params: paramsComFiltro(filtro),
      });
      return data;
    },

    async buscarTabelaPaginada(
      filtro: PerformanceFiltro,
      pagina: number,
      tamanhoPagina: number,
    ): Promise<PaginacaoResponse<PerformanceEntregaRow>> {
      return buscarTabelaPaginada<PerformanceEntregaRow, PerformanceFiltro>(
        `${basePath}/tabela/paginada`,
        filtro,
        pagina,
        tamanhoPagina,
      );
    },
  };
}

export const performanceServico = createPerformanceServico();

export const buscarPerformanceOverview = performanceServico.buscarOverview;
export const buscarPerformanceSerieTemporal = performanceServico.buscarSerieTemporal;
export const buscarPerformanceStatus = performanceServico.buscarStatus;
export const buscarPerformanceHistorico = performanceServico.buscarHistorico;
export const buscarPerformanceDrilldown = performanceServico.buscarDrilldown;
export const buscarPerformanceAging = performanceServico.buscarAging;
export const buscarPerformanceTabelaPaginada = performanceServico.buscarTabelaPaginada;
