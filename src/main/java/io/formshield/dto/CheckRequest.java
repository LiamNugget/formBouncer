package io.formshield.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CheckRequest(
        @NotBlank @Email String email,
        String name,
        String message,
        String ip,
        String honeypot,
        Long formLoadedAt,
        Long formSubmittedAt
) {}
