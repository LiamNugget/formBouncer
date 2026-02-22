package io.formshield.service.checks;

import io.formshield.dto.CheckRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Layer 1: Honeypot check.
 * If the honeypot field has any content, instant spam score of 1.0.
 */
@Component
public class HoneypotCheck {

    public record Result(double score, List<String> reasons) {}

    public Result check(CheckRequest request) {
        if (request.honeypot() != null && !request.honeypot().isBlank()) {
            return new Result(1.0, List.of("honeypot_filled"));
        }
        return new Result(0.0, List.of());
    }
}
