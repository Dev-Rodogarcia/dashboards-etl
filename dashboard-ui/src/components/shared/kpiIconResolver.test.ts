import {
  AlertCircle,
  BanknoteArrowDown,
  BarChart3,
  CalendarClock,
  Gauge,
  TrendingUp,
  Users,
  Weight,
} from 'lucide-react';
import { describe, expect, it } from 'vitest';
import { resolveKpiIcon } from './kpiIconResolver';

describe('resolveKpiIcon', () => {
  it('mantem mapeamentos especificos dos cards atuais', () => {
    expect(resolveKpiIcon('Peso Taxado')).toBe(Weight);
    expect(resolveKpiIcon('Taxa Sucesso')).toBe(TrendingUp);
    expect(resolveKpiIcon('Valor a Pagar')).toBe(BanknoteArrowDown);
    expect(resolveKpiIcon('SLA Agendamento')).toBe(CalendarClock);
    expect(resolveKpiIcon('Clientes Ativos')).toBe(Users);
  });

  it('resolve heuristicas mesmo com acentos e labels novos', () => {
    expect(resolveKpiIcon('Título em atraso')).toBe(AlertCircle);
    expect(resolveKpiIcon('% de ocupação operacional')).toBe(Gauge);
  });

  it('usa um icone generico quando nao encontra padrao conhecido', () => {
    expect(resolveKpiIcon('Indicador Experimental')).toBe(BarChart3);
  });
});
