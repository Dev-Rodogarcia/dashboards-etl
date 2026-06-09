import {
  isValidElement,
  useId,
  useState,
  type CSSProperties,
  type FocusEvent,
  type ReactNode,
} from 'react';
import type { KpiDefinition } from '../../constants/kpiDictionary';
import { Popover, PopoverAnchor, PopoverContent } from '../ui/popover';
import { getKpiFlexBasis } from './kpiCardLayout';

interface TooltipKpiProps {
  definition: KpiDefinition;
  children: ReactNode;
  className?: string;
  style?: CSSProperties;
}

export default function TooltipKpi({
  definition,
  children,
  className = '',
  style,
}: TooltipKpiProps) {
  const [isHovered, setIsHovered] = useState(false);
  const [isFocused, setIsFocused] = useState(false);
  const tooltipId = useId();
  const open = isHovered || isFocused;
  const cardValue = isValidElement<{ valor?: unknown }>(children)
    ? children.props.valor
    : undefined;
  const flexBasis = typeof cardValue === 'string'
    ? getKpiFlexBasis(cardValue)
    : 160;

  const handleBlur = (event: FocusEvent<HTMLDivElement>) => {
    if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
      setIsFocused(false);
    }
  };

  return (
    <Popover
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen) {
          setIsHovered(false);
          setIsFocused(false);
        }
      }}
    >
      <PopoverAnchor asChild>
        <div
          aria-describedby={open ? tooltipId : undefined}
          className={`flex min-w-0 rounded-[20px] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--color-primary)] ${className}`}
          onBlur={handleBlur}
          onFocus={() => setIsFocused(true)}
          onKeyDown={(event) => {
            if (event.key === 'Escape') {
              setIsHovered(false);
              setIsFocused(false);
            }
          }}
          onMouseEnter={() => setIsHovered(true)}
          onMouseLeave={() => setIsHovered(false)}
          style={{
            flexGrow: 1,
            flexShrink: 1,
            flexBasis: `${flexBasis}px`,
            ...style,
          }}
          tabIndex={0}
        >
          {children}
        </div>
      </PopoverAnchor>

      <PopoverContent
        id={tooltipId}
        role="tooltip"
        side="top"
        align="center"
        sideOffset={8}
        collisionPadding={12}
        onOpenAutoFocus={(event) => event.preventDefault()}
        className="pointer-events-none space-y-4 p-4"
        style={{
          width: 'min(24rem, calc(100vw - 1.5rem))',
          color: 'var(--color-text)',
        }}
      >
        <div>
          <p className="text-base font-bold leading-snug">{definition.titulo}</p>
          <p
            className="mt-1.5 text-sm leading-relaxed"
            style={{ color: 'var(--color-text-subtle)' }}
          >
            {definition.descricao}
          </p>
        </div>

        <div>
          <p
            className="text-xs font-bold uppercase tracking-wider"
            style={{ color: 'var(--color-primary)' }}
          >
            Cálculo
          </p>
          <p className="mt-1.5 text-sm font-semibold leading-relaxed">
            {definition.calculo}
          </p>
        </div>

        {definition.observacao && (
          <div
            className="rounded-xl border px-3 py-2.5"
            style={{
              borderColor: 'var(--color-border)',
              backgroundColor: 'var(--color-bg)',
            }}
          >
            <p
              className="text-xs font-bold uppercase tracking-wider"
              style={{ color: 'var(--color-text-subtle)' }}
            >
              Observação
            </p>
            <p
              className="mt-1.5 text-[13px] leading-relaxed"
              style={{ color: 'var(--color-text-subtle)' }}
            >
              {definition.observacao}
            </p>
          </div>
        )}
      </PopoverContent>
    </Popover>
  );
}
