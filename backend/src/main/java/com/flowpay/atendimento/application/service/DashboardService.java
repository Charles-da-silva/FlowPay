package com.flowpay.atendimento.application.service;

import com.flowpay.atendimento.application.dto.AttendantDailySummaryResponse;
import com.flowpay.atendimento.application.dto.DashboardSummaryResponse;
import com.flowpay.atendimento.domain.entity.Attendant;
import com.flowpay.atendimento.domain.enums.AttendantStatus;
import com.flowpay.atendimento.domain.enums.ServiceRequestStatus;
import com.flowpay.atendimento.domain.repository.AttendantPauseRepository;
import com.flowpay.atendimento.domain.repository.AttendantRepository;
import com.flowpay.atendimento.domain.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class DashboardService {

    private final AttendantRepository attendantRepository;
    private final AttendantPauseRepository attendantPauseRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    public DashboardService(
            AttendantRepository attendantRepository,
            AttendantPauseRepository attendantPauseRepository,
            ServiceRequestRepository serviceRequestRepository
    ) {
        this.attendantRepository = attendantRepository;
        this.attendantPauseRepository = attendantPauseRepository;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        long todayServiceRequests = serviceRequestRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        long todayWaitedServiceRequests = serviceRequestRepository.countByQueuedAtBetween(startOfDay, endOfDay);
        double todayServiceLevel = todayServiceRequests == 0
                ? 100.0
                : ((todayServiceRequests - todayWaitedServiceRequests) * 100.0) / todayServiceRequests;
        List<AttendantDailySummaryResponse> todayByAttendant = attendantRepository.findAll()
                .stream()
                .map(attendant -> toDailySummary(attendant, startOfDay, endOfDay))
                .toList();

        // Resumo simples para monitorar operacao e fila em tempo real.
        return new DashboardSummaryResponse(
                attendantRepository.countLogged(),
                attendantRepository.countByStatus(AttendantStatus.AVAILABLE),
                attendantRepository.countByStatus(AttendantStatus.BUSY),
                attendantRepository.countByStatus(AttendantStatus.PAUSED),
                attendantRepository.countByStatus(AttendantStatus.INACTIVE),
                serviceRequestRepository.count(),
                serviceRequestRepository.countByStatus(ServiceRequestStatus.WAITING),
                serviceRequestRepository.countByStatus(ServiceRequestStatus.IN_PROGRESS),
                serviceRequestRepository.countByStatus(ServiceRequestStatus.COMPLETED),
                todayServiceRequests,
                todayWaitedServiceRequests,
                todayServiceLevel,
                todayByAttendant
        );
    }

    private AttendantDailySummaryResponse toDailySummary(Attendant attendant, Instant startOfDay, Instant endOfDay) {
        return new AttendantDailySummaryResponse(
                attendant.getId(),
                attendant.getName(),
                attendant.getBadge(),
                serviceRequestRepository.countByAttendantIdAndCreatedAtBetween(attendant.getId(), startOfDay, endOfDay),
                serviceRequestRepository.averageCompletedServiceSecondsByAttendantAndCreatedAtBetween(
                        attendant.getId(),
                        startOfDay,
                        endOfDay
                ),
                attendantPauseRepository.countByAttendantIdAndStartedAtBetween(attendant.getId(), startOfDay, endOfDay),
                attendantPauseRepository.sumPauseSecondsByAttendantAndStartedAtBetween(
                        attendant.getId(),
                        startOfDay,
                        endOfDay
                )
        );
    }
}
