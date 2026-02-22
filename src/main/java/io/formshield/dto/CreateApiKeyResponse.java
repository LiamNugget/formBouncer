package io.formshield.dto;

public record CreateApiKeyResponse(
        Long id,
        String name,
        String key,   // Full key — only shown once at creation
        String keyPrefix,
        String allowedDomain
) {}
