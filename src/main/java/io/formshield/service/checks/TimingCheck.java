package io.formshield.service.checks;

import io.formshield.dto.CheckRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Layer 2: Timing analysis.
 * Humans take 10–120 seconds to fill a form.
 * Bots typically submit in under 2 seconds.
 */
@Component
public class TimingCheck {

    public record Result(double score, List<String> reasons, int timingSeconds) {}

    public Result check(CheckRequest request) {
        if (request.formLoadedAt() == null || request.formSubmittedAt() == null) {
            return new Result(0.0, List.of(), -1);
        }

        long elapsed = request.formSubmittedAt() - request.formLoadedAt();
        List<String> reasons = new ArrayList<>();
        double score = 0.0;

        if (elapsed < 0) {
            // Submitted before loaded — impossible, clear manipulation
            score = 0.3;
            reasons.add("submission_time_invalid");
        } else if (elapsed < 2) {
            score = 0.3;
            reasons.add("submission_too_fast");
        } else if (elapsed < 5) {
            score = 0.15;
            reasons.add("submission_very_fast");
        } else if (elapsed > 86400) {
            // Over 24 hours — likely a bot retrying a stored form
            score = 0.1;
            reasons.add("submission_suspiciously_delayed");
        }

        return new Result(score, reasons, (int) elapsed);
    }
}
