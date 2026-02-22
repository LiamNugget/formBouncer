package io.formshield.controller;

import io.formshield.model.Submission;
import io.formshield.model.User;
import io.formshield.repository.SubmissionRepository;
import io.formshield.repository.UserRepository;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    @PostMapping("/{id}")
    public Map<String, String> report(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!user.getPlan().isFalsePositiveReporting()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Reporting requires Pro or Business plan");
        }

        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Submission not found"));

        if (!submission.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your submission");
        }

        String type = body.getOrDefault("type", "");
        if (!type.equals("false_positive") && !type.equals("false_negative")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "type must be 'false_positive' or 'false_negative'");
        }

        submission.setReportedAs(type);
        submissionRepository.save(submission);

        return Map.of("status", "reported", "type", type);
    }
}
