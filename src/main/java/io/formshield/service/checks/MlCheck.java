package io.formshield.service.checks;

import io.formshield.dto.CheckRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Layer 4: ML classifier (Pro+ plans only).
 * Calls the Python Flask microservice for Naive Bayes / transformer scoring.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MlCheck {

    private final RestClient.Builder restClientBuilder;

    @Value("${formshield.ml.url}")
    private String mlServiceUrl;

    @Value("${formshield.ml.enabled}")
    private boolean mlEnabled;

    public record Result(double score, List<String> reasons) {}

    public Result check(CheckRequest request) {
        if (!mlEnabled) {
            return new Result(0.0, List.of());
        }

        try {
            RestClient client = restClientBuilder.baseUrl(mlServiceUrl).build();
            String text = buildText(request);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = client.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text))
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return new Result(0.0, List.of());
            }

            double mlScore = ((Number) response.getOrDefault("score", 0.0)).doubleValue();
            String label = (String) response.getOrDefault("label", "ham");

            List<String> reasons = "spam".equals(label) ? List.of("ml_flagged") : List.of();
            return new Result(Math.min(mlScore, 0.5), reasons);

        } catch (Exception e) {
            // ML service unavailable — degrade gracefully, don't fail the check
            log.warn("ML service unavailable: {}", e.getMessage());
            return new Result(0.0, List.of());
        }
    }

    private String buildText(CheckRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.name() != null) sb.append(request.name()).append(" ");
        if (request.email() != null) sb.append(request.email()).append(" ");
        if (request.message() != null) sb.append(request.message());
        return sb.toString().trim();
    }
}
