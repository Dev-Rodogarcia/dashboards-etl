import React from 'react';
import type { ReactNode } from 'react';

interface KpiGridProps {
  children: ReactNode;
  count?: number;
}

export default function KpiGrid({ children, count = 4 }: KpiGridProps) {
  // count === 5: two-row layout (3 on top, 2 on bottom with last card featured)
  if (count === 5) {
    const cards = React.Children.toArray(children);
    return (
      <div className="grid grid-cols-6 gap-3 mb-4">
        {cards.map((card, i) => (
          <div key={i} className={i < 3 ? 'col-span-2' : i === 3 ? 'col-span-2' : 'col-span-4'}>
            {card}
          </div>
        ))}
      </div>
    );
  }

  // count >= 6: flex-wrap, content-aware widths via KpiCard flex-basis
  if (count >= 6) {
    return (
      <div className="flex flex-wrap gap-3 mb-4">
        {children}
      </div>
    );
  }

  // count <= 4: single-row CSS grid, equal-width columns
  return (
    <div
      className="grid gap-3 mb-4"
      style={{ gridTemplateColumns: `repeat(${count}, minmax(0, 1fr))` }}
    >
      {children}
    </div>
  );
}
