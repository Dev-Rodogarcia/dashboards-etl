package com.dashboard.api.dto.dimensoes;

public record DimensaoOpcaoDTO(
        String value,
        String label,
        String description
) {
    public DimensaoOpcaoDTO(String value, String label) {
        this(value, label, null);
    }
}
