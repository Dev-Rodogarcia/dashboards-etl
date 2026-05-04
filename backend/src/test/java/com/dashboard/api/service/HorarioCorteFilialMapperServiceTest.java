package com.dashboard.api.service;

import com.dashboard.api.model.DimFilialEntity;
import com.dashboard.api.repository.DimFilialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HorarioCorteFilialMapperServiceTest {

    @Mock
    private DimFilialRepository dimFilialRepository;

    private HorarioCorteFilialMapperService service;

    @BeforeEach
    void setUp() {
        service = new HorarioCorteFilialMapperService(dimFilialRepository);
    }

    @Test
    void deveMapearSiglaDaLinhaOperacaoParaNomeCanonicoDaFilial() {
        when(dimFilialRepository.findAll()).thenReturn(List.of(
                filial("SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA"),
                filial("CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA"),
                filial("TR RODOGARCIA | SPO")
        ));

        String mapeada = service.mapearFilialCanonica("SPO-CAS");

        assertThat(mapeada).isEqualTo("SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA");
    }

    @Test
    void deveManterNomeCanonicoQuandoLinhaJaVierCompleta() {
        when(dimFilialRepository.findAll()).thenReturn(List.of(
                filial("SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA")
        ));

        String mapeada = service.mapearFilialCanonica("SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA");

        assertThat(mapeada).isEqualTo("SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA");
    }

    private static DimFilialEntity filial(String nomeFilial) {
        DimFilialEntity entity = novaInstancia(DimFilialEntity.class);
        definirCampo(entity, "nomeFilial", nomeFilial);
        return entity;
    }

    private static <T> T novaInstancia(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Nao foi possivel instanciar " + type.getSimpleName(), ex);
        }
    }

    private static void definirCampo(Object target, String nomeCampo, Object valor) {
        try {
            Field field = target.getClass().getDeclaredField(nomeCampo);
            field.setAccessible(true);
            field.set(target, valor);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Nao foi possivel definir o campo " + nomeCampo + ".", ex);
        }
    }
}
