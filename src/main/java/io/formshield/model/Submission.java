package io.formshield.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "submissions")
@Getter
@Setter
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id", nullable = false)
    private ApiKey apiKey;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal score;

    @Column(nullable = false)
    private boolean isSpam;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal thresholdUsed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private List<String> reasons;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 512)
    private String userAgent;

    private Integer timingSeconds;

    // "false_positive" | "false_negative" | null
    @Column(length = 20)
    private String reportedAs;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
