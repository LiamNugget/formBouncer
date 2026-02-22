package io.formshield.dto;

public record LoginResponse(
        String token,
        String email,
        String name,
        String plan
) {}
