package com.dashboard.api.util;

import java.time.OffsetDateTime;
import java.util.Objects;

public record JanelaOffsetDateTime(
        OffsetDateTime inicioInclusivo,
        OffsetDateTime fimExclusivo
) {
    public JanelaOffsetDateTime {
        Objects.requireNonNull(inicioInclusivo, "inicioInclusivo");
        Objects.requireNonNull(fimExclusivo, "fimExclusivo");
    }
}
