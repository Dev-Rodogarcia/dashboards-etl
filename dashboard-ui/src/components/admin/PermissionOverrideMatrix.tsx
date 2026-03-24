import type {
  PermissionCatalogItem,
  PermissionOverrideMode,
  PermissionOverrideStateMap,
} from '../../types/access';

interface PermissionOverrideMatrixProps {
  catalogo: PermissionCatalogItem[];
  valor: PermissionOverrideStateMap;
  onChange: (proximo: PermissionOverrideStateMap) => void;
  disabled?: boolean;
}

const OPCOES: Array<{ valor: PermissionOverrideMode; label: string }> = [
  { valor: 'inherit', label: 'Herdar' },
  { valor: 'grant', label: 'Liberar' },
  { valor: 'deny', label: 'Negar' },
];

export default function PermissionOverrideMatrix({
  catalogo,
  valor,
  onChange,
  disabled,
}: PermissionOverrideMatrixProps) {
  return (
    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
      {catalogo.map((item) => (
        <div
          key={item.chave}
          className={`rounded-2xl border px-4 py-3 ${
            valor[item.chave] === 'grant'
              ? 'border-emerald-300 bg-emerald-50'
              : valor[item.chave] === 'deny'
                ? 'border-red-300 bg-red-50'
                : 'border-gray-200 bg-white'
          } ${disabled ? 'opacity-60' : ''}`}
        >
          <div>
            <div className="text-sm font-semibold text-gray-900">{item.nome}</div>
            <div className="text-xs text-gray-500">{item.descricao}</div>
            {item.rota && <div className="mt-1 text-xs text-gray-400">{item.rota}</div>}
          </div>

          <div className="mt-3 flex flex-wrap gap-2">
            {OPCOES.map((opcao) => {
              const ativo = valor[item.chave] === opcao.valor;
              return (
                <button
                  key={opcao.valor}
                  type="button"
                  disabled={disabled}
                  onClick={() =>
                    onChange({
                      ...valor,
                      [item.chave]: opcao.valor,
                    })
                  }
                  className={`rounded-full px-3 py-1.5 text-xs font-medium transition-colors ${
                    ativo
                      ? 'bg-[#21478A] text-white'
                      : 'border border-gray-300 bg-white text-gray-700'
                  } disabled:cursor-not-allowed`}
                >
                  {opcao.label}
                </button>
              );
            })}
          </div>
        </div>
      ))}
    </div>
  );
}
