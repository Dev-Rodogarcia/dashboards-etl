package com.dashboard.api.service;

import com.dashboard.api.model.acesso.HomeSolicitacaoMelhoriaEntity;
import com.dashboard.api.repository.acesso.HomeSolicitacaoMelhoriaRepository;
import com.dashboard.api.repository.acesso.HomeSolicitacaoMelhoriaAnexoRepository;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class HomeSolicitacaoMelhoriaServiceTest {

    @Mock
    private HomeSolicitacaoMelhoriaRepository repository;

    @Mock
    private HomeSolicitacaoMelhoriaAnexoRepository anexoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private HomeSolicitacaoMelhoriaService service;

    @Test
    void deveListarSomenteSolicitacoesAtivasDoUsuarioAutenticadoIncluindoConcluidas() {
        HomeSolicitacaoMelhoriaEntity aberta = solicitacao("ABERTA", "Automatizar conferência");
        HomeSolicitacaoMelhoriaEntity concluida = solicitacao("CONCLUIDA", "Novo relatório");
        when(repository.listarAtivasDoSolicitanteOrdenadas("lucas@rodogarcia.com.br"))
                .thenReturn(List.of(aberta, concluida));

        var resultado = service.listarAtivasDoSolicitante(" Lucas@RodoGarcia.com.br ");

        assertThat(resultado)
                .extracting(item -> item.status())
                .containsExactly("ABERTA", "CONCLUIDA");
        assertThat(resultado)
                .extracting(item -> item.solicitanteEmail())
                .containsOnly("lucas@rodogarcia.com.br");
        verify(repository).listarAtivasDoSolicitanteOrdenadas("lucas@rodogarcia.com.br");
    }

    @Test
    void deveArquivarSolicitacaoEPurgarConteudoDosAnexosImediatamente() {
        HomeSolicitacaoMelhoriaEntity solicitacao = solicitacao("ABERTA", "Automatizar conferência");
        when(repository.findById(21L)).thenReturn(java.util.Optional.of(solicitacao));

        service.arquivar(21L, "lucas@rodogarcia.com.br");

        assertThat(solicitacao.isAtivo()).isFalse();
        verify(repository).save(solicitacao);
        verify(anexoRepository).removerConteudosDaSolicitacao(eq(21L), any(Instant.class));
    }

    private HomeSolicitacaoMelhoriaEntity solicitacao(String status, String titulo) {
        HomeSolicitacaoMelhoriaEntity entity = new HomeSolicitacaoMelhoriaEntity();
        entity.setTipo("MELHORIA");
        entity.setTitulo(titulo);
        entity.setDescricao("Descrição da solicitação");
        entity.setStatus(status);
        entity.setAtivo(true);
        entity.setSolicitanteNome("Lucas Andrade");
        entity.setSolicitanteEmail("lucas@rodogarcia.com.br");
        return entity;
    }
}
