package com.flowpay.atendimento.application.dto;

import com.flowpay.atendimento.domain.enums.ServiceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateServiceRequestRequest(
        @NotBlank
        @Size(min = 2, max = 120)
        @Pattern(regexp = "^[\\p{L} ]+$", message = "deve conter apenas letras e espacos")
        String customerName,
        @NotNull ServiceCategory category
) {
}
