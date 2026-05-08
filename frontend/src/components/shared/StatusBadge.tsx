import { CORES_STATUS } from '../../utils/chartColors';

interface StatusBadgeProps {
  status: string;
}

const TEXT_COLORS_BY_HEX: Record<string, string> = {
  '#f59e0b': '#a16207',
  '#21478A': '#21478A',
  '#10b981': '#047857',
  '#ef4444': '#b91c1c',
  '#06b6d4': '#0e7490',
  '#7c3aed': '#6d28d9',
  '#f97316': '#c2410c',
  '#3b82f6': '#1d4ed8',
  '#8b5cf6': '#7c3aed',
  '#6366f1': '#4f46e5',
  '#d1d5db': '#4b5563',
  '#6b7280': '#4b5563',
};

function hexToRgba(hex: string, alpha: number) {
  const normalized = hex.replace('#', '');
  if (normalized.length !== 6) {
    return 'rgba(107, 114, 128, 0.10)';
  }

  const red = Number.parseInt(normalized.slice(0, 2), 16);
  const green = Number.parseInt(normalized.slice(2, 4), 16);
  const blue = Number.parseInt(normalized.slice(4, 6), 16);

  return `rgba(${red}, ${green}, ${blue}, ${alpha})`;
}

export default function StatusBadge({ status }: StatusBadgeProps) {
  const statusNormalizado = status.toLowerCase();
  const cor = CORES_STATUS[statusNormalizado] ?? '#6b7280';
  const textColor = TEXT_COLORS_BY_HEX[cor] ?? cor;

  return (
    <span
      className="inline-flex rounded-full px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide"
      style={{ backgroundColor: hexToRgba(cor, 0.18), color: textColor }}
    >
      {status}
    </span>
  );
}
