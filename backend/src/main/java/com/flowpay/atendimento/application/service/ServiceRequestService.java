package com.flowpay.atendimento.application.service;

import com.flowpay.atendimento.application.dto.CreateServiceRequestRequest;
import com.flowpay.atendimento.application.dto.ServiceRequestResponse;
import com.flowpay.atendimento.application.mapper.ServiceRequestMapper;
import com.flowpay.atendimento.domain.entity.Attendant;
import com.flowpay.atendimento.domain.entity.ServiceRequest;
import com.flowpay.atendimento.domain.enums.ServiceCategory;
import com.flowpay.atendimento.domain.enums.ServiceRequestStatus;
import com.flowpay.atendimento.domain.repository.AttendantRepository;
import com.flowpay.atendimento.domain.repository.ServiceRequestRepository;
import com.flowpay.atendimento.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ServiceRequestService {

    private final AttendantRepository attendantRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final AttendantService attendantService;
    private final ServiceRequestMapper serviceRequestMapper;
    private final DashboardSseService dashboardSseService;

    public ServiceRequestService(
            AttendantRepository attendantRepository,
            ServiceRequestRepository serviceRequestRepository,
            AttendantService attendantService,
            ServiceRequestMapper serviceRequestMapper,
            DashboardSseService dashboardSseService
    ) {
        this.attendantRepository = attendantRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.attendantService = attendantService;
        this.serviceRequestMapper = serviceRequestMapper;
        this.dashboardSseService = dashboardSseService;
    }

    @Transactional
    public ServiceRequestResponse create(CreateServiceRequestRequest request) {
        ServiceRequest serviceRequest = ServiceRequest.builder()
                .customerName(request.customerName())
                .category(request.category())
                .createdAt(Instant.now())
                .build();

        // Regra principal: tenta alocar imediatamente; sem slot, entra em fila.
        Optional<Attendant> eligible = findEligibleAttendant(request.category());
        ServiceRequest saved;
        if (eligible.isPresent()) {
            Attendant attendant = eligible.get();
            assignToAttendant(serviceRequest, attendant);
            saved = serviceRequestRepository.save(serviceRequest);
            refreshAttendantStatus(attendant);
        } else {
            serviceRequest.setStatus(ServiceRequestStatus.WAITING);
            serviceRequest.setAttendant(null);
            saved = serviceRequestRepository.save(serviceRequest);
        }

        notifyDashboardUpdate();
        return serviceRequestMapper.toResponse(saved);
    }

    @Transactional
    public ServiceRequestResponse finish(Long serviceRequestId) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Atendimento nao encontrado: " + serviceRequestId));

        if (serviceRequest.getStatus() == ServiceRequestStatus.COMPLETED) {
            return serviceRequestMapper.toResponse(serviceRequest);
        }

        Attendant previousAttendant = serviceRequest.getAttendant();

        serviceRequest.setStatus(ServiceRequestStatus.COMPLETED);
        serviceRequest.setFinishedAt(Instant.now());
        serviceRequestRepository.save(serviceRequest);

        if (previousAttendant != null) {
            long openCount = serviceRequestRepository.countByAttendantIdAndStatus(
                    previousAttendant.getId(),
                    ServiceRequestStatus.IN_PROGRESS
            );
            attendantService.refreshStatus(previousAttendant, openCount);
        }

        // Redistribuicao automatica: sempre que libera slot, tenta puxar da fila.
        redistributeQueue(serviceRequest.getCategory());
        notifyDashboardUpdate();
        return serviceRequestMapper.toResponse(serviceRequest);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> list() {
        return serviceRequestRepository.findAll()
                .stream()
                .map(serviceRequestMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> listQueue() {
        return serviceRequestRepository.findByStatusOrderByCreatedAtAsc(ServiceRequestStatus.WAITING)
                .stream()
                .map(serviceRequestMapper::toResponse)
                .toList();
    }

    private Optional<Attendant> findEligibleAttendant(ServiceCategory category) {
        return attendantRepository.findEligible(category).stream().findFirst();
    }

    private void assignToAttendant(ServiceRequest serviceRequest, Attendant attendant) {
        serviceRequest.setAttendant(attendant);
        serviceRequest.setStatus(ServiceRequestStatus.IN_PROGRESS);
        serviceRequest.setStartedAt(Instant.now());
    }

    // Mantem o status do atendente coerente com a quantidade real de atendimentos em aberto.
    private void refreshAttendantStatus(Attendant attendant) {
        long openCount = serviceRequestRepository.countByAttendantIdAndStatus(
                attendant.getId(),
                ServiceRequestStatus.IN_PROGRESS
        );
        attendantService.refreshStatus(attendant, openCount);
    }

    private void redistributeQueue(ServiceCategory category) {
        Optional<Attendant> eligible = findEligibleAttendant(category);
        if (eligible.isEmpty()) {
            return;
        }

        Optional<ServiceRequest> nextInQueue = serviceRequestRepository.findFirstByStatusAndCategoryOrderByCreatedAtAsc(
                ServiceRequestStatus.WAITING,
                category
        );
        if (nextInQueue.isEmpty()) {
            return;
        }

        ServiceRequest next = nextInQueue.get();
        Attendant attendant = eligible.get();
        assignToAttendant(next, attendant);
        serviceRequestRepository.save(next);
        refreshAttendantStatus(attendant);

        // Mantem FIFO enquanto houver slot + fila para a categoria.
        redistributeQueue(category);
    }

    // Dispara evento SSE para todos os dashboards conectados.
    private void notifyDashboardUpdate() {
        dashboardSseService.notifyDashboardChanged();
    }
}
