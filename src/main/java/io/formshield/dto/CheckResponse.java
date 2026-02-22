package io.formshield.dto;

import java.util.List;

public record CheckResponse(
        boolean spam,
        double score,
        double threshold,
        List<String> reasons,
        String action,
        String requestId
) {}
