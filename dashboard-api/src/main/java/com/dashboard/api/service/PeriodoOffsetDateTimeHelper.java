package com.dashboard.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Component
public class PeriodoOffsetDateTimeHelper {

    static final String DEFAULT_ZONE_ID = "America/Sao_Paulo";

    private final ZoneId zoneId;

    @Autowired
    public PeriodoOffsetDateTimeHelper(
            @Value("${dashboard.periodo.zone-id:" + DEFAULT_ZONE_ID + "}") String zoneId
    ) {
        this(ZoneId.of(zoneId));
    }

    PeriodoOffsetDateTimeHelper(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    }

    static PeriodoOffsetDateTimeHelper padrao() {
        return new PeriodoOffsetDateTimeHelper(ZoneId.of(DEFAULT_ZONE_ID));
    }

    JanelaOffsetDateTime criarJanela(LocalDate dataInicio, LocalDate dataFim) {
        Objects.requireNonNull(dataInicio, "dataInicio");
        Objects.requireNonNull(dataFim, "dataFim");

        OffsetDateTime inicioInclusivo = dataInicio.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime fimExclusivo = dataFim.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();
        return new JanelaOffsetDateTime(inicioInclusivo, fimExclusivo);
    }
}

record JanelaOffsetDateTime(
        OffsetDateTime inicioInclusivo,
        OffsetDateTime fimExclusivo
) {
    JanelaOffsetDateTime {
        Objects.requireNonNull(inicioInclusivo, "inicioInclusivo");
        Objects.requireNonNull(fimExclusivo, "fimExclusivo");
    }
}
