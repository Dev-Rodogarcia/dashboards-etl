package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresRowDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresSeriePointDTO;
import com.dashboard.api.model.VisaoInventarioEntity;
import com.dashboard.api.model.VisaoManifestosEntity;
import com.dashboard.api.repository.VisaoInventarioRepository;
import com.dashboard.api.repository.VisaoManifestosRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class UtilizacaoColetoresIndicadorService {

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoInventarioRepository inventarioRepository;
    private final VisaoManifestosRepository manifestosRepository;
    private final EscopoFilialService escopoFilialService;
    private final PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper;

    UtilizacaoColetoresIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoInventarioRepository inventarioRepository,
            VisaoManifestosRepository manifestosRepository
    ) {
        this(validadorPeriodo, inventarioRepository, manifestosRepository, escopoSemRestricao(), PeriodoOffsetDateTimeHelper.padrao());
    }

    @Autowired
    public UtilizacaoColetoresIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoInventarioRepository inventarioRepository,
            VisaoManifestosRepository manifestosRepository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.inventarioRepository = inventarioRepository;
        this.manifestosRepository = manifestosRepository;
        this.escopoFilialService = escopoFilialService;
        this.periodoOffsetDateTimeHelper = periodoOffsetDateTimeHelper;
    }

    public UtilizacaoColetoresOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<UtilizacaoColetoresPonto> pontos = buscarPontos(filtro);
        if (pontos.isEmpty()) {
            return new UtilizacaoColetoresOverviewDTO(
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    0,
                    0,
                    0,
                    0,
                    0.0
            );
        }

        int ordensConferencia = pontos.stream().mapToInt(UtilizacaoColetoresPonto::ordensConferencia).sum();
        int manifestosEmitidos = pontos.stream().mapToInt(UtilizacaoColetoresPonto::manifestosEmitidos).sum();
        int manifestosDescarregamento = pontos.stream().mapToInt(UtilizacaoColetoresPonto::manifestosDescarregamento).sum();
        int totalManifestos = pontos.stream().mapToInt(UtilizacaoColetoresPonto::totalManifestos).sum();

        return new UtilizacaoColetoresOverviewDTO(
                IndicadoresGestaoMetricasUtils.latestUpdate(pontos, UtilizacaoColetoresPonto::updatedAt),
                ordensConferencia,
                manifestosEmitidos,
                manifestosDescarregamento,
                totalManifestos,
                IndicadoresGestaoMetricasUtils.percentual(ordensConferencia, totalManifestos)
        );
    }

    public List<UtilizacaoColetoresSeriePointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return buscarPontos(filtro).stream()
                .map(ponto -> new UtilizacaoColetoresSeriePointDTO(
                        ponto.data().toString(),
                        ponto.filial(),
                        ponto.ordensConferencia(),
                        ponto.manifestosEmitidos(),
                        ponto.manifestosDescarregamento(),
                        ponto.totalManifestos(),
                        IndicadoresGestaoMetricasUtils.percentual(ponto.ordensConferencia(), ponto.totalManifestos())
                ))
                .sorted(Comparator.comparing(UtilizacaoColetoresSeriePointDTO::date)
                        .thenComparing(UtilizacaoColetoresSeriePointDTO::filial, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public List<UtilizacaoColetoresRowDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 500);

        return buscarPontos(filtro).stream()
                .sorted(Comparator.comparing(UtilizacaoColetoresPonto::data, Comparator.reverseOrder())
                        .thenComparing(UtilizacaoColetoresPonto::filial, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .limit(limiteAplicado)
                .map(ponto -> new UtilizacaoColetoresRowDTO(
                        chavePonto(ponto.data(), ponto.filial()),
                        IndicadoresGestaoMetricasUtils.formatar(ponto.data()),
                        ponto.filial(),
                        ponto.ordensConferencia(),
                        ponto.manifestosEmitidos(),
                        ponto.manifestosDescarregamento(),
                        ponto.totalManifestos(),
                        IndicadoresGestaoMetricasUtils.percentual(ponto.ordensConferencia(), ponto.totalManifestos())
                ))
                .toList();
    }

    private List<UtilizacaoColetoresPonto> buscarPontos(FiltroConsultaDTO filtro) {
        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        List<VisaoInventarioEntity> inventarios = inventarioRepository.findAll(criarInventarioSpecification(filtro, escopo, janela));
        List<VisaoManifestosEntity> manifestos = manifestosRepository.findAll(criarManifestosSpecification(janela));

        Map<String, AcumuladorPonto> pontos = new LinkedHashMap<>();
        popularOrdensConferencia(pontos, inventarios, filtro, escopo);
        popularManifestos(pontos, manifestos, filtro, escopo);

        return pontos.values().stream()
                .map(AcumuladorPonto::toPonto)
                .filter(ponto -> ponto.totalManifestos() > 0 || ponto.ordensConferencia() > 0)
                .toList();
    }

    private void popularOrdensConferencia(
            Map<String, AcumuladorPonto> pontos,
            List<VisaoInventarioEntity> inventarios,
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        Map<Long, InventarioRegistro> ordens = new LinkedHashMap<>();
        for (VisaoInventarioEntity inventario : inventarios) {
            Long numeroOrdem = inventario.getNumeroOrdem();
            if (numeroOrdem == null || !tipoRelevante(inventario.getTipo())) {
                continue;
            }

            String filial = primeiroTexto(
                    inventario.getFilialEmissoraFrete(),
                    inventario.getFilialOrdemConferencia(),
                    inventario.getFilial()
            );
            if (!permiteFilial(escopo, filtro, filial)) {
                continue;
            }

            LocalDate data = dataInventario(inventario);
            if (data == null) {
                continue;
            }

            InventarioRegistro registro = new InventarioRegistro(numeroOrdem, data, filial, inventario.getDataExtracao());
            ordens.merge(numeroOrdem, registro, this::preferirInventarioMaisAtual);
        }

        ordens.values().forEach(registro -> ponto(pontos, registro.data(), registro.filial())
                .registrarOrdem(registro.updatedAt()));
    }

    private void popularManifestos(
            Map<String, AcumuladorPonto> pontos,
            List<VisaoManifestosEntity> manifestos,
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        Set<String> emitidosRegistrados = new LinkedHashSet<>();
        Set<String> descarregamentosRegistrados = new LinkedHashSet<>();

        for (VisaoManifestosEntity manifesto : manifestos) {
            Long numero = manifesto.getNumero();
            LocalDate data = manifesto.getDataCriacao() != null ? manifesto.getDataCriacao().toLocalDate() : null;
            if (numero == null || data == null) {
                continue;
            }

            String filialEmissora = primeiroTexto(manifesto.getFilialEmissora(), manifesto.getFilial());
            if (permiteFilial(escopo, filtro, filialEmissora)) {
                String chaveEmitido = numero + "|" + data + "|" + normalizar(filialEmissora);
                if (emitidosRegistrados.add(chaveEmitido)) {
                    ponto(pontos, data, filialEmissora).registrarManifestoEmitido(manifesto.getDataExtracao());
                }
            }

            for (String filialDescarga : extrairFiliaisDescarregamento(manifesto.getLocalDescarregamento())) {
                if (!permiteFilial(escopo, filtro, filialDescarga)) {
                    continue;
                }
                String chaveDescarga = numero + "|" + data + "|" + normalizar(filialDescarga);
                if (descarregamentosRegistrados.add(chaveDescarga)) {
                    ponto(pontos, data, filialDescarga).registrarManifestoDescarregamento(manifesto.getDataExtracao());
                }
            }
        }
    }

    @NonNull
    private Specification<VisaoInventarioEntity> criarInventarioSpecification(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            JanelaOffsetDateTime janela
    ) {
        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.greaterThanOrEqualTo("dataHoraInicio", janela.inicioInclusivo()),
                ConsultaSpecificationUtils.lessThan("dataHoraInicio", janela.fimExclusivo()),
                ConsultaSpecificationUtils.escopoFiliais(escopo, "filialEmissoraFrete", "filialOrdemConferencia", "filial"),
                ConsultaSpecificationUtils.filtroTextoQualquerCampo(filtro, "filiais", "filialEmissoraFrete", "filialOrdemConferencia", "filial")
        );
    }

    @NonNull
    private Specification<VisaoManifestosEntity> criarManifestosSpecification(JanelaOffsetDateTime janela) {
        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.greaterThanOrEqualTo("dataCriacao", janela.inicioInclusivo()),
                ConsultaSpecificationUtils.lessThan("dataCriacao", janela.fimExclusivo())
        );
    }

    private AcumuladorPonto ponto(Map<String, AcumuladorPonto> pontos, LocalDate data, String filial) {
        return pontos.computeIfAbsent(
                chavePonto(data, filial),
                chave -> new AcumuladorPonto(data, filial)
        );
    }

    private String chavePonto(LocalDate data, String filial) {
        return IndicadoresGestaoMetricasUtils.chaveSerie(data, filial);
    }

    private InventarioRegistro preferirInventarioMaisAtual(InventarioRegistro atual, InventarioRegistro candidato) {
        if (atual.updatedAt() == null) {
            return candidato;
        }
        if (candidato.updatedAt() == null) {
            return atual;
        }
        return candidato.updatedAt().isAfter(atual.updatedAt()) ? candidato : atual;
    }

    private LocalDate dataInventario(VisaoInventarioEntity inventario) {
        OffsetDateTime dataHora = inventario.getDataHoraInicio() != null ? inventario.getDataHoraInicio() : inventario.getDataHoraFim();
        return dataHora != null ? dataHora.toLocalDate() : null;
    }

    private boolean permiteFilial(
            EscopoFilialService.EscopoFilial escopo,
            FiltroConsultaDTO filtro,
            String filial
    ) {
        return filial != null
                && escopo.permiteAlgumaFilial(filial)
                && filtro.corresponde("filiais", filial);
    }

    private boolean tipoRelevante(String tipo) {
        String normalizado = normalizar(tipo);
        return normalizado.equals("carregamento")
                || normalizado.equals("loading")
                || normalizado.equals("descarregamento")
                || normalizado.equals("unloading")
                || normalizado.equals("picking");
    }

    private List<String> extrairFiliaisDescarregamento(String localDescarregamento) {
        if (localDescarregamento == null || localDescarregamento.isBlank()) {
            return List.of();
        }

        List<String> filiais = new ArrayList<>();
        for (String parte : localDescarregamento.split("[,;\\n]+")) {
            String valor = parte == null ? "" : parte.trim();
            if (!valor.isBlank()) {
                filiais.add(valor);
            }
        }
        return filiais.stream().distinct().toList();
    }

    private String primeiroTexto(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor.trim();
            }
        }
        return null;
    }

    private String normalizar(String valor) {
        return Objects.toString(valor, "").trim().toLowerCase(Locale.ROOT);
    }

    private static EscopoFilialService escopoSemRestricao() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }

    private record InventarioRegistro(
            long numeroOrdem,
            LocalDate data,
            String filial,
            LocalDateTime updatedAt
    ) {
    }

    private record UtilizacaoColetoresPonto(
            LocalDate data,
            String filial,
            int ordensConferencia,
            int manifestosEmitidos,
            int manifestosDescarregamento,
            LocalDateTime updatedAt
    ) {
        private int totalManifestos() {
            return manifestosEmitidos + manifestosDescarregamento;
        }
    }

    private static final class AcumuladorPonto {
        private final LocalDate data;
        private final String filial;
        private int ordensConferencia;
        private int manifestosEmitidos;
        private int manifestosDescarregamento;
        private LocalDateTime updatedAt;

        private AcumuladorPonto(LocalDate data, String filial) {
            this.data = data;
            this.filial = filial;
        }

        private void registrarOrdem(LocalDateTime dataExtracao) {
            ordensConferencia++;
            atualizarDataExtracao(dataExtracao);
        }

        private void registrarManifestoEmitido(LocalDateTime dataExtracao) {
            manifestosEmitidos++;
            atualizarDataExtracao(dataExtracao);
        }

        private void registrarManifestoDescarregamento(LocalDateTime dataExtracao) {
            manifestosDescarregamento++;
            atualizarDataExtracao(dataExtracao);
        }

        private void atualizarDataExtracao(LocalDateTime dataExtracao) {
            if (dataExtracao == null) {
                return;
            }
            if (updatedAt == null || dataExtracao.isAfter(updatedAt)) {
                updatedAt = dataExtracao;
            }
        }

        private UtilizacaoColetoresPonto toPonto() {
            return new UtilizacaoColetoresPonto(
                    data,
                    filial,
                    ordensConferencia,
                    manifestosEmitidos,
                    manifestosDescarregamento,
                    updatedAt
            );
        }
    }
}
