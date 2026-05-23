import {
  dataHojeLocal,
  dataNDiasAtrasLocal,
  primeiroDiaMesAtualLocal,
  primeiroDiaMesPassadoLocal,
  ultimoDiaMesPassadoLocal,
} from '../../utils/dateUtils';

export type DatePreset = {
  label: string;
  getRange: () => { dataInicio: string; dataFim: string };
};

// Atalhos corporativos: semana, quinzena, mês, bimestre, trimestre, semestre e períodos mensais fechados.
export const DATE_RANGE_PRESETS: DatePreset[] = [
  { label: '7d', getRange: () => ({ dataInicio: dataNDiasAtrasLocal(7), dataFim: dataHojeLocal() }) },
  { label: '15d', getRange: () => ({ dataInicio: dataNDiasAtrasLocal(15), dataFim: dataHojeLocal() }) },
  { label: '30d', getRange: () => ({ dataInicio: dataNDiasAtrasLocal(30), dataFim: dataHojeLocal() }) },
  { label: '60d', getRange: () => ({ dataInicio: dataNDiasAtrasLocal(60), dataFim: dataHojeLocal() }) },
  { label: '90d', getRange: () => ({ dataInicio: dataNDiasAtrasLocal(90), dataFim: dataHojeLocal() }) },
  { label: '180d', getRange: () => ({ dataInicio: dataNDiasAtrasLocal(180), dataFim: dataHojeLocal() }) },
  { label: 'Este mês', getRange: () => ({ dataInicio: primeiroDiaMesAtualLocal(), dataFim: dataHojeLocal() }) },
  { label: 'Mês passado', getRange: () => ({ dataInicio: primeiroDiaMesPassadoLocal(), dataFim: ultimoDiaMesPassadoLocal() }) },
];
