import { createElement } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it } from 'vitest';
import IndicadoresGestaoPanoramaSection from './IndicadoresGestaoPanoramaSection';

describe('IndicadoresGestaoPanoramaSection', () => {
  it('renderiza a secao de panorama sem acoes de tabela ou exportacao', () => {
    const html = renderToStaticMarkup(
      createElement(IndicadoresGestaoPanoramaSection, {
        items: [
          {
            id: 'performance',
            title: 'Performance de Entrega',
            value: '8,0%',
            statusLabel: 'Crítico',
            tone: 'negative',
            progressPct: 42.1,
            detail: '137 no prazo / 1.716 entregas · Meta 95%',
            alertDetail: 'Gap 87,0 p.p. · 137 no prazo de 1.716',
            severityScore: 0.9158,
            icon: createElement('span', null, 'P'),
          },
          {
            id: 'horarios',
            title: 'Horários de Corte',
            value: '85,7%',
            statusLabel: 'Em atenção',
            tone: 'warning',
            progressPct: 95.2,
            detail: '6 no horário / 7 saídas · Meta 90%',
            alertDetail: 'Gap 4,3 p.p. · 6 de 7 no horário',
            severityScore: 0.0478,
            icon: createElement('span', null, 'H'),
          },
        ],
      }),
    );

    expect(html).toContain('Panorama Operacional');
    expect(html).toContain('Atenções do Período');
    expect(html).toContain('Performance de Entrega');
    expect(html).toContain('Horários de Corte');
    expect(html).toContain('137 no prazo / 1.716 entregas');
    expect(html).toContain('Gap 87,0 p.p. · 137 no prazo de 1.716');
    expect(html).not.toContain('Mostrar tabela');
    expect(html).not.toContain('Ocultar tabela');
    expect(html).not.toContain('Exportar CSV');
  });

  it('mostra mensagem neutra quando nao ha alertas ativos no panorama', () => {
    const html = renderToStaticMarkup(
      createElement(IndicadoresGestaoPanoramaSection, {
        items: [
          {
            id: 'coletores',
            title: 'Utilização dos Coletores',
            value: '84,0%',
            statusLabel: 'Dentro da meta',
            tone: 'positive',
            progressPct: 100,
            detail: '84 bipados / 100 manifestos · Meta 80%',
            alertDetail: 'Meta atendida · 84 bipados sobre 100 manifestos',
            severityScore: 0,
            icon: createElement('span', null, 'C'),
          },
        ],
      }),
    );

    expect(html).toContain('Nenhum alerta crítico no período.');
  });
});
