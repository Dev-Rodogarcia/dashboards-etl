interface MensagemErroProps {
  mensagem: string;
}

export default function MensagemErro({ mensagem }: MensagemErroProps) {
  return (
    <div className="border border-red-400 bg-red-50 rounded-lg p-4">
      <p className="text-red-700 text-sm">{mensagem}</p>
    </div>
  );
}
