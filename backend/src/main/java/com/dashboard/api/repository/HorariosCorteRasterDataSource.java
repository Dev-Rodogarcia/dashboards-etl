package com.dashboard.api.repository;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.HorariosCorteSeriePointDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.model.VisaoHorariosCorteEntity;
import com.dashboard.api.service.acesso.EscopoFilialService;
import java.time.LocalDate;
import java.util.List;

public interface HorariosCorteRasterDataSource {
    HorariosCorteResumo buscarResumoPorPeriodo(
            LocalDate dataInicio,
            LocalDate dataFim,
            EscopoFilialService.EscopoFilial escopo,
            List<String> filiaisFiltro
    );

    List<HorariosCorteSeriePointDTO> buscarSeriePorPeriodo(
            LocalDate dataInicio,
            LocalDate dataFim,
            EscopoFilialService.EscopoFilial escopo,
            List<String> filiaisFiltro
    );

    List<VisaoHorariosCorteEntity> findByDataBetween(
            LocalDate dataInicio,
            LocalDate dataFim,
            EscopoFilialService.EscopoFilial escopo,
            List<String> filiaisFiltro,
            FiltroConsultaDTO filtro
    );

    PaginaDTO<VisaoHorariosCorteEntity> findPageByDataBetween(
            LocalDate dataInicio,
            LocalDate dataFim,
            EscopoFilialService.EscopoFilial escopo,
            List<String> filiaisFiltro,
            FiltroConsultaDTO filtro,
            int pagina,
            int tamanhoPagina
    );

    record HorariosCorteResumo(
            String updatedAt,
            long totalProgramado,
            long saidasNoHorario,
            long saidasForaHorario,
            String ultimaImportacaoEm,
            String ultimaImportacaoArquivo
    ) {
    }
}
