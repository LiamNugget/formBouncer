package io.formshield.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "plans")
@Getter
@Setter
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int monthlyLimit;

    @Column(nullable = false)
    private int requestsPerMinute;

    @Column(nullable = false)
    private int requestsPerDay;

    @Column(nullable = false)
    private int maxApiKeys;

    @Column(nullable = false)
    private boolean mlEnabled;

    @Column(nullable = false)
    private boolean customThreshold;

    @Column(nullable = false)
    private boolean webhookEnabled;

    @Column(nullable = false)
    private boolean falsePositiveReporting;

    @Column(nullable = false)
    private int priceMonthlyPence;

    @Column(nullable = false)
    private int priceYearlyPence;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
