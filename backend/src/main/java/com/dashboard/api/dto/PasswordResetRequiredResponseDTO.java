package com.dashboard.api.dto;

public record PasswordResetRequiredResponseDTO(
        String status,
        String userId
) {
    public static final String STATUS_PASSWORD_RESET_REQUIRED = "PASSWORD_RESET_REQUIRED";

    public PasswordResetRequiredResponseDTO(Long userId) {
        this(STATUS_PASSWORD_RESET_REQUIRED, String.valueOf(userId));
    }
}
