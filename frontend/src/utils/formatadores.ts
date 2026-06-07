export function formatarMoeda(valor: number): string {
  return valor.toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 2,
  });
}

export function formatarNumero(valor: number, decimais = 0): string {
  return valor.toLocaleString('pt-BR', {
    minimumFractionDigits: decimais,
    maximumFractionDigits: decimais,
  });
}

export function formatarPorcentagem(valor: number, decimais = 1): string {
  return `${valor.toLocaleString('pt-BR', {
    minimumFractionDigits: decimais,
    maximumFractionDigits: decimais,
  })}%`;
}

export function formatarPeso(valorKg: number): string {
  if (valorKg >= 1000) {
    return `${(valorKg / 1000).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} t`;
  }
  return `${valorKg.toLocaleString('pt-BR', { maximumFractionDigits: 1 })} kg`;
}

const DATA_HORA_MINUTO_FORMATTER = new Intl.DateTimeFormat('pt-BR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

function parseDataHoraLocal(valor: string): Date | null {
  const texto = valor.trim();
  const match = /^(\d{4})-(\d{2})-(\d{2})(?:[T\s]+(\d{2}):(\d{2})(?::(\d{2})(?:\.\d+)?)?)?(?:\s*(?:Z|[+-]\d{2}:?\d{2}))?$/.exec(texto);

  if (match) {
    const [, anoTexto, mesTexto, diaTexto, horaTexto = '0', minutoTexto = '0'] = match;
    const ano = Number(anoTexto);
    const mes = Number(mesTexto);
    const dia = Number(diaTexto);
    const hora = Number(horaTexto);
    const minuto = Number(minutoTexto);
    const data = new Date(ano, mes - 1, dia, hora, minuto);

    if (
      data.getFullYear() === ano
      && data.getMonth() === mes - 1
      && data.getDate() === dia
      && data.getHours() === hora
      && data.getMinutes() === minuto
    ) {
      return data;
    }
  }

  const data = new Date(texto);
  return Number.isNaN(data.getTime()) ? null : data;
}

export function formatarDataHoraMinuto(dataISO: string): string {
  const data = parseDataHoraLocal(dataISO);
  if (!data) {
    return dataISO;
  }

  return DATA_HORA_MINUTO_FORMATTER.format(data).replace(',', '').replace(/\s+/g, ' ').trim();
}

export function formatarData(dataISO: string): string {
  const data = new Date(dataISO);
  return data.toLocaleDateString('pt-BR');
}

export function formatarDataHora(dataISO: string): string {
  const data = new Date(dataISO);
  return data.toLocaleString('pt-BR');
}

export function formatarDataCurta(dataISO: string): string {
  const data = new Date(dataISO);
  return data.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' });
}
