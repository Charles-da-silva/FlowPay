package com.flowpay.atendimento.application.dto;

public record AttendantDailySummaryResponse(
        Long attendantId,
        String attendantName,
        long serviceRequests,
        double averageServiceSeconds,
        long pauseCount,
        double totalPauseSeconds
) {
}
