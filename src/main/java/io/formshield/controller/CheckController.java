package io.formshield.controller;

import io.formshield.dto.CheckRequest;
import io.formshield.dto.CheckResponse;
import io.formshield.model.ApiKey;
import io.formshield.model.User;
import io.formshield.security.ApiKeyAuthentication;
import io.formshield.service.RateLimitService;
import io.formshield.service.SpamScorerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CheckController {

    private final SpamScorerService spamScorerService;
    private final RateLimitService rateLimitService;

    @PostMapping("/check")
    public CheckResponse check(@Valid @RequestBody CheckRequest request,
                               HttpServletRequest httpRequest) {
        ApiKeyAuthentication auth =
                (ApiKeyAuthentication) SecurityContextHolder.getContext().getAuthentication();

        User user = auth.getUser();
        ApiKey apiKey = auth.getApiKey();
        String userAgent = httpRequest.getHeader("User-Agent");

        // Enforce rate limits before scoring
        rateLimitService.checkAndIncrement(user);

        return spamScorerService.score(request, user, apiKey, userAgent);
    }
}
