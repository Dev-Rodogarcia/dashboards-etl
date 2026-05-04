package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresRowDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresSeriePointDTO;
import com.dashboard.api.model.VisaoInventarioEntity;
import com.dashboard.api.model.VisaoManifestosEntity;
import com.dashboard.api.repository.DimFilialRepository;
import com.dashboard.api.repository.VisaoInventarioRepository;
import com.dashboard.api.repository.VisaoManifestosRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private static final String CLASSIFICACAO_GERAL = "Geral";
    private static final Set<String> TIPOS_ORDEM_CONFERENCIA = Set.of(
            "picking",
            "retorno",
            "recebimento",
            "carregamento",
            "descarregamento"
    );

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoManifestosRepository manifestosRepository;
    private final VisaoInventarioRepository inventarioRepository;
    private final DimFilialRepository dimFilialRepository;
    private final EscopoFilialService escopoFilialService;
    private final PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper;

    UtilizacaoColetoresIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoManifestosRepository manifestosRepository,
            VisaoInventarioRepository inventarioRepository,
            DimFilialRepository dimFilialRepository
    ) {
        this(
                validadorPeriodo,
                manifestosRepository,
                inventarioRepository,
                dimFilialRepository,
                escopoSemRestricao(),
                PeriodoOffsetDateTimeHelper.padrao()
        );
    }

    @Autowired
    public UtilizacaoColetoresIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoManifestosRepository manifestosRepository,
            VisaoInventarioRepository inventarioRepository,
            DimFilialRepository dimFilialRepository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.manifestosRepository = manifestosRepository;
        this.inventarioRepository = inventarioRepository;
        this.dimFilialRepository = dimFilialRepository;
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
                    0,
                    0.0
            );
        }

        int manifestosBipados = pontos.stream().mapToInt(UtilizacaoColetoresPonto::manifestosBipados).sum();
        int manifestosEmitidos = pontos.stream().mapToInt(UtilizacaoColetoresPonto::manifestosEmitidos).sum();
        int manifestosDescarregamento = pontos.stream().mapToInt(UtilizacaoColetoresPonto::manifestosDescarregamento).sum();
        int totalManifestos = pontos.stream().mapToInt(UtilizacaoColetoresPonto::totalManifestos).sum();
        int manifestosIncompletos = pontos.stream().mapToInt(UtilizacaoColetoresPonto::manifestosIncompletos).sum();

        return new UtilizacaoColetoresOverviewDTO(
                IndicadoresGestaoMetricasUtils.latestUpdate(pontos, UtilizacaoColetoresPonto::updatedAt),
                manifestosBipados,
                manifestosEmitidos,
                manifestosDescarregamento,
                totalManifestos,
                manifestosIncompletos,
                IndicadoresGestaoMetricasUtils.percentual(manifestosBipados, totalManifestos)
        );
    }

    public List<UtilizacaoColetoresSeriePointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return buscarPontos(filtro).stream()
                .map(ponto -> new UtilizacaoColetoresSeriePointDTO(
                        ponto.data().toString(),
                        ponto.filial(),
                        ponto.classificacao(),
                        ponto.manifestosBipados(),
                        ponto.manifestosEmitidos(),
                        ponto.manifestosDescarregamento(),
                        ponto.totalManifestos(),
                        ponto.manifestosIncompletos(),
                        IndicadoresGestaoMetricasUtils.percentual(ponto.manifestosBipados(), ponto.totalManifestos())
                ))
                .sorted(Comparator.comparing(UtilizacaoColetoresSeriePointDTO::date)
                        .thenComparing(UtilizacaoColetoresSeriePointDTO::filial, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(UtilizacaoColetoresSeriePointDTO::classificacao, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public List<UtilizacaoColetoresRowDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 500);

        return buscarPontos(filtro).stream()
                .sorted(Comparator.comparing(UtilizacaoColetoresPonto::data, Comparator.reverseOrder())
                        .thenComparing(UtilizacaoColetoresPonto::filial, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(UtilizacaoColetoresPonto::classificacao, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .limit(limiteAplicado)
                .map(ponto -> new UtilizacaoColetoresRowDTO(
                        chavePonto(ponto.data(), ponto.filial(), ponto.classificacao()),
                        IndicadoresGestaoMetricasUtils.formatar(ponto.data()),
                        ponto.filial(),
                        ponto.classificacao(),
                        ponto.manifestosBipados(),
                        ponto.manifestosEmitidos(),
                        ponto.manifestosDescarregamento(),
                        ponto.totalManifestos(),
                        ponto.manifestosIncompletos(),
                        IndicadoresGestaoMetricasUtils.percentual(ponto.manifestosBipados(), ponto.totalManifestos())
                ))
                .toList();
    }

    public List<UtilizacaoColetoresRowDTO> buscarExportacao(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return buscarPontos(filtro).stream()
                .sorted(Comparator.comparing(UtilizacaoColetoresPonto::data, Comparator.reverseOrder())
                        .thenComparing(UtilizacaoColetoresPonto::filial, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(UtilizacaoColetoresPonto::classificacao, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(ponto -> new UtilizacaoColetoresRowDTO(
                        chavePonto(ponto.data(), ponto.filial(), ponto.classificacao()),
                        IndicadoresGestaoMetricasUtils.formatar(ponto.data()),
                        ponto.filial(),
                        ponto.classificacao(),
                        ponto.manifestosBipados(),
                        ponto.manifestosEmitidos(),
                        ponto.manifestosDescarregamento(),
                        ponto.totalManifestos(),
                        ponto.manifestosIncompletos(),
                        IndicadoresGestaoMetricasUtils.percentual(ponto.manifestosBipados(), ponto.totalManifestos())
                ))
                .toList();
    }

    public PaginaDTO<UtilizacaoColetoresRowDTO> buscarTabelaPaginada(FiltroConsultaDTO filtro, int pagina, int tamanhoPagina) {
        return PaginacaoListaUtils.paginar(buscarExportacao(filtro), pagina, tamanhoPagina);
    }

    private List<UtilizacaoColetoresPonto> buscarPontos(FiltroConsultaDTO filtro) {
        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        Map<String, String> filiaisValidas = carregarFiliaisValidas();
        List<VisaoManifestosEntity> manifestos = manifestosRepository.findAll(criarManifestosSpecification(janela));
        List<VisaoInventarioEntity> ordens = inventarioRepository.findAll(criarInventarioSpecification(janela));

        Map<String, AcumuladorPonto> pontos = new LinkedHashMap<>();
        Set<String> emitidosRegistrados = new LinkedHashSet<>();
        Set<String> descarregamentosRegistrados = new LinkedHashSet<>();
        Set<Long> ordensRegistradas = new LinkedHashSet<>();

        for (VisaoManifestosEntity manifesto : manifestos) {
            ManifestoElegivel registro = analisarManifesto(manifesto);
            if (registro == null) {
                continue;
            }

            String filialEmissora = primeiroTexto(manifesto.getFilialEmissora(), manifesto.getFilial());

            if (permiteFilial(escopo, filtro, filialEmissora)) {
                String chaveEmitido = registro.chaveManifesto();
                if (emitidosRegistrados.add(chaveEmitido)) {
                    ponto(pontos, registro.data(), filialEmissora)
                            .registrarManifestoEmitido(manifesto.getDataExtracao());
                }
            }

            String filialDescarga = filialDescarregamentoElegivel(
                    manifesto.getLocalDescarregamento(),
                    filiaisValidas,
                    escopo,
                    filtro
            );
            if (filialDescarga != null && descarregamentosRegistrados.add(registro.chaveManifesto())) {
                ponto(pontos, registro.data(), filialDescarga)
                        .registrarManifestoDescarregamento(manifesto.getDataExtracao());
            }
        }

        for (VisaoInventarioEntity ordem : ordens) {
            OrdemConferenciaElegivel registro = analisarOrdemConferencia(ordem, filtro, escopo);
            if (registro == null || !ordensRegistradas.add(registro.numeroOrdem())) {
                continue;
            }

            ponto(pontos, registro.data(), registro.filial())
                    .registrarOrdemConferencia(registro.incompleta(), ordem.getDataExtracao());
        }

        return pontos.values().stream()
                .map(AcumuladorPonto::toPonto)
                .filter(ponto -> ponto.totalManifestos() > 0 || ponto.manifestosBipados() > 0)
                .toList();
    }

    private ManifestoElegivel analisarManifesto(VisaoManifestosEntity manifesto) {
        LocalDate data = manifesto.getDataCriacao() != null ? manifesto.getDataCriacao().toLocalDate() : null;
        if (data == null) {
            return null;
        }
        if (classificacaoExcluida(manifesto.getClassificacao())) {
            return null;
        }

        String chaveManifesto = chaveManifesto(manifesto.getNumero(), manifesto.getIdentificadorUnico());
        if (chaveManifesto == null) {
            return null;
        }

        return new ManifestoElegivel(chaveManifesto, data);
    }

    private OrdemConferenciaElegivel analisarOrdemConferencia(
            VisaoInventarioEntity ordem,
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        Long numeroOrdem = ordem.getNumeroOrdem();
        LocalDate data = ordem.getDataHoraInicio() != null ? ordem.getDataHoraInicio().toLocalDate() : null;
        if (numeroOrdem == null || data == null || !tipoOrdemElegivel(ordem.getTipo())) {
            return null;
        }

        String filial = primeiroTexto(
                ordem.getFilialOrdemConferencia(),
                ordem.getFilial(),
                ordem.getFilialEmissoraFrete()
        );
        if (!permiteFilial(escopo, filtro, filial)) {
            return null;
        }

        return new OrdemConferenciaElegivel(
                numeroOrdem,
                data,
                filial,
                ordem.getDataHoraFim() == null
        );
    }

    @NonNull
    private Specification<VisaoManifestosEntity> criarManifestosSpecification(JanelaOffsetDateTime janela) {
        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.greaterThanOrEqualTo("dataCriacao", janela.inicioInclusivo()),
                ConsultaSpecificationUtils.lessThan("dataCriacao", janela.fimExclusivo())
        );
    }

    @NonNull
    private Specification<VisaoInventarioEntity> criarInventarioSpecification(JanelaOffsetDateTime janela) {
        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.greaterThanOrEqualTo("dataHoraInicio", janela.inicioInclusivo()),
                ConsultaSpecificationUtils.lessThan("dataHoraInicio", janela.fimExclusivo())
        );
    }

    private AcumuladorPonto ponto(Map<String, AcumuladorPonto> pontos, LocalDate data, String filial) {
        String filialNormalizada = textoOuPadrao(filial, "Filial nao informada");
        return pontos.computeIfAbsent(
                chavePonto(data, filialNormalizada, CLASSIFICACAO_GERAL),
                chave -> new AcumuladorPonto(data, filialNormalizada, CLASSIFICACAO_GERAL)
        );
    }

    private String chavePonto(LocalDate data, String filial, String classificacao) {
        return IndicadoresGestaoMetricasUtils.chaveSerie(data, filial) + "|" + normalizarTexto(classificacao);
    }

    private Map<String, String> carregarFiliaisValidas() {
        Map<String, String> filiais = new LinkedHashMap<>();
        dimFilialRepository.findAll().stream()
                .map(filial -> filial.getNomeFilial())
                .filter(this::temTexto)
                .map(String::trim)
                .forEach(nome -> filiais.putIfAbsent(normalizarTexto(nome), nome));
        return filiais;
    }

    private boolean permiteFilial(
            EscopoFilialService.EscopoFilial escopo,
            FiltroConsultaDTO filtro,
            String filial
    ) {
        if (!temTexto(filial)) {
            return escopo.acessoTotal() && !filtro.temFiltro("filiais");
        }

        String valor = filial.trim();
        return escopo.permiteAlgumaFilial(valor)
                && filtro.corresponde("filiais", valor);
    }

    private boolean classificacaoExcluida(String classificacao) {
        String normalizado = normalizarTexto(classificacao);
        return normalizado.startsWith("carga fechada")
                || normalizado.startsWith("acerto de motorista")
                || normalizado.startsWith("frete retorno")
                || normalizado.startsWith("viagem vazia");
    }

    private String filialDescarregamentoElegivel(
            String localDescarregamento,
            Map<String, String> filiaisValidas,
            EscopoFilialService.EscopoFilial escopo,
            FiltroConsultaDTO filtro
    ) {
        for (String filialDescarga : extrairFiliaisDescarregamento(localDescarregamento)) {
            String filialCanonica = filiaisValidas.getOrDefault(normalizarTexto(filialDescarga), filialDescarga);
            if (permiteFilial(escopo, filtro, filialCanonica)) {
                return filialCanonica;
            }
        }
        return null;
    }

    private boolean tipoOrdemElegivel(String tipo) {
        return TIPOS_ORDEM_CONFERENCIA.contains(normalizarTexto(tipo));
    }

    private List<String> extrairFiliaisDescarregamento(String localDescarregamento) {
        if (!temTexto(localDescarregamento)) {
            return List.of();
        }

        List<String> filiais = new ArrayList<>();
        Set<String> registradas = new LinkedHashSet<>();
        for (String parte : localDescarregamento.split("[,;\\n]+")) {
            String valor = textoOuPadrao(parte, "");
            String normalizado = normalizarTexto(valor);
            if (normalizado.isBlank() || normalizado.equals("null")) {
                continue;
            }
            if (registradas.add(normalizado)) {
                filiais.add(valor);
            }
        }
        return filiais;
    }

    private String textoOuPadrao(String valor, String padrao) {
        return temTexto(valor) ? valor.trim() : padrao;
    }

    private String primeiroTexto(String... valores) {
        for (String valor : valores) {
            if (temTexto(valor)) {
                return valor.trim();
            }
        }
        return null;
    }

    private String chaveManifesto(Long numero, String identificadorUnico) {
        String identificador = temTexto(identificadorUnico) ? identificadorUnico.trim() : null;
        if (numero != null) {
            return numero.toString();
        }
        return identificador;
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String normalizarTexto(String valor) {
        String texto = Objects.toString(valor, "").trim().toLowerCase(Locale.ROOT);
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcento.replaceAll("\\s+", " ");
    }

    private static EscopoFilialService escopoSemRestricao() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }

    private record ManifestoElegivel(
            String chaveManifesto,
            LocalDate data
    ) {
    }

    private record OrdemConferenciaElegivel(
            Long numeroOrdem,
            LocalDate data,
            String filial,
            boolean incompleta
    ) {
    }

    private record UtilizacaoColetoresPonto(
            LocalDate data,
            String filial,
            String classificacao,
            int manifestosBipados,
            int manifestosEmitidos,
            int manifestosDescarregamento,
            int manifestosIncompletos,
            LocalDateTime updatedAt
    ) {
        private int totalManifestos() {
            return manifestosEmitidos + manifestosDescarregamento;
        }
    }

    private static final class AcumuladorPonto {
        private final LocalDate data;
        private final String filial;
        private final String classificacao;
        private int manifestosBipados;
        private int manifestosEmitidos;
        private int manifestosDescarregamento;
        private int manifestosIncompletos;
        private LocalDateTime updatedAt;

        private AcumuladorPonto(LocalDate data, String filial, String classificacao) {
            this.data = data;
            this.filial = filial;
            this.classificacao = classificacao;
        }

        private void registrarManifestoEmitido(LocalDateTime dataExtracao) {
            manifestosEmitidos++;
            atualizarDataExtracao(dataExtracao);
        }

        private void registrarManifestoDescarregamento(LocalDateTime dataExtracao) {
            manifestosDescarregamento++;
            atualizarDataExtracao(dataExtracao);
        }

        private void registrarOrdemConferencia(boolean incompleta, LocalDateTime dataExtracao) {
            manifestosBipados++;
            if (incompleta) {
                manifestosIncompletos++;
            }
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
                    classificacao,
                    manifestosBipados,
                    manifestosEmitidos,
                    manifestosDescarregamento,
                    manifestosIncompletos,
                    updatedAt
            );
        }
    }
}
