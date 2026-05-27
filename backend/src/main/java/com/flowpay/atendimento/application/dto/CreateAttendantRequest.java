package com.flowpay.atendimento.application.dto;

import com.flowpay.atendimento.domain.enums.ServiceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record CreateAttendantRequest(
        @NotBlank String name,
        @NotEmpty Set<ServiceCategory> categories
) {
}
