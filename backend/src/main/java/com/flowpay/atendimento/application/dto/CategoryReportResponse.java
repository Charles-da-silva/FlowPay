package com.flowpay.atendimento.application.dto;

import com.flowpay.atendimento.domain.enums.ServiceCategory;

public record CategoryReportResponse(
        ServiceCategory category,
        long totalServiceRequests,
        long waitedServiceRequests,
        long inProgressServiceRequests,
        long completedServiceRequests,
        double averageServiceSeconds
) {
}
