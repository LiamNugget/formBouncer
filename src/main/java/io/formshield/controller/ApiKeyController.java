package io.formshield.controller;

import io.formshield.dto.ApiKeyDto;
import io.formshield.dto.CreateApiKeyRequest;
import io.formshield.dto.CreateApiKeyResponse;
import io.formshield.model.User;
import io.formshield.repository.UserRepository;
import io.formshield.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final UserRepository userRepository;

    @GetMapping
    public List<ApiKeyDto> list(@AuthenticationPrincipal UserDetails principal) {
        return apiKeyService.listKeys(resolveUser(principal));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateApiKeyResponse create(@AuthenticationPrincipal UserDetails principal,
                                       @Valid @RequestBody CreateApiKeyRequest request) {
        return apiKeyService.createKey(resolveUser(principal), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        apiKeyService.revokeKey(resolveUser(principal), id);
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
