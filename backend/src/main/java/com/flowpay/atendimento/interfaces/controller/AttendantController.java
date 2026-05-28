package com.flowpay.atendimento.interfaces.controller;

import com.flowpay.atendimento.application.dto.AttendantResponse;
import com.flowpay.atendimento.application.dto.CreateAttendantRequest;
import com.flowpay.atendimento.application.dto.UpdateAttendantRequest;
import com.flowpay.atendimento.application.service.AttendantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/attendants")
public class AttendantController {

    private final AttendantService attendantService;

    public AttendantController(AttendantService attendantService) {
        this.attendantService = attendantService;
    }

    @PostMapping
    public ResponseEntity<AttendantResponse> create(@Valid @RequestBody CreateAttendantRequest request) {
        // Cria um novo agente ja com limite padrao de 3 atendimentos simultaneos.
        AttendantResponse created = attendantService.create(request);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping
    public ResponseEntity<List<AttendantResponse>> list() {
        return ResponseEntity.ok(attendantService.list());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttendantResponse> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAttendantRequest request
    ) {
        return ResponseEntity.ok(attendantService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        attendantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/pause")
    public ResponseEntity<AttendantResponse> pause(@PathVariable("id") Long id) {
        return ResponseEntity.ok(attendantService.pause(id));
    }

    @PatchMapping("/{id}/resume")
    public ResponseEntity<AttendantResponse> resume(@PathVariable("id") Long id) {
        return ResponseEntity.ok(attendantService.resume(id));
    }
}
