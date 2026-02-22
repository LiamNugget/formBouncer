package io.formshield.security;

import io.formshield.model.ApiKey;
import io.formshield.model.User;
import io.formshield.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String rawKey = request.getHeader("X-API-Key");
        if (rawKey == null || rawKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String hash = hashKey(rawKey);
        Optional<ApiKey> maybeKey = apiKeyRepository.findByKeyHashAndIsActiveTrue(hash);

        if (maybeKey.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        ApiKey apiKey = maybeKey.get();

        // Optional: enforce domain restriction
        String origin = request.getHeader("Origin");
        if (apiKey.getAllowedDomain() != null && origin != null) {
            if (!origin.contains(apiKey.getAllowedDomain())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Domain not allowed for this API key");
                return;
            }
        }

        // Update last used (fire and forget — no need for a transaction here)
        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);

        User user = apiKey.getUser();
        ApiKeyAuthentication auth = new ApiKeyAuthentication(user, apiKey,
                List.of(new SimpleGrantedAuthority("ROLE_API")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    public static String hashKey(String rawKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawKey.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
