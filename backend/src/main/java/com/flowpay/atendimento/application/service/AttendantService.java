package com.flowpay.atendimento.application.service;

import com.flowpay.atendimento.application.dto.AttendantResponse;
import com.flowpay.atendimento.application.dto.CreateAttendantRequest;
import com.flowpay.atendimento.application.dto.UpdateAttendantRequest;
import com.flowpay.atendimento.domain.entity.Attendant;
import com.flowpay.atendimento.domain.entity.AttendantPause;
import com.flowpay.atendimento.domain.enums.AttendantStatus;
import com.flowpay.atendimento.domain.enums.ServiceRequestStatus;
import com.flowpay.atendimento.domain.repository.AttendantPauseRepository;
import com.flowpay.atendimento.domain.repository.AttendantRepository;
import com.flowpay.atendimento.domain.repository.ServiceRequestRepository;
import com.flowpay.atendimento.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;

@Service
public class AttendantService {

    private final AttendantRepository attendantRepository;
    private final AttendantPauseRepository attendantPauseRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final DashboardSseService dashboardSseService;

    public AttendantService(
            AttendantRepository attendantRepository,
            AttendantPauseRepository attendantPauseRepository,
            ServiceRequestRepository serviceRequestRepository,
            DashboardSseService dashboardSseService
    ) {
        this.attendantRepository = attendantRepository;
        this.attendantPauseRepository = attendantPauseRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.dashboardSseService = dashboardSseService;
    }

    @Transactional
    public AttendantResponse create(CreateAttendantRequest request) {
        Attendant attendant = Attendant.builder()
                .name(request.name())
                .status(AttendantStatus.AVAILABLE)
                .availableSince(Instant.now())
                .maxSimultaneousCustomers(3)
                .categories(request.categories())
                .build();

        Attendant saved = attendantRepository.save(attendant);
        dashboardSseService.notifyDashboardChanged();
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AttendantResponse> list() {
        return attendantRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AttendantResponse update(Long attendantId, UpdateAttendantRequest request) {
        Attendant attendant = attendantRepository.findById(attendantId)
                .orElseThrow(() -> new ResourceNotFoundException("Agente nao encontrado: " + attendantId));

        attendant.setName(request.name());
        attendant.setCategories(request.categories());

        long openRequestsCount = serviceRequestRepository.countByAttendantIdAndStatus(
                attendant.getId(),
                ServiceRequestStatus.IN_PROGRESS
        );
        refreshStatus(attendant, openRequestsCount);
        dashboardSseService.notifyDashboardChanged();
        return toResponse(attendant);
    }

    @Transactional
    public void delete(Long attendantId) {
        Attendant attendant = attendantRepository.findById(attendantId)
                .orElseThrow(() -> new ResourceNotFoundException("Agente nao encontrado: " + attendantId));

        long openRequestsCount = serviceRequestRepository.countByAttendantIdAndStatus(
                attendant.getId(),
                ServiceRequestStatus.IN_PROGRESS
        );
        if (openRequestsCount > 0) {
            throw new IllegalStateException("Nao e possivel excluir agente com atendimentos em andamento.");
        }

        serviceRequestRepository.clearInactiveAssignmentsForAttendant(attendant.getId());
        attendantRepository.delete(attendant);
        dashboardSseService.notifyDashboardChanged();
    }

    @Transactional
    public AttendantResponse pause(Long attendantId) {
        Attendant attendant = attendantRepository.findById(attendantId)
                .orElseThrow(() -> new ResourceNotFoundException("Agente nao encontrado: " + attendantId));

        long openRequestsCount = serviceRequestRepository.countByAttendantIdAndStatus(
                attendant.getId(),
                ServiceRequestStatus.IN_PROGRESS
        );
        if (attendant.getStatus() != AttendantStatus.AVAILABLE || openRequestsCount > 0) {
            throw new IllegalStateException("Pausa permitida apenas para agentes disponiveis.");
        }

        Instant now = Instant.now();
        AttendantPause pause = new AttendantPause();
        pause.setAttendant(attendant);
        pause.setStartedAt(now);
        attendantPauseRepository.save(pause);

        attendant.setStatus(AttendantStatus.PAUSED);
        attendant.setAvailableSince(null);
        attendant.setPausedSince(now);
        Attendant saved = attendantRepository.save(attendant);
        dashboardSseService.notifyDashboardChanged();
        return toResponse(saved);
    }

    @Transactional
    public AttendantResponse resume(Long attendantId) {
        Attendant attendant = attendantRepository.findById(attendantId)
                .orElseThrow(() -> new ResourceNotFoundException("Agente nao encontrado: " + attendantId));

        if (attendant.getStatus() != AttendantStatus.PAUSED) {
            throw new IllegalStateException("Somente agentes em pausa podem voltar.");
        }

        Instant now = Instant.now();
        attendantPauseRepository.findFirstByAttendantIdAndFinishedAtIsNullOrderByStartedAtDesc(attendant.getId())
                .ifPresent(pause -> {
                    pause.setFinishedAt(now);
                    attendantPauseRepository.save(pause);
                });

        attendant.setStatus(AttendantStatus.AVAILABLE);
        attendant.setAvailableSince(now);
        attendant.setPausedSince(null);
        Attendant saved = attendantRepository.save(attendant);
        dashboardSseService.notifyDashboardChanged();
        return toResponse(saved);
    }

    @Transactional
    public void refreshStatus(Attendant attendant, long openRequestsCount) {
        if (attendant.getStatus() == AttendantStatus.INACTIVE || attendant.getStatus() == AttendantStatus.PAUSED) {
            return;
        }
        AttendantStatus newStatus = openRequestsCount > 0
                ? AttendantStatus.BUSY
                : AttendantStatus.AVAILABLE;
        attendant.setStatus(newStatus);
        if (newStatus == AttendantStatus.AVAILABLE && attendant.getAvailableSince() == null) {
            attendant.setAvailableSince(Instant.now());
        }
        if (newStatus == AttendantStatus.BUSY) {
            attendant.setAvailableSince(null);
        }
        attendantRepository.save(attendant);
    }

    private AttendantResponse toResponse(Attendant attendant) {
        return new AttendantResponse(
                attendant.getId(),
                attendant.getName(),
                attendant.getStatus(),
                serviceRequestRepository.countByAttendantIdAndStatus(
                        attendant.getId(),
                        ServiceRequestStatus.IN_PROGRESS
                ),
                attendant.getAvailableSince(),
                attendant.getPausedSince(),
                attendant.getMaxSimultaneousCustomers(),
                attendant.getCategories()
        );
    }
}
