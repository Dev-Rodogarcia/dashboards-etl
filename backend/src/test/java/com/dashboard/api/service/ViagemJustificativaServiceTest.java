package com.dashboard.api.service;

import com.dashboard.api.repository.ViagemJustificativaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViagemJustificativaServiceTest {

    @Mock
    private ViagemJustificativaRepository repository;

    @InjectMocks
    private ViagemJustificativaService service;

    @Test
    void excluirDeveSerIdempotenteQuandoSmNaoPossuiJustificativa() {
        when(repository.deleteByCodSolicitacao(123L)).thenReturn(0);

        service.excluir(123L);

        verify(repository).deleteByCodSolicitacao(123L);
    }
}
