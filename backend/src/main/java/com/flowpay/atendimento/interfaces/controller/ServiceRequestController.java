package com.flowpay.atendimento.interfaces.controller;

import com.flowpay.atendimento.application.dto.CreateServiceRequestRequest;
import com.flowpay.atendimento.application.dto.ServiceRequestResponse;
import com.flowpay.atendimento.application.service.ServiceRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/service-requests")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    public ServiceRequestController(ServiceRequestService serviceRequestService) {
        this.serviceRequestService = serviceRequestService;
    }

    @PostMapping
    public ResponseEntity<ServiceRequestResponse> create(@Valid @RequestBody CreateServiceRequestRequest request) {
        // Tenta distribuir imediatamente; se nao houver slot, ja entra em fila.
        ServiceRequestResponse created = serviceRequestService.create(request);
        return ResponseEntity.status(201).body(created);
    }

    @PatchMapping("/{id}/finish")
    public ResponseEntity<ServiceRequestResponse> finish(@PathVariable("id") Long id) {
        // Finaliza o atendimento e dispara redistribuicao automatica da fila.
        return ResponseEntity.ok(serviceRequestService.finish(id));
    }

    @GetMapping
    public ResponseEntity<List<ServiceRequestResponse>> list() {
        return ResponseEntity.ok(serviceRequestService.list());
    }

    @GetMapping("/queue")
    public ResponseEntity<List<ServiceRequestResponse>> queue() {
        // Lista apenas os atendimentos aguardando em fila (FIFO por categoria).
        return ResponseEntity.ok(serviceRequestService.listQueue());
    }
}
