package com.dashboard.api.service;

import com.dashboard.api.model.VisaoFretesEntity;
import com.dashboard.api.repository.DimFilialRepository;
import com.dashboard.api.repository.DimUsuarioRepository;
import com.dashboard.api.repository.DimVeiculoRepository;
import com.dashboard.api.repository.VisaoColetasRepository;
import com.dashboard.api.repository.VisaoContasAPagarRepository;
import com.dashboard.api.repository.VisaoCotacoesRepository;
import com.dashboard.api.repository.VisaoFaturasClienteRepository;
import com.dashboard.api.repository.VisaoFretesRepository;
import com.dashboard.api.repository.VisaoManifestosRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DimensoesServiceTest {

    @Test
    void listarClientesDeveIgnorarCamposNulosSemGerarErro() throws Exception {
        DimFilialRepository dimFilialRepository = mock(DimFilialRepository.class);
        DimUsuarioRepository dimUsuarioRepository = mock(DimUsuarioRepository.class);
        DimVeiculoRepository dimVeiculoRepository = mock(DimVeiculoRepository.class);
        VisaoColetasRepository coletasRepository = mock(VisaoColetasRepository.class);
        VisaoFretesRepository fretesRepository = mock(VisaoFretesRepository.class);
        VisaoCotacoesRepository cotacoesRepository = mock(VisaoCotacoesRepository.class);
        VisaoFaturasClienteRepository faturasClienteRepository = mock(VisaoFaturasClienteRepository.class);
        VisaoManifestosRepository manifestosRepository = mock(VisaoManifestosRepository.class);
        VisaoContasAPagarRepository contasAPagarRepository = mock(VisaoContasAPagarRepository.class);

        EscopoFilialService escopoFilialService = new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };

        DimensoesService service = new DimensoesService(
                dimFilialRepository,
                dimUsuarioRepository,
                dimVeiculoRepository,
                coletasRepository,
                fretesRepository,
                cotacoesRepository,
                faturasClienteRepository,
                manifestosRepository,
                contasAPagarRepository,
                escopoFilialService
        );

        when(coletasRepository.findAll()).thenReturn(List.of());
        when(cotacoesRepository.findAll()).thenReturn(List.of());
        when(faturasClienteRepository.findAll()).thenReturn(List.of());
        when(fretesRepository.findAll()).thenReturn(List.of(
                criarFrete("Filial A", "Cliente Pagador", null, "Destinatario Final")
        ));

        List<String> clientes = assertDoesNotThrow(service::listarClientes);

        assertEquals(List.of("Cliente Pagador", "Destinatario Final"), clientes);
    }

    private VisaoFretesEntity criarFrete(String filialNome, String pagadorNome, String remetenteNome, String destinatarioNome) throws Exception {
        Constructor<VisaoFretesEntity> constructor = VisaoFretesEntity.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        VisaoFretesEntity entity = constructor.newInstance();
        definirCampo(entity, "id", 1L);
        definirCampo(entity, "filialNome", filialNome);
        definirCampo(entity, "pagadorNome", pagadorNome);
        definirCampo(entity, "remetenteNome", remetenteNome);
        definirCampo(entity, "destinatarioNome", destinatarioNome);
        return entity;
    }

    private void definirCampo(Object alvo, String nomeCampo, Object valor) throws Exception {
        Field field = alvo.getClass().getDeclaredField(nomeCampo);
        field.setAccessible(true);
        field.set(alvo, valor);
    }
}
