package io.formshield.controller;

import io.formshield.model.User;
import io.formshield.repository.SubmissionRepository;
import io.formshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    @GetMapping
    public Map<String, Object> stats(@AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);
        long total = submissionRepository.countByUserAndCreatedAtAfter(user, last30Days);
        long spam = submissionRepository.countByUserAndIsSpamTrueAndCreatedAtAfter(user, last30Days);
        long allowed = total - spam;

        return Map.of(
                "submissions_this_month", user.getSubmissionsThisMonth(),
                "monthly_limit", user.getPlan().getMonthlyLimit(),
                "last_30_days", Map.of(
                        "total", total,
                        "spam_caught", spam,
                        "allowed", allowed,
                        "spam_rate", total > 0 ? Math.round((double) spam / total * 1000.0) / 10.0 : 0.0
                ),
                "plan", user.getPlan().getName()
        );
    }
}
