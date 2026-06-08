package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.manifestos.ManifestoResumoDTO;
import com.dashboard.api.dto.manifestos.ManifestosChartsDTO;
import com.dashboard.api.dto.manifestos.ManifestosComposicaoCustoDTO;
import com.dashboard.api.dto.manifestos.ManifestosCustoPorFilialDTO;
import com.dashboard.api.dto.manifestos.ManifestosOcupacaoScatterDTO;
import com.dashboard.api.dto.manifestos.ManifestosOverviewDTO;
import com.dashboard.api.dto.manifestos.ManifestosRankingMotoristaDTO;
import com.dashboard.api.dto.manifestos.ManifestosTrendPointDTO;
import com.dashboard.api.filter.DashboardQueryFilters;
import com.dashboard.api.repository.VisaoManifestosRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.ConsultaFiltroUtils;
import com.dashboard.api.util.ConsultaLimiteUtils;
import com.dashboard.api.util.TemporalJsonUtils;
import com.dashboard.api.util.PeriodoOffsetDateTimeHelper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ManifestosService {

    private static final Logger log = LoggerFactory.getLogger(ManifestosService.class);
    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoManifestosRepository repository;
    private final EscopoFilialService escopoFilialService;

    ManifestosService(ValidadorPeriodoService validadorPeriodo, VisaoManifestosRepository repository) {
        this(validadorPeriodo, repository, escopoSemRestricao(), PeriodoOffsetDateTimeHelper.padrao());
    }

    @Autowired
    public ManifestosService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoManifestosRepository repository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.repository = repository;
        this.escopoFilialService = escopoFilialService;
    }

    public ManifestosOverviewDTO buscarOverview(LocalDate dataInicio, LocalDate dataFim) {
        return buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public ManifestosOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        VisaoManifestosRepository.ManifestosOverviewProjection overview = buscarOverviewAgregado(consulta(filtro));
        int totalManifestos = overview != null ? overview.getTotalManifestos() : 0;

        if (totalManifestos == 0) {
            return new ManifestosOverviewDTO(
                    TemporalJsonUtils.formatarUtc(null),
                    0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, 0.0
            );
        }

        BigDecimal kmTotal = zero(overview.getKmTotal()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal custoTotal = zero(overview.getCustoTotal()).setScale(2, RoundingMode.HALF_UP);

        BigDecimal custoPorKm = kmTotal.compareTo(BigDecimal.ZERO) > 0
                ? custoTotal.divide(kmTotal, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        log.info("Overview manifestos calculado: total={}, periodo={} a {}", totalManifestos, filtro.dataInicio(), filtro.dataFim());

        return new ManifestosOverviewDTO(
                formatarAtualizacao(overview.getUpdatedAt()),
                totalManifestos,
                overview.getEmTransito(),
                overview.getEncerrados(),
                kmTotal,
                custoTotal,
                custoPorKm,
                zero(overview.getOcupacaoPesoMediaPct()).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                zero(overview.getOcupacaoCubagemMediaPct()).setScale(2, RoundingMode.HALF_UP).doubleValue()
        );
    }

    public List<ManifestosTrendPointDTO> buscarSerieTemporal(LocalDate dataInicio, LocalDate dataFim) {
        return buscarSerieTemporal(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public List<ManifestosTrendPointDTO> buscarSerieTemporal(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return buscarSerieTemporalAgregada(consulta(filtro)).stream()
                .map(row -> new ManifestosTrendPointDTO(
                        row.getDate(),
                        row.getEncerrado(),
                        row.getEmTransito(),
                        row.getPendente()
                ))
                .toList();
    }

    public List<ManifestoResumoDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 200);

        return buscarTabelaPaginada(consulta(filtro), limiteAplicado).stream()
                .map(row -> new ManifestoResumoDTO(
                        row.getNumero(),
                        row.getIdentificadorUnico(),
                        row.getStatus(),
                        row.getClassificacao(),
                        row.getFilial(),
                        row.getDataCriacao(),
                        row.getFechamento(),
                        row.getMotorista(),
                        row.getVeiculoPlaca(),
                        row.getTipoVeiculo(),
                        zero(row.getTotalPesoTaxado()).setScale(2, RoundingMode.HALF_UP),
                        zero(row.getTotalM3()).setScale(2, RoundingMode.HALF_UP),
                        zero(row.getCustoTotal()).setScale(2, RoundingMode.HALF_UP),
                        zero(row.getValorFrete()).setScale(2, RoundingMode.HALF_UP),
                        zero(row.getCombustivel()).setScale(2, RoundingMode.HALF_UP),
                        zero(row.getPedagio()).setScale(2, RoundingMode.HALF_UP),
                        zero(row.getSaldoPagar()).setScale(2, RoundingMode.HALF_UP),
                        zero(row.getKmTotal()).setScale(2, RoundingMode.HALF_UP),
                        row.getItensTotal()
                ))
                .toList();
    }

    public ManifestosChartsDTO buscarGraficos(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        ManifestosConsulta consulta = consulta(filtro);

        List<ManifestosCustoPorFilialDTO> custoPorFilial = buscarCustoPorFilial(consulta).stream()
                .map(row -> new ManifestosCustoPorFilialDTO(
                        row.getFilial(),
                        zero(row.getCustoTotal()).setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();

        List<ManifestosRankingMotoristaDTO> rankingMotorista = buscarRankingMotorista(consulta).stream()
                .map(row -> new ManifestosRankingMotoristaDTO(
                        row.getMotorista(),
                        row.getManifestos(),
                        zero(row.getKm()).setScale(2, RoundingMode.HALF_UP),
                        zero(row.getCustoTotal()).setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();

        List<ManifestosComposicaoCustoDTO> composicaoCusto = buscarComposicaoCusto(consulta).stream()
                .map(row -> new ManifestosComposicaoCustoDTO(
                        row.getCategoria(),
                        zero(row.getValor()).setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();

        List<ManifestosOcupacaoScatterDTO> ocupacaoScatter = buscarOcupacaoScatter(consulta).stream()
                .map(row -> new ManifestosOcupacaoScatterDTO(
                        zero(row.getPesoTaxado()).setScale(2, RoundingMode.HALF_UP),
                        zero(row.getTotalM3()).setScale(2, RoundingMode.HALF_UP),
                        zero(row.getCustoTotal()).setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();

        return new ManifestosChartsDTO(custoPorFilial, rankingMotorista, composicaoCusto, ocupacaoScatter);
    }

    private BigDecimal zero(BigDecimal valor) {
        return ConsultaFiltroUtils.zeroSeNulo(valor);
    }

    private String formatarAtualizacao(LocalDateTime updatedAt) {
        return TemporalJsonUtils.formatarUtc(updatedAt);
    }

    private ManifestosConsulta consulta(FiltroConsultaDTO filtro) {
        return new ManifestosConsulta(
                filtro.dataInicio(),
                filtro.dataFim().plusDays(1),
                DashboardQueryFilters.escopo(escopoFilialService.escopoAtual()),
                DashboardQueryFilters.of(filtro.valores("filiais")),
                DashboardQueryFilters.of(filtro.valores("status")),
                DashboardQueryFilters.of(filtro.valores("motoristas")),
                DashboardQueryFilters.of(filtro.valores("veiculos")),
                DashboardQueryFilters.of(filtro.valores("tiposCarga")),
                DashboardQueryFilters.of(filtro.valores("tiposContrato"))
        );
    }

    private VisaoManifestosRepository.ManifestosOverviewProjection buscarOverviewAgregado(ManifestosConsulta consulta) {
        return repository.buscarOverviewAgregado(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.motoristas().valores(), consulta.motoristas().vazio(),
                consulta.veiculos().valores(), consulta.veiculos().vazio(),
                consulta.tiposCarga().valores(), consulta.tiposCarga().vazio(),
                consulta.tiposContrato().valores(), consulta.tiposContrato().vazio()
        );
    }

    private List<VisaoManifestosRepository.ManifestosTrendProjection> buscarSerieTemporalAgregada(
            ManifestosConsulta consulta
    ) {
        return repository.buscarSerieTemporalAgregada(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.motoristas().valores(), consulta.motoristas().vazio(),
                consulta.veiculos().valores(), consulta.veiculos().vazio(),
                consulta.tiposCarga().valores(), consulta.tiposCarga().vazio(),
                consulta.tiposContrato().valores(), consulta.tiposContrato().vazio()
        );
    }

    private List<VisaoManifestosRepository.ManifestoResumoProjection> buscarTabelaPaginada(
            ManifestosConsulta consulta,
            int limite
    ) {
        return repository.buscarTabelaPaginada(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.motoristas().valores(), consulta.motoristas().vazio(),
                consulta.veiculos().valores(), consulta.veiculos().vazio(),
                consulta.tiposCarga().valores(), consulta.tiposCarga().vazio(),
                consulta.tiposContrato().valores(), consulta.tiposContrato().vazio(),
                limite
        );
    }

    private List<VisaoManifestosRepository.ManifestosCustoFilialProjection> buscarCustoPorFilial(
            ManifestosConsulta consulta
    ) {
        return repository.buscarCustoPorFilial(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.motoristas().valores(), consulta.motoristas().vazio(),
                consulta.veiculos().valores(), consulta.veiculos().vazio(),
                consulta.tiposCarga().valores(), consulta.tiposCarga().vazio(),
                consulta.tiposContrato().valores(), consulta.tiposContrato().vazio()
        );
    }

    private List<VisaoManifestosRepository.ManifestosRankingMotoristaProjection> buscarRankingMotorista(
            ManifestosConsulta consulta
    ) {
        return repository.buscarRankingMotorista(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.motoristas().valores(), consulta.motoristas().vazio(),
                consulta.veiculos().valores(), consulta.veiculos().vazio(),
                consulta.tiposCarga().valores(), consulta.tiposCarga().vazio(),
                consulta.tiposContrato().valores(), consulta.tiposContrato().vazio()
        );
    }

    private List<VisaoManifestosRepository.ManifestosComposicaoProjection> buscarComposicaoCusto(
            ManifestosConsulta consulta
    ) {
        return repository.buscarComposicaoCusto(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.motoristas().valores(), consulta.motoristas().vazio(),
                consulta.veiculos().valores(), consulta.veiculos().vazio(),
                consulta.tiposCarga().valores(), consulta.tiposCarga().vazio(),
                consulta.tiposContrato().valores(), consulta.tiposContrato().vazio()
        );
    }

    private List<VisaoManifestosRepository.ManifestosOcupacaoProjection> buscarOcupacaoScatter(
            ManifestosConsulta consulta
    ) {
        return repository.buscarOcupacaoScatter(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.motoristas().valores(), consulta.motoristas().vazio(),
                consulta.veiculos().valores(), consulta.veiculos().vazio(),
                consulta.tiposCarga().valores(), consulta.tiposCarga().vazio(),
                consulta.tiposContrato().valores(), consulta.tiposContrato().vazio()
        );
    }

    private record ManifestosConsulta(
            LocalDate dataInicio,
            LocalDate dataFimExclusivo,
            DashboardQueryFilters.ParametroLista escopo,
            DashboardQueryFilters.ParametroLista filiais,
            DashboardQueryFilters.ParametroLista status,
            DashboardQueryFilters.ParametroLista motoristas,
            DashboardQueryFilters.ParametroLista veiculos,
            DashboardQueryFilters.ParametroLista tiposCarga,
            DashboardQueryFilters.ParametroLista tiposContrato
    ) {
    }

    private static EscopoFilialService escopoSemRestricao() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }
}
