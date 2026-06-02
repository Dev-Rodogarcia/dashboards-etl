package com.dashboard.api.service;

import com.dashboard.api.repository.DimFilialRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.dao.QueryTimeoutException;
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
        when(dimFilialRepository.findDistinctNomes()).thenReturn(List.of(
                "SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
                "CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
                "TR RODOGARCIA | SPO"
        ));

        String mapeada = service.mapearFilialCanonica("SPO-CAS");

        assertThat(mapeada).isEqualTo("SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA");
    }

    @Test
    void deveManterNomeCanonicoQuandoLinhaJaVierCompleta() {
        when(dimFilialRepository.findDistinctNomes()).thenReturn(List.of(
                "SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA"
        ));

        String mapeada = service.mapearFilialCanonica("SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA");

        assertThat(mapeada).isEqualTo("SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA");
    }

    @Test
    void deveUsarAliasesRasterQuandoDimensaoFiliaisFalhar() {
        when(dimFilialRepository.findDistinctNomes()).thenThrow(new QueryTimeoutException("timeout vw_dim_filiais"));

        String mapeada = service.mapearFilialCanonica("SPO-CAS");

        assertThat(mapeada).isEqualTo("SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA");
    }
}
