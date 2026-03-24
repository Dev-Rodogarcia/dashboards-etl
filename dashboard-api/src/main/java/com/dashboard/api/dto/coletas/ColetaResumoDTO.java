package com.dashboard.api.dto.coletas;

import java.math.BigDecimal;

public record ColetaResumoDTO(
        String id,
        Long coleta,
        String solicitacao,
        String agendamento,
        String finalizacao,
        String status,
        Integer volumes,
        BigDecimal pesoTaxado,
        BigDecimal valorNf,
        Long numeroManifesto,
        String cliente,
        String cidade,
        String uf,
        String regiaoColeta,
        String filial,
        String usuario,
        String motivoCancelamento,
        Integer numeroTentativas
) {}
