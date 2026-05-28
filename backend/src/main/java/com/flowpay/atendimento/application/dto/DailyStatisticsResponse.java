package com.flowpay.atendimento.application.dto;

import java.time.LocalDate;

public record DailyStatisticsResponse(
        LocalDate date,
        long totalServiceRequests,
        long waitedServiceRequests,
        double serviceLevel,
        double averageServiceSeconds
) {
}
