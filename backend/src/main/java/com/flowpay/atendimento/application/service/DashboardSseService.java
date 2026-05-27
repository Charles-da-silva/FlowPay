package com.flowpay.atendimento.application.service;

import com.flowpay.atendimento.application.dto.DashboardSummaryResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Gerencia conexoes SSE do dashboard.
 * Quando a fila muda, todos os clientes conectados recebem um novo resumo automaticamente.
 */
@Service
public class DashboardSseService {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;
    private static final String EVENT_NAME = "dashboard-update";

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final DashboardService dashboardService;

    public DashboardSseService(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Cliente abre conexao SSE e recebe um snapshot inicial + atualizacoes futuras.
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));

        // Envia estado atual assim que o cliente conecta.
        sendSummary(emitter, dashboardService.getSummary());
        return emitter;
    }

    /**
     * Chamado apos criar/finalizar atendimento ou redistribuir fila.
     */
    public void notifyDashboardChanged() {
        DashboardSummaryResponse summary = dashboardService.getSummary();
        emitters.forEach(emitter -> sendSummary(emitter, summary));
    }

    private void sendSummary(SseEmitter emitter, DashboardSummaryResponse summary) {
        try {
            emitter.send(SseEmitter.event()
                    .name(EVENT_NAME)
                    .data(summary, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException ex) {
            emitters.remove(emitter);
            try {
                emitter.complete();
            } catch (IllegalStateException ignored) {
                // The connection is already unusable; removing it is enough.
            }
        }
    }
}
