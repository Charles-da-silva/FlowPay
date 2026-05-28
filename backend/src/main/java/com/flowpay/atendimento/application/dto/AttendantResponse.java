package com.flowpay.atendimento.application.dto;

import com.flowpay.atendimento.domain.enums.AttendantStatus;
import com.flowpay.atendimento.domain.enums.ServiceCategory;

import java.time.Instant;
import java.util.Set;

public record AttendantResponse(
        Long id,
        String name,
        AttendantStatus status,
        Long activeServiceRequests,
        Instant availableSince,
        Instant pausedSince,
        Integer maxSimultaneousCustomers,
        Set<ServiceCategory> categories
) {
}
