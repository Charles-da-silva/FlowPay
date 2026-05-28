package com.flowpay.atendimento.application.dto;

import java.time.LocalDate;

public record WorstServiceLevelDayResponse(
        LocalDate date,
        double serviceLevel
) {
}
