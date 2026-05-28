package com.flowpay.atendimento.application.service;

import com.flowpay.atendimento.application.dto.CreateServiceRequestRequest;
import com.flowpay.atendimento.application.mapper.ServiceRequestMapper;
import com.flowpay.atendimento.domain.entity.Attendant;
import com.flowpay.atendimento.domain.entity.ServiceRequest;
import com.flowpay.atendimento.domain.enums.AttendantStatus;
import com.flowpay.atendimento.domain.enums.ServiceCategory;
import com.flowpay.atendimento.domain.enums.ServiceRequestStatus;
import com.flowpay.atendimento.domain.repository.AttendantPauseRepository;
import com.flowpay.atendimento.domain.repository.AttendantRepository;
import com.flowpay.atendimento.domain.repository.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceRequestServiceTest {

    private AttendantRepository attendantRepository;
    private ServiceRequestRepository serviceRequestRepository;
    private AttendantService attendantService;
    private ServiceRequestService service;

    @BeforeEach
    void setUp() {
        attendantRepository = mock(AttendantRepository.class);
        serviceRequestRepository = mock(ServiceRequestRepository.class);
        AttendantPauseRepository attendantPauseRepository = mock(AttendantPauseRepository.class);
        ServiceRequestMapper mapper = new ServiceRequestMapper();
        DashboardSseService dashboardSseService = new NoOpDashboardSseService();
        attendantService = new AttendantService(
                attendantRepository,
                attendantPauseRepository,
                serviceRequestRepository,
                dashboardSseService
        );

        service = new ServiceRequestService(
                attendantRepository,
                serviceRequestRepository,
                attendantService,
                mapper,
                dashboardSseService
        );

        when(serviceRequestRepository.save(any(ServiceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createAssignsRequestToEligibleAttendantWithLongestAvailableTime() {
        Attendant newerAvailable = attendant(1L, "Ana", Instant.parse("2026-05-28T12:00:00Z"));
        Attendant longestAvailable = attendant(2L, "Bruno", Instant.parse("2026-05-28T10:00:00Z"));

        when(attendantRepository.findEligible(ServiceCategory.CARD_ISSUES))
                .thenReturn(List.of(newerAvailable, longestAvailable));
        when(serviceRequestRepository.countByAttendantIdAndStatus(anyLong(), eq(ServiceRequestStatus.IN_PROGRESS)))
                .thenReturn(0L);

        service.create(new CreateServiceRequestRequest("Maria Silva", ServiceCategory.CARD_ISSUES));

        ArgumentCaptor<ServiceRequest> captor = ArgumentCaptor.forClass(ServiceRequest.class);
        verify(serviceRequestRepository).save(captor.capture());

        ServiceRequest saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ServiceRequestStatus.IN_PROGRESS);
        assertThat(saved.getAttendant()).isEqualTo(longestAvailable);
        assertThat(saved.getStartedAt()).isNotNull();
        assertThat(saved.getQueuedAt()).isNull();
    }

    @Test
    void createQueuesRequestWhenNoEligibleAttendantHasCapacity() {
        when(attendantRepository.findEligible(ServiceCategory.LOAN_CONTRACTING))
                .thenReturn(List.of());

        service.create(new CreateServiceRequestRequest("Carlos Lima", ServiceCategory.LOAN_CONTRACTING));

        ArgumentCaptor<ServiceRequest> captor = ArgumentCaptor.forClass(ServiceRequest.class);
        verify(serviceRequestRepository).save(captor.capture());

        ServiceRequest saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ServiceRequestStatus.WAITING);
        assertThat(saved.getAttendant()).isNull();
        assertThat(saved.getQueuedAt()).isNotNull();
        assertThat(saved.getStartedAt()).isNull();
    }

    @Test
    void finishCompletesRequestAndRedistributesOldestQueuedRequestFromSameCategory() {
        Attendant attendant = attendant(1L, "Ana", Instant.parse("2026-05-28T10:00:00Z"));
        ServiceRequest current = serviceRequest(10L, ServiceCategory.OTHER_SUBJECTS, ServiceRequestStatus.IN_PROGRESS);
        current.setAttendant(attendant);
        ServiceRequest queued = serviceRequest(11L, ServiceCategory.OTHER_SUBJECTS, ServiceRequestStatus.WAITING);

        when(serviceRequestRepository.findById(10L)).thenReturn(Optional.of(current));
        when(serviceRequestRepository.countByAttendantIdAndStatus(1L, ServiceRequestStatus.IN_PROGRESS))
                .thenReturn(0L);
        when(attendantRepository.findEligible(ServiceCategory.OTHER_SUBJECTS))
                .thenReturn(List.of(attendant), List.of());
        when(serviceRequestRepository.findFirstByStatusAndCategoryOrderByCreatedAtAsc(
                ServiceRequestStatus.WAITING,
                ServiceCategory.OTHER_SUBJECTS
        )).thenReturn(Optional.of(queued));

        service.finish(10L);

        assertThat(current.getStatus()).isEqualTo(ServiceRequestStatus.COMPLETED);
        assertThat(current.getFinishedAt()).isNotNull();
        assertThat(queued.getStatus()).isEqualTo(ServiceRequestStatus.IN_PROGRESS);
        assertThat(queued.getAttendant()).isEqualTo(attendant);
        assertThat(queued.getStartedAt()).isNotNull();
    }

    private Attendant attendant(Long id, String name, Instant availableSince) {
        return Attendant.builder()
                .id(id)
                .name(name)
                .badge("AG" + id)
                .status(AttendantStatus.AVAILABLE)
                .availableSince(availableSince)
                .maxSimultaneousCustomers(3)
                .categories(Set.of(ServiceCategory.CARD_ISSUES, ServiceCategory.LOAN_CONTRACTING, ServiceCategory.OTHER_SUBJECTS))
                .build();
    }

    private ServiceRequest serviceRequest(Long id, ServiceCategory category, ServiceRequestStatus status) {
        return ServiceRequest.builder()
                .id(id)
                .customerName("Cliente Teste")
                .category(category)
                .status(status)
                .createdAt(Instant.now())
                .build();
    }

    private static class NoOpDashboardSseService extends DashboardSseService {

        private NoOpDashboardSseService() {
            super(null);
        }

        @Override
        public void notifyDashboardChanged() {
            // Test double: SSE delivery is outside the distribution rule tests.
        }
    }
}
