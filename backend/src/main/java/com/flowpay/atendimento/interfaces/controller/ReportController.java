package com.flowpay.atendimento.interfaces.controller;

import com.flowpay.atendimento.application.dto.DailyReportResponse;
import com.flowpay.atendimento.application.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/daily")
    public ResponseEntity<DailyReportResponse> daily(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        LocalDate resolvedStartDate = startDate != null ? startDate : date != null ? date : LocalDate.now();
        LocalDate resolvedEndDate = endDate != null ? endDate : resolvedStartDate;
        return ResponseEntity.ok(reportService.getPeriodReport(resolvedStartDate, resolvedEndDate));
    }

    @GetMapping(value = "/daily.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> dailyCsv(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        LocalDate resolvedStartDate = startDate != null ? startDate : date != null ? date : LocalDate.now();
        LocalDate resolvedEndDate = endDate != null ? endDate : resolvedStartDate;
        String filename = "flowpay-relatorio-" + resolvedStartDate + "_" + resolvedEndDate + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                .header(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8")
                .body(reportService.getPeriodReportCsv(resolvedStartDate, resolvedEndDate));
    }
}
