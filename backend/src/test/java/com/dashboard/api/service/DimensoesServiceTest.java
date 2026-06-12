package com.dashboard.api.service;

import com.dashboard.api.repository.DimFilialRepository;
import com.dashboard.api.repository.DimMotoristaRepository;
import com.dashboard.api.repository.DimUsuarioRepository;
import com.dashboard.api.repository.DimVeiculoRepository;
import com.dashboard.api.repository.VisaoColetasRepository;
import com.dashboard.api.repository.VisaoContasAPagarRepository;
import com.dashboard.api.repository.VisaoCotacoesRepository;
import com.dashboard.api.repository.VisaoFaturasClienteRepository;
import com.dashboard.api.repository.VisaoFretesRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DimensoesServiceTest {

    @Test
    void listarClientesDeveIgnorarCamposNulosSemGerarErro() throws Exception {
        DimFilialRepository dimFilialRepository = mock(DimFilialRepository.class);
        DimMotoristaRepository dimMotoristaRepository = mock(DimMotoristaRepository.class);
        DimUsuarioRepository dimUsuarioRepository = mock(DimUsuarioRepository.class);
        DimVeiculoRepository dimVeiculoRepository = mock(DimVeiculoRepository.class);
        VisaoColetasRepository coletasRepository = mock(VisaoColetasRepository.class);
        VisaoFretesRepository fretesRepository = mock(VisaoFretesRepository.class);
        VisaoCotacoesRepository cotacoesRepository = mock(VisaoCotacoesRepository.class);
        VisaoFaturasClienteRepository faturasClienteRepository = mock(VisaoFaturasClienteRepository.class);
        VisaoContasAPagarRepository contasAPagarRepository = mock(VisaoContasAPagarRepository.class);

        EscopoFilialService escopoFilialService = new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };

        DimensoesService service = new DimensoesService(
                dimFilialRepository,
                dimMotoristaRepository,
                dimUsuarioRepository,
                dimVeiculoRepository,
                coletasRepository,
                fretesRepository,
                cotacoesRepository,
                faturasClienteRepository,
                contasAPagarRepository,
                escopoFilialService
        );

        when(coletasRepository.findDistinctClientes()).thenReturn(List.of());
        when(cotacoesRepository.findDistinctClientes()).thenReturn(List.of());
        when(faturasClienteRepository.findDistinctClientes()).thenReturn(List.of());
        when(fretesRepository.findDistinctClientes()).thenReturn(Arrays.asList(
                "Cliente Pagador", null, "Destinatario Final", " "
        ));

        List<String> clientes = assertDoesNotThrow(service::listarClientes);

        assertEquals(List.of("Cliente Pagador", "Destinatario Final"), clientes);
    }
}
