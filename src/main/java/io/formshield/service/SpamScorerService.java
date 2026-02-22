package io.formshield.service;

import io.formshield.dto.CheckRequest;
import io.formshield.dto.CheckResponse;
import io.formshield.model.ApiKey;
import io.formshield.model.Submission;
import io.formshield.model.User;
import io.formshield.repository.SubmissionRepository;
import io.formshield.service.checks.HeuristicCheck;
import io.formshield.service.checks.HoneypotCheck;
import io.formshield.service.checks.MlCheck;
import io.formshield.service.checks.TimingCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpamScorerService {

    private final HoneypotCheck honeypotCheck;
    private final TimingCheck timingCheck;
    private final HeuristicCheck heuristicCheck;
    private final MlCheck mlCheck;
    private final SubmissionRepository submissionRepository;

    @Transactional
    public CheckResponse score(CheckRequest request, User user, ApiKey apiKey, String userAgent) {
        List<String> reasons = new ArrayList<>();
        double score = 0.0;
        int timingSeconds = -1;

        // Layer 1: Honeypot (instant kill — short-circuit if filled)
        HoneypotCheck.Result honeypot = honeypotCheck.check(request);
        if (honeypot.score() >= 1.0) {
            score = 1.0;
            reasons.addAll(honeypot.reasons());
        } else {
            // Layer 2: Timing
            TimingCheck.Result timing = timingCheck.check(request);
            score += timing.score();
            reasons.addAll(timing.reasons());
            timingSeconds = timing.timingSeconds();

            // Layer 3: Heuristics
            HeuristicCheck.Result heuristic = heuristicCheck.check(request, user, submissionRepository);
            score += heuristic.score();
            reasons.addAll(heuristic.reasons());

            // Layer 4: ML (Pro+ only)
            if (user.getPlan().isMlEnabled()) {
                MlCheck.Result ml = mlCheck.check(request);
                score += ml.score();
                reasons.addAll(ml.reasons());
            }

            score = Math.min(score, 1.0);
        }

        double threshold = user.getPlan().isCustomThreshold()
                ? user.getSpamThreshold().doubleValue()
                : 0.7;

        boolean isSpam = score >= threshold;
        String requestId = "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // Log submission (no PII — score/metadata only)
        Submission submission = new Submission();
        submission.setUser(user);
        submission.setApiKey(apiKey);
        submission.setScore(BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_UP));
        submission.setSpam(isSpam);
        submission.setThresholdUsed(BigDecimal.valueOf(threshold).setScale(2, RoundingMode.HALF_UP));
        submission.setReasons(reasons);
        submission.setIpAddress(request.ip());
        submission.setUserAgent(userAgent);
        submission.setTimingSeconds(timingSeconds >= 0 ? timingSeconds : null);
        submissionRepository.save(submission);

        // Increment monthly usage counter
        user.setSubmissionsThisMonth(user.getSubmissionsThisMonth() + 1);

        return new CheckResponse(
                isSpam,
                Math.round(score * 1000.0) / 1000.0,
                threshold,
                isSpam ? reasons : List.of(),
                isSpam ? "reject" : "allow",
                requestId
        );
    }
}
