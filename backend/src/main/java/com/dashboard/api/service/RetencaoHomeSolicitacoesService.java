package com.dashboard.api.service;

import com.dashboard.api.repository.acesso.HomeSolicitacaoMelhoriaAnexoRepository;
import com.dashboard.api.repository.acesso.HomeSolicitacaoMelhoriaRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetencaoHomeSolicitacoesService {

    private static final Logger log = LoggerFactory.getLogger(RetencaoHomeSolicitacoesService.class);

    private final HomeSolicitacaoMelhoriaRepository solicitacaoRepository;
    private final HomeSolicitacaoMelhoriaAnexoRepository anexoRepository;
    private final long concluidasRetencaoDias;
    private final long arquivadasRetencaoDias;

    public RetencaoHomeSolicitacoesService(
            HomeSolicitacaoMelhoriaRepository solicitacaoRepository,
            HomeSolicitacaoMelhoriaAnexoRepository anexoRepository,
            @Value("${home.solicitacoes.retencao.concluidas-dias:60}") long concluidasRetencaoDias,
            @Value("${home.solicitacoes.retencao.arquivadas-dias:60}") long arquivadasRetencaoDias
    ) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.anexoRepository = anexoRepository;
        this.concluidasRetencaoDias = concluidasRetencaoDias;
        this.arquivadasRetencaoDias = arquivadasRetencaoDias;
    }

    @Scheduled(cron = "${home.solicitacoes.retencao.cron:0 45 3 * * *}")
    public void executarRetencaoAgendada() {
        try {
            ResultadoRetencao resultado = executarRetencao(Instant.now());
            if (resultado.concluidasArquivadas() > 0 || resultado.arquivadasOcultadas() > 0) {
                log.info(
                        "Retencao de solicitacoes concluida: concluidasArquivadas={}, arquivadasOcultadas={}",
                        resultado.concluidasArquivadas(),
                        resultado.arquivadasOcultadas()
                );
            }
        } catch (Exception ex) {
            log.error("Falha na retencao de solicitacoes de melhoria.", ex);
        }
    }

    @Transactional
    public ResultadoRetencao executarRetencao(Instant referencia) {
        Instant limiteConclusao = referencia.minus(concluidasRetencaoDias, ChronoUnit.DAYS);
        Instant limiteArquivamento = referencia.minus(arquivadasRetencaoDias, ChronoUnit.DAYS);
        anexoRepository.removerConteudosDasConcluidasAntes(limiteConclusao, referencia);
        int concluidasArquivadas = solicitacaoRepository.arquivarConcluidasAntes(limiteConclusao, referencia);
        int arquivadasOcultadas = solicitacaoRepository.ocultarArquivadasExpiradas(limiteArquivamento, referencia);
        return new ResultadoRetencao(concluidasArquivadas, arquivadasOcultadas);
    }

    public record ResultadoRetencao(int concluidasArquivadas, int arquivadasOcultadas) {
    }
}
