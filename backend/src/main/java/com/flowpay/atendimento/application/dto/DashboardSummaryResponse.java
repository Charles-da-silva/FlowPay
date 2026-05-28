package com.flowpay.atendimento.application.dto;

import java.util.List;

public record DashboardSummaryResponse(
        long totalAttendants,
        long availableAttendants,
        long busyAttendants,
        long pausedAttendants,
        long inactiveAttendants,
        long totalServiceRequests,
        long waitingServiceRequests,
        long inProgressServiceRequests,
        long completedServiceRequests,
        long todayServiceRequests,
        long todayWaitedServiceRequests,
        double todayServiceLevel,
        List<AttendantDailySummaryResponse> todayByAttendant
) {
}
