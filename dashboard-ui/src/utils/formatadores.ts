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
