package com.flowpay.atendimento.application.dto;

import com.flowpay.atendimento.domain.enums.ServiceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateAttendantRequest(
        @NotBlank
        @Size(min = 2, max = 120)
        @Pattern(regexp = "^[\\p{L} ]+$", message = "deve conter apenas letras e espacos")
        String name,
        @NotBlank
        @Size(min = 2, max = 20)
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "deve conter apenas letras, numeros e hifen")
        String badge,
        @NotEmpty Set<ServiceCategory> categories
) {
}
