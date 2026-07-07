package com.dashboard.api.controller;

import com.dashboard.api.dto.FiltroConsultaDTO;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import static org.assertj.core.api.Assertions.assertThat;

class FiltroRequestMapperTest {

    @Test
    void deveConsolidarParceirosLogisticosNoFiltroDeFiliais() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("f.filiais", "SPO");
        params.add("f.filiais", "GLOBAL");
        params.add("f.parceirosLogisticos", "SPO | Parceiro A");
        params.add("f.parceirosLogisticos", "SPO | Parceiro A");
        params.add("f.status", "Finalizada");

        FiltroConsultaDTO filtro = FiltroRequestMapper.from(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 7),
                params
        );

        assertThat(filtro.valores("filiais")).containsExactly("SPO", "SPO | Parceiro A");
        assertThat(filtro.valores("parceirosLogisticos")).isEmpty();
        assertThat(filtro.valores("status")).containsExactly("Finalizada");
    }
}
