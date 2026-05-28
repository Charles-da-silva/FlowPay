package com.flowpay.atendimento.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DailyReportResponse(
        LocalDate startDate,
        LocalDate endDate,
        Instant generatedAt,
        long totalServiceRequests,
        long waitedServiceRequests,
        long inProgressServiceRequests,
        long completedServiceRequests,
        double serviceLevel,
        double averageServiceSeconds,
        List<CategoryReportResponse> categories,
        List<AttendantDailySummaryResponse> attendants
) {
}
