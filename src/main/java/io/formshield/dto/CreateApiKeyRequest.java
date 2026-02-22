package io.formshield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApiKeyRequest(
        @NotBlank @Size(min = 1, max = 100) String name,
        String allowedDomain
) {}
