package io.formshield.repository;

import io.formshield.model.Submission;
import io.formshield.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Page<Submission> findByUser(User user, Pageable pageable);

    long countByUserAndCreatedAtAfter(User user, LocalDateTime since);

    long countByUserAndIsSpamTrueAndCreatedAtAfter(User user, LocalDateTime since);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.user = :user AND s.ipAddress = :ip AND s.createdAt > :since")
    long countByUserAndIpAddressAndCreatedAtAfter(User user, String ip, LocalDateTime since);
}
