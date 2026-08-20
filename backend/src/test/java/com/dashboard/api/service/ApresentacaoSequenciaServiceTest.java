package com.dashboard.api.service;

import com.dashboard.api.dto.apresentacao.ApresentacaoSequenciaRequestDTO;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.ApresentacaoSequenciaRepository;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import com.dashboard.api.service.acesso.PermissaoResolverService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApresentacaoSequenciaServiceTest {
    @Mock private ApresentacaoSequenciaRepository sequenciaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PermissaoResolverService permissaoResolver;

    @Test
    void impedeSalvarPaginaRevogadaPorOverrideIndividual() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(10L);
        when(usuarioRepository.findByEmailIgnoreCaseAndAtivoTrue("usuario@empresa.com")).thenReturn(Optional.of(usuario));
        when(permissaoResolver.permissoesEfetivas(usuario)).thenReturn(Map.of(
                "fretes", true, "cotacoes", false, "coletas", true, "performance", true,
                "manifestos", true, "indicadoresGestaoAVista", true));
        ApresentacaoSequenciaService service = new ApresentacaoSequenciaService(sequenciaRepository, usuarioRepository, permissaoResolver);

        assertThatThrownBy(() -> service.criar(
                new ApresentacaoSequenciaRequestDTO("Diretoria", java.util.List.of("faturamento", "cotacoes")),
                "usuario@empresa.com"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
