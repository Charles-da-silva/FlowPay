package com.flowpay.atendimento.application.service;

import com.flowpay.atendimento.application.dto.AttendantResponse;
import com.flowpay.atendimento.application.dto.CreateAttendantRequest;
import com.flowpay.atendimento.application.dto.UpdateAttendantRequest;
import com.flowpay.atendimento.domain.entity.Attendant;
import com.flowpay.atendimento.domain.enums.AttendantStatus;
import com.flowpay.atendimento.domain.enums.ServiceRequestStatus;
import com.flowpay.atendimento.domain.repository.AttendantRepository;
import com.flowpay.atendimento.domain.repository.ServiceRequestRepository;
import com.flowpay.atendimento.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AttendantService {

    private final AttendantRepository attendantRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final DashboardSseService dashboardSseService;

    public AttendantService(
            AttendantRepository attendantRepository,
            ServiceRequestRepository serviceRequestRepository,
            DashboardSseService dashboardSseService
    ) {
        this.attendantRepository = attendantRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.dashboardSseService = dashboardSseService;
    }

    @Transactional
    public AttendantResponse create(CreateAttendantRequest request) {
        Attendant attendant = Attendant.builder()
                .name(request.name())
                .status(AttendantStatus.AVAILABLE)
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
                .orElseThrow(() -> new ResourceNotFoundException("Atendente nao encontrado: " + attendantId));

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
                .orElseThrow(() -> new ResourceNotFoundException("Atendente nao encontrado: " + attendantId));

        long openRequestsCount = serviceRequestRepository.countByAttendantIdAndStatus(
                attendant.getId(),
                ServiceRequestStatus.IN_PROGRESS
        );
        if (openRequestsCount > 0) {
            throw new IllegalStateException("Nao e possivel excluir atendente com atendimentos em andamento.");
        }

        serviceRequestRepository.clearInactiveAssignmentsForAttendant(attendant.getId());
        attendantRepository.delete(attendant);
        dashboardSseService.notifyDashboardChanged();
    }

    @Transactional
    public void refreshStatus(Attendant attendant, long openRequestsCount) {
        if (attendant.getStatus() == AttendantStatus.INACTIVE) {
            return;
        }
        AttendantStatus newStatus = openRequestsCount > 0
                ? AttendantStatus.BUSY
                : AttendantStatus.AVAILABLE;
        attendant.setStatus(newStatus);
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
                attendant.getMaxSimultaneousCustomers(),
                attendant.getCategories()
        );
    }
}
