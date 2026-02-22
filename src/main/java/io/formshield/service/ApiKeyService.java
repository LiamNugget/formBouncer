package io.formshield.service;

import io.formshield.dto.ApiKeyDto;
import io.formshield.dto.CreateApiKeyRequest;
import io.formshield.dto.CreateApiKeyResponse;
import io.formshield.model.ApiKey;
import io.formshield.model.User;
import io.formshield.repository.ApiKeyRepository;
import io.formshield.repository.UserRepository;
import io.formshield.security.ApiKeyAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public List<ApiKeyDto> listKeys(User user) {
        return apiKeyRepository.findByUser(user).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CreateApiKeyResponse createKey(User user, CreateApiKeyRequest request) {
        int activeKeys = apiKeyRepository.countByUserAndIsActiveTrue(user);
        int maxKeys = user.getPlan().getMaxApiKeys();
        if (activeKeys >= maxKeys) {
            throw new IllegalStateException(
                    "API key limit reached for your plan (%d/%d). Upgrade to create more.".formatted(activeKeys, maxKeys)
            );
        }

        // Generate a 32-byte random key, URL-safe Base64 encoded
        byte[] raw = new byte[32];
        secureRandom.nextBytes(raw);
        String rawKey = "fs_live_" + Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        String prefix = rawKey.substring(0, 16) + "...";

        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        apiKey.setKeyHash(ApiKeyAuthFilter.hashKey(rawKey));
        apiKey.setKeyPrefix(prefix);
        apiKey.setName(request.name());
        apiKey.setAllowedDomain(request.allowedDomain());
        apiKeyRepository.save(apiKey);

        return new CreateApiKeyResponse(apiKey.getId(), apiKey.getName(), rawKey, prefix, apiKey.getAllowedDomain());
    }

    @Transactional
    public void revokeKey(User user, Long keyId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new NoSuchElementException("API key not found"));

        if (!apiKey.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not your API key");
        }

        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);
    }

    private ApiKeyDto toDto(ApiKey key) {
        return new ApiKeyDto(
                key.getId(),
                key.getName(),
                key.getKeyPrefix(),
                key.getAllowedDomain(),
                key.isActive(),
                key.getLastUsedAt(),
                key.getCreatedAt()
        );
    }
}
