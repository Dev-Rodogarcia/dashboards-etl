package com.dashboard.api.service;

import com.dashboard.api.repository.acesso.HomeSolicitacaoMelhoriaAnexoRepository;
import com.dashboard.api.repository.acesso.HomeSolicitacaoMelhoriaRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetencaoHomeSolicitacoesServiceTest {

    @Test
    void deveArquivarConcluidasEAposSessentaDiasOcultarArquivadas() {
        HomeSolicitacaoMelhoriaRepository solicitacaoRepository = mock(HomeSolicitacaoMelhoriaRepository.class);
        HomeSolicitacaoMelhoriaAnexoRepository anexoRepository = mock(HomeSolicitacaoMelhoriaAnexoRepository.class);
        RetencaoHomeSolicitacoesService service = new RetencaoHomeSolicitacoesService(
                solicitacaoRepository,
                anexoRepository,
                60,
                60
        );
        Instant referencia = Instant.parse("2026-07-28T03:45:00Z");
        Instant limite = Instant.parse("2026-05-29T03:45:00Z");
        when(solicitacaoRepository.arquivarConcluidasAntes(limite, referencia)).thenReturn(3);
        when(solicitacaoRepository.ocultarArquivadasExpiradas(limite, referencia)).thenReturn(2);

        var resultado = service.executarRetencao(referencia);

        assertThat(resultado.concluidasArquivadas()).isEqualTo(3);
        assertThat(resultado.arquivadasOcultadas()).isEqualTo(2);
        verify(anexoRepository).removerConteudosDasConcluidasAntes(eq(limite), eq(referencia));
        verify(solicitacaoRepository).arquivarConcluidasAntes(eq(limite), eq(referencia));
        verify(solicitacaoRepository).ocultarArquivadasExpiradas(eq(limite), eq(referencia));
    }
}
