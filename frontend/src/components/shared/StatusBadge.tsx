import { CORES_STATUS } from '../../utils/chartColors';

interface StatusBadgeProps {
  status: string;
}

export default function StatusBadge({ status }: StatusBadgeProps) {
  const statusNormalizado = status.toLowerCase();
  const cor = CORES_STATUS[statusNormalizado] ?? '#6b7280';

  return (
    <span
      className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium text-white"
      style={{ backgroundColor: cor }}
    >
      {status}
    </span>
  );
}
