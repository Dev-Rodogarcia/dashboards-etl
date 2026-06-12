package com.dashboard.api.repository;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.manifestos.ManifestosCustosEvolucaoDTO.CustoDiarioDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ManifestosCostDataRepository {

    List<CustoDiarioDTO> buscarCustosDiarios(FiltroConsultaDTO filtro);

    BigDecimal buscarCustoTotal(FiltroConsultaDTO filtro);

    LocalDate buscarUltimoDiaUtilFechado(LocalDate dataReferencia);

    Integer contarDiasUteisCalendario(LocalDate dataInicio, LocalDate dataFim);
}
