package com.dashboard.api.service;

import com.dashboard.api.model.ViagemJustificativa;
import com.dashboard.api.repository.ViagemJustificativaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViagemJustificativaServiceTest {

    @Mock
    private ViagemJustificativaRepository repository;

    @InjectMocks
    private ViagemJustificativaService service;

    @Test
    void excluirDeveUsarSoftDeleteDaEntidadeQuandoSmPossuiJustificativaAtiva() {
        ViagemJustificativa justificativa = new ViagemJustificativa();
        when(repository.findByCodSolicitacao(123L)).thenReturn(Optional.of(justificativa));

        service.excluir(123L);

        verify(repository).delete(justificativa);
    }

    @Test
    void excluirDeveSerIdempotenteQuandoSmNaoPossuiJustificativaAtiva() {
        when(repository.findByCodSolicitacao(123L)).thenReturn(Optional.empty());

        service.excluir(123L);

        verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
    }
}
