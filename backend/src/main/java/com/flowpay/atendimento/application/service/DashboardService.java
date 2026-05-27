package com.flowpay.atendimento.application.service;

import com.flowpay.atendimento.application.dto.DashboardSummaryResponse;
import com.flowpay.atendimento.domain.enums.AttendantStatus;
import com.flowpay.atendimento.domain.enums.ServiceRequestStatus;
import com.flowpay.atendimento.domain.repository.AttendantRepository;
import com.flowpay.atendimento.domain.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final AttendantRepository attendantRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    public DashboardService(
            AttendantRepository attendantRepository,
            ServiceRequestRepository serviceRequestRepository
    ) {
        this.attendantRepository = attendantRepository;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        // Resumo simples para monitorar operacao e fila em tempo real.
        return new DashboardSummaryResponse(
                attendantRepository.count(),
                attendantRepository.countByStatus(AttendantStatus.AVAILABLE),
                attendantRepository.countByStatus(AttendantStatus.BUSY),
                attendantRepository.countByStatus(AttendantStatus.INACTIVE),
                serviceRequestRepository.count(),
                serviceRequestRepository.countByStatus(ServiceRequestStatus.WAITING),
                serviceRequestRepository.countByStatus(ServiceRequestStatus.IN_PROGRESS),
                serviceRequestRepository.countByStatus(ServiceRequestStatus.COMPLETED)
        );
    }
}
