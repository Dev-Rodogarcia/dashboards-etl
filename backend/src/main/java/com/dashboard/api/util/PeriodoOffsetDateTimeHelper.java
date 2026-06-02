package com.dashboard.api.util;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PeriodoOffsetDateTimeHelper {

    public static final String DEFAULT_ZONE_ID = "America/Sao_Paulo";

    private final ZoneId zoneId;

    @Autowired
    public PeriodoOffsetDateTimeHelper(
            @Value("${dashboard.periodo.zone-id:" + DEFAULT_ZONE_ID + "}") String zoneId
    ) {
        this(ZoneId.of(zoneId));
    }

    public PeriodoOffsetDateTimeHelper(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    }

    public static PeriodoOffsetDateTimeHelper padrao() {
        return new PeriodoOffsetDateTimeHelper(ZoneId.of(DEFAULT_ZONE_ID));
    }

    public JanelaOffsetDateTime criarJanela(LocalDate dataInicio, LocalDate dataFim) {
        Objects.requireNonNull(dataInicio, "dataInicio");
        Objects.requireNonNull(dataFim, "dataFim");

        OffsetDateTime inicioInclusivo = dataInicio.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime fimExclusivo = dataFim.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();
        return new JanelaOffsetDateTime(inicioInclusivo, fimExclusivo);
    }

    public LocalDate hoje() {
        return LocalDate.now(zoneId);
    }
}
