package com.flowpay.atendimento.application.dto;

import com.flowpay.atendimento.domain.enums.ServiceCategory;
import com.flowpay.atendimento.domain.enums.ServiceRequestStatus;

import java.time.Instant;

public record ServiceRequestResponse(
        Long id,
        String customerName,
        ServiceCategory category,
        ServiceRequestStatus status,
        Long attendantId,
        String attendantName,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        Instant queuedAt
) {
}
