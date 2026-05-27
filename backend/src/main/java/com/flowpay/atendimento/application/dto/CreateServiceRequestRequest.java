package com.flowpay.atendimento.application.dto;

import com.flowpay.atendimento.domain.enums.ServiceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateServiceRequestRequest(
        @NotBlank String customerName,
        @NotNull ServiceCategory category
) {
}
