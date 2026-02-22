package io.formshield.service.checks;

import io.formshield.dto.CheckRequest;
import io.formshield.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Layer 3: Heuristic rules.
 * Pattern-matching on content and metadata for common spam signals.
 */
@Component
@RequiredArgsConstructor
public class HeuristicCheck {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[^\\s]+|www\\.[^\\s]+",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> SPAM_KEYWORDS = Set.of(
            "viagra", "cialis", "casino", "lottery", "winner", "prize", "free money",
            "click here", "buy now", "limited offer", "make money fast", "work from home",
            "nigerian prince", "inheritance", "wire transfer", "cryptocurrency investment",
            "bitcoin profit", "earn $", "100% free", "no risk", "guarantee",
            "enlargement", "adult", "xxx", "porn", "dating site", "hot singles"
    );

    private static final Set<String> DISPOSABLE_EMAIL_DOMAINS = Set.of(
            "mailinator.com", "guerrillamail.com", "10minutemail.com", "tempmail.com",
            "throwaway.email", "yopmail.com", "sharklasers.com", "guerrillamailblock.com",
            "grr.la", "spam4.me", "trashmail.com", "fakeinbox.com", "mailnull.com",
            "spamgourmet.com", "trashmail.me", "dispostable.com", "maildrop.cc"
    );

    public record Result(double score, List<String> reasons) {}

    public Result check(CheckRequest request, io.formshield.model.User user, SubmissionRepository submissionRepository) {
        double score = 0.0;
        List<String> reasons = new ArrayList<>();

        String message = request.message() != null ? request.message() : "";
        String email = request.email() != null ? request.email() : "";

        // Check link density
        Matcher urlMatcher = URL_PATTERN.matcher(message);
        int urlCount = 0;
        while (urlMatcher.find()) {
            urlCount++;
        }
        if (urlCount >= 3) {
            score += 0.2;
            reasons.add("excessive_links");
        } else if (urlCount >= 1) {
            score += 0.05;
            reasons.add("contains_link");
        }

        // Check spam keywords
        String messageLower = message.toLowerCase();
        for (String keyword : SPAM_KEYWORDS) {
            if (messageLower.contains(keyword)) {
                score += 0.15;
                reasons.add("keyword_match:" + keyword.replace(" ", "_"));
                break; // One hit is enough to flag, avoid stacking same category
            }
        }

        // Check disposable email
        String emailDomain = email.contains("@") ? email.substring(email.indexOf('@') + 1).toLowerCase() : "";
        if (DISPOSABLE_EMAIL_DOMAINS.contains(emailDomain)) {
            score += 0.2;
            reasons.add("disposable_email");
        }

        // Excessive caps (over 60% uppercase in a non-trivial message)
        if (message.length() > 20) {
            long upperCount = message.chars().filter(Character::isUpperCase).count();
            long letterCount = message.chars().filter(Character::isLetter).count();
            if (letterCount > 0 && (double) upperCount / letterCount > 0.6) {
                score += 0.1;
                reasons.add("excessive_caps");
            }
        }

        // Repeated IP submissions (check last hour)
        if (request.ip() != null && !request.ip().isBlank()) {
            long recentFromIp = submissionRepository.countByUserAndIpAddressAndCreatedAtAfter(
                    user, request.ip(), LocalDateTime.now().minusHours(1)
            );
            if (recentFromIp >= 10) {
                score += 0.3;
                reasons.add("repeated_ip_submissions");
            } else if (recentFromIp >= 5) {
                score += 0.15;
                reasons.add("frequent_ip_submissions");
            }
        }

        return new Result(Math.min(score, 0.5), reasons);
    }
}
