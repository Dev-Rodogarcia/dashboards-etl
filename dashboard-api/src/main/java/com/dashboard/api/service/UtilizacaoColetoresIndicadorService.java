package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresRowDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresSeriePointDTO;
import com.dashboard.api.model.VisaoManifestosEntity;
import com.dashboard.api.repository.DimFilialRepository;
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

    private static final String CLASSIFICACAO_DISTRIBUICAO = "DISTRIBUIÇÃO";
    private static final String CLASSIFICACAO_TRANSFERENCIA = "TRANSFERÊNCIA";
    private static final String CLASSIFICACAO_NAO_INFORMADA = "Sem classificação";

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoManifestosRepository manifestosRepository;
    private final DimFilialRepository dimFilialRepository;
    private final EscopoFilialService escopoFilialService;
    private final PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper;

    UtilizacaoColetoresIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoManifestosRepository manifestosRepository,
            DimFilialRepository dimFilialRepository
    ) {
        this(
                validadorPeriodo,
                manifestosRepository,
                dimFilialRepository,
                escopoSemRestricao(),
                PeriodoOffsetDateTimeHelper.padrao()
        );
    }

    @Autowired
    public UtilizacaoColetoresIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoManifestosRepository manifestosRepository,
            DimFilialRepository dimFilialRepository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.manifestosRepository = manifestosRepository;
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

        Map<String, AcumuladorPonto> pontos = new LinkedHashMap<>();
        Set<String> emitidosRegistrados = new LinkedHashSet<>();
        Set<String> descarregamentosRegistrados = new LinkedHashSet<>();

        for (VisaoManifestosEntity manifesto : manifestos) {
            ManifestoElegivel registro = analisarManifesto(manifesto, filtro);
            if (registro == null) {
                continue;
            }

            boolean manifestoBipado = manifestoBipado(manifesto);
            boolean conferenciaIncompleta = conferenciaIncompleta(manifesto, manifestoBipado);
            String filialEmissora = primeiroTexto(manifesto.getFilialEmissora(), manifesto.getFilial());

            if (permiteFilial(escopo, filtro, filialEmissora)) {
                String chaveEmitido = registro.chaveManifesto() + "|saida|" + normalizarTexto(filialEmissora);
                if (emitidosRegistrados.add(chaveEmitido)) {
                    ponto(pontos, registro.data(), filialEmissora, registro.classificacao())
                            .registrarManifestoEmitido(manifestoBipado, conferenciaIncompleta, manifesto.getDataExtracao());
                }
            }

            for (String filialDescarga : extrairFiliaisDescarregamento(manifesto.getLocalDescarregamento())) {
                String filialCanonica = filiaisValidas.get(normalizarTexto(filialDescarga));
                if (filialCanonica == null || !permiteFilial(escopo, filtro, filialCanonica)) {
                    continue;
                }

                String chaveDescarga = registro.chaveManifesto() + "|chegada|" + normalizarTexto(filialCanonica);
                if (descarregamentosRegistrados.add(chaveDescarga)) {
                    ponto(pontos, registro.data(), filialCanonica, registro.classificacao())
                            .registrarManifestoDescarregamento(manifestoBipado, conferenciaIncompleta, manifesto.getDataExtracao());
                }
            }
        }

        return pontos.values().stream()
                .map(AcumuladorPonto::toPonto)
                .filter(ponto -> ponto.totalManifestos() > 0 || ponto.manifestosBipados() > 0)
                .toList();
    }

    private ManifestoElegivel analisarManifesto(VisaoManifestosEntity manifesto, FiltroConsultaDTO filtro) {
        LocalDate data = manifesto.getDataCriacao() != null ? manifesto.getDataCriacao().toLocalDate() : null;
        if (data == null) {
            return null;
        }
        if (!statusElegivel(manifesto.getStatus()) || classificacaoExcluida(manifesto.getClassificacao())) {
            return null;
        }

        String classificacao = rotuloClassificacao(manifesto.getClassificacao());
        if (!permiteClassificacao(filtro, classificacao)) {
            return null;
        }

        String chaveManifesto = chaveManifesto(manifesto.getNumero(), manifesto.getIdentificadorUnico());
        if (chaveManifesto == null) {
            return null;
        }

        return new ManifestoElegivel(chaveManifesto, data, classificacao);
    }

    @NonNull
    private Specification<VisaoManifestosEntity> criarManifestosSpecification(JanelaOffsetDateTime janela) {
        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.greaterThanOrEqualTo("dataCriacao", janela.inicioInclusivo()),
                ConsultaSpecificationUtils.lessThan("dataCriacao", janela.fimExclusivo())
        );
    }

    private AcumuladorPonto ponto(Map<String, AcumuladorPonto> pontos, LocalDate data, String filial, String classificacao) {
        String filialNormalizada = textoOuPadrao(filial, "Filial nao informada");
        String classificacaoNormalizada = textoOuPadrao(classificacao, CLASSIFICACAO_NAO_INFORMADA);
        return pontos.computeIfAbsent(
                chavePonto(data, filialNormalizada, classificacaoNormalizada),
                chave -> new AcumuladorPonto(data, filialNormalizada, classificacaoNormalizada)
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
        String valor = temTexto(filial) ? filial.trim() : null;
        return valor != null
                && escopo.permiteAlgumaFilial(valor)
                && filtro.corresponde("filiais", valor);
    }

    private boolean permiteClassificacao(FiltroConsultaDTO filtro, String classificacao) {
        if (!filtro.temFiltro("classificacoes")) {
            return true;
        }
        String classificacaoNormalizada = normalizarTexto(classificacao);
        return filtro.valores("classificacoes").stream()
                .map(this::rotuloClassificacao)
                .map(this::normalizarTexto)
                .anyMatch(classificacaoNormalizada::equals);
    }

    private boolean statusElegivel(String status) {
        String normalizado = normalizarTexto(status);
        return normalizado.equals("encerrado") || normalizado.equals("closed");
    }

    private boolean classificacaoExcluida(String classificacao) {
        String normalizado = normalizarTexto(classificacao);
        return normalizado.startsWith("carga fechada")
                || normalizado.startsWith("frete retorno")
                || normalizado.startsWith("viagem vazia");
    }

    private boolean manifestoBipado(VisaoManifestosEntity manifesto) {
        return manifesto.getLeituraMovelEm() != null || inteiro(manifesto.getItensFinalizados()) > 0;
    }

    private boolean conferenciaIncompleta(VisaoManifestosEntity manifesto, boolean manifestoBipado) {
        if (!manifestoBipado) {
            return false;
        }
        int itensTotal = inteiro(manifesto.getItensTotal());
        int itensFinalizados = inteiro(manifesto.getItensFinalizados());
        return itensTotal > 0 && itensFinalizados < itensTotal;
    }

    private int inteiro(Integer valor) {
        return valor == null ? 0 : valor;
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

    private String rotuloClassificacao(String classificacao) {
        String valor = textoOuPadrao(classificacao, CLASSIFICACAO_NAO_INFORMADA);
        String normalizado = normalizarTexto(valor);
        if (normalizado.startsWith("distribuicao")) {
            return CLASSIFICACAO_DISTRIBUICAO;
        }
        if (normalizado.startsWith("transferencia")) {
            return CLASSIFICACAO_TRANSFERENCIA;
        }
        return valor;
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
        if (numero != null && identificador != null) {
            return numero + "|" + identificador;
        }
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
            LocalDate data,
            String classificacao
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

        private void registrarManifestoEmitido(boolean manifestoBipado, boolean conferenciaIncompleta, LocalDateTime dataExtracao) {
            manifestosEmitidos++;
            registrarQualidade(manifestoBipado, conferenciaIncompleta, dataExtracao);
        }

        private void registrarManifestoDescarregamento(boolean manifestoBipado, boolean conferenciaIncompleta, LocalDateTime dataExtracao) {
            manifestosDescarregamento++;
            registrarQualidade(manifestoBipado, conferenciaIncompleta, dataExtracao);
        }

        private void registrarQualidade(boolean manifestoBipado, boolean conferenciaIncompleta, LocalDateTime dataExtracao) {
            if (manifestoBipado) {
                manifestosBipados++;
            }
            if (conferenciaIncompleta) {
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
