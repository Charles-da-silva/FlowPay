package com.flowpay.atendimento.application.service;

import com.flowpay.atendimento.application.dto.AttendantResponse;
import com.flowpay.atendimento.application.dto.CreateAttendantRequest;
import com.flowpay.atendimento.application.dto.UpdateAttendantRequest;
import com.flowpay.atendimento.domain.entity.Attendant;
import com.flowpay.atendimento.domain.entity.AttendantPause;
import com.flowpay.atendimento.domain.entity.ServiceRequest;
import com.flowpay.atendimento.domain.enums.AttendantStatus;
import com.flowpay.atendimento.domain.enums.ServiceCategory;
import com.flowpay.atendimento.domain.enums.ServiceRequestStatus;
import com.flowpay.atendimento.domain.repository.AttendantPauseRepository;
import com.flowpay.atendimento.domain.repository.AttendantRepository;
import com.flowpay.atendimento.domain.repository.ServiceRequestRepository;
import com.flowpay.atendimento.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;
import java.util.Locale;

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
        String badge = normalizeBadge(request.badge());
        Attendant existing = attendantRepository.findByBadgeIgnoreCase(badge).orElse(null);
        if (existing != null) {
            if (existing.getStatus() != AttendantStatus.INACTIVE) {
                throw new IllegalStateException("Badge ja esta em uso por agente logado.");
            }

            existing.setName(request.name());
            existing.setBadge(badge);
            existing.setStatus(AttendantStatus.AVAILABLE);
            existing.setAvailableSince(Instant.now());
            existing.setPausedSince(null);
            existing.setCategories(request.categories());
            Attendant saved = attendantRepository.save(existing);
            redistributeWaitingRequestsFor(saved);
            dashboardSseService.notifyDashboardChanged();
            return toResponse(saved);
        }

        Attendant attendant = Attendant.builder()
                .name(request.name())
                .badge(badge)
                .status(AttendantStatus.AVAILABLE)
                .availableSince(Instant.now())
                .maxSimultaneousCustomers(3)
                .categories(request.categories())
                .build();

        Attendant saved = attendantRepository.save(attendant);
        redistributeWaitingRequestsFor(saved);
        dashboardSseService.notifyDashboardChanged();
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AttendantResponse> list() {
        return attendantRepository.findLogged()
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
        redistributeWaitingRequestsFor(attendant);
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

        attendant.setStatus(AttendantStatus.INACTIVE);
        attendant.setAvailableSince(null);
        attendant.setPausedSince(null);
        attendantRepository.save(attendant);
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
        redistributeWaitingRequestsFor(saved);
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
        redistributeWaitingRequestsFor(saved);
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
                attendant.getBadge(),
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

    private void redistributeWaitingRequestsFor(Attendant attendant) {
        if (attendant.getStatus() == AttendantStatus.PAUSED || attendant.getStatus() == AttendantStatus.INACTIVE) {
            return;
        }

        long openRequestsCount = serviceRequestRepository.countByAttendantIdAndStatus(
                attendant.getId(),
                ServiceRequestStatus.IN_PROGRESS
        );

        boolean assigned;
        do {
            assigned = false;
            if (openRequestsCount >= attendant.getMaxSimultaneousCustomers()) {
                break;
            }

            for (ServiceCategory category : attendant.getCategories()) {
                if (openRequestsCount >= attendant.getMaxSimultaneousCustomers()) {
                    break;
                }

                ServiceRequest next = serviceRequestRepository
                        .findFirstByStatusAndCategoryOrderByCreatedAtAsc(ServiceRequestStatus.WAITING, category)
                        .orElse(null);
                if (next == null) {
                    continue;
                }

                next.setAttendant(attendant);
                next.setStatus(ServiceRequestStatus.IN_PROGRESS);
                next.setStartedAt(Instant.now());
                serviceRequestRepository.save(next);
                openRequestsCount++;
                assigned = true;
            }
        } while (assigned);

        refreshStatus(attendant, openRequestsCount);
    }

    private String normalizeBadge(String badge) {
        return badge.trim().toUpperCase(Locale.ROOT);
    }
}
