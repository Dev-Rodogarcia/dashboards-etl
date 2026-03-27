/**
 * Utilitários centralizados de data para o sistema de filtros.
 * Todas as funções usam o timezone LOCAL do browser (não UTC),
 * evitando o bug clássico onde toISOString() retorna "amanhã" para
 * usuários no Brasil (UTC-3) após as 21h.
 */

/** Retorna a data de hoje em formato YYYY-MM-DD no fuso local. */
export function dataHojeLocal(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Retorna a data de N dias atrás em formato YYYY-MM-DD no fuso local. */
export function dataNDiasAtrasLocal(dias: number): string {
  const d = new Date();
  d.setDate(d.getDate() - dias);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Retorna a data de 30 dias atrás em formato YYYY-MM-DD no fuso local. */
export function data30DiasAtrasLocal(): string {
  return dataNDiasAtrasLocal(30);
}

/**
 * Adiciona N dias a uma data no formato YYYY-MM-DD.
 * A operação é feita no fuso local para evitar drift de timezone.
 */
export function adicionarDias(data: string, dias: number): string {
  const d = new Date(`${data}T00:00:00`);
  d.setDate(d.getDate() + dias);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/**
 * Normaliza um período de datas:
 * - Se início > fim, troca os dois
 * - Se o período exceder maxDias, trunca dataFim
 * - Datas vazias são retornadas sem alteração
 */
export function normalizarPeriodo(
  dataInicio: string,
  dataFim: string,
  maxDias = 366,
): { dataInicio: string; dataFim: string } {
  if (!dataInicio || !dataFim) return { dataInicio, dataFim };
  if (dataInicio > dataFim) return { dataInicio: dataFim, dataFim: dataInicio };
  const limiteMaximo = adicionarDias(dataInicio, maxDias);
  if (limiteMaximo < dataFim) return { dataInicio, dataFim: limiteMaximo };
  return { dataInicio, dataFim };
}
