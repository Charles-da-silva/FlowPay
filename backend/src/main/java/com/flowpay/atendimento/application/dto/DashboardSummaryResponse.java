package com.flowpay.atendimento.application.dto;

public record DashboardSummaryResponse(
        long totalAttendants,
        long availableAttendants,
        long busyAttendants,
        long inactiveAttendants,
        long totalServiceRequests,
        long waitingServiceRequests,
        long inProgressServiceRequests,
        long completedServiceRequests
) {
}
