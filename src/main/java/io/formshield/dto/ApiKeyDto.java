package io.formshield.dto;

import java.time.LocalDateTime;

public record ApiKeyDto(
        Long id,
        String name,
        String keyPrefix,
        String allowedDomain,
        boolean isActive,
        LocalDateTime lastUsedAt,
        LocalDateTime createdAt
) {}
