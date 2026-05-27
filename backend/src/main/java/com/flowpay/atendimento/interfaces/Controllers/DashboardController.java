package com.flowpay.atendimento.interfaces.Controllers;

import com.flowpay.atendimento.application.dto.DashboardSummaryResponse;
import com.flowpay.atendimento.application.service.DashboardService;
import com.flowpay.atendimento.application.service.DashboardSseService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardSseService dashboardSseService;

    public DashboardController(DashboardService dashboardService, DashboardSseService dashboardSseService) {
        this.dashboardService = dashboardService;
        this.dashboardSseService = dashboardSseService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> summary() {
        // Endpoint de visao executiva: capacidade de atendentes + situacao da fila.
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        // SSE: conexao aberta; servidor envia eventos quando fila/dashboard mudam.
        return dashboardSseService.subscribe();
    }
}
