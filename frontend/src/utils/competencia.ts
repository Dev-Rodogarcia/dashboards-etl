const YEAR_MONTH_PATTERN = /^(\d{4})-(\d{2})(?:-\d{2})?$/;

function pad2(value: number): string {
  return String(value).padStart(2, '0');
}

function competenciaAtual(): string {
  const hoje = new Date();
  return `${hoje.getFullYear()}-${pad2(hoje.getMonth() + 1)}-01`;
}

export function normalizarCompetenciaApi(value?: string | null): string {
  const raw = value?.trim();
  if (!raw) {
    return competenciaAtual();
  }

  const match = YEAR_MONTH_PATTERN.exec(raw);
  if (match) {
    const year = Number(match[1]);
    const month = Number(match[2]);
    if (Number.isInteger(year) && month >= 1 && month <= 12) {
      return `${match[1]}-${match[2]}-01`;
    }
  }

  const parsed = new Date(raw);
  if (!Number.isNaN(parsed.getTime())) {
    return `${parsed.getFullYear()}-${pad2(parsed.getMonth() + 1)}-01`;
  }

  return competenciaAtual();
}

export function normalizarCompetenciaApiOpcional(value?: string | null): string | undefined {
  return value?.trim() ? normalizarCompetenciaApi(value) : undefined;
}

export function competenciaParaMonthInput(value?: string | null): string {
  return normalizarCompetenciaApi(value).slice(0, 7);
}

export function formatarCompetenciaMeta(value?: string | null): string {
  const competencia = normalizarCompetenciaApi(value);
  const year = competencia.slice(0, 4);
  const monthIndex = Number(competencia.slice(5, 7)) - 1;
  const meses = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];
  return `Meta ref. ${meses[monthIndex] ?? competencia.slice(5, 7)}/${year}`;
}
