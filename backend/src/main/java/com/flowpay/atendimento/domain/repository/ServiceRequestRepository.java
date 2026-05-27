package com.flowpay.atendimento.domain.repository;

import com.flowpay.atendimento.domain.entity.ServiceRequest;
import com.flowpay.atendimento.domain.enums.ServiceCategory;
import com.flowpay.atendimento.domain.enums.ServiceRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    long countByAttendantIdAndStatus(Long attendantId, ServiceRequestStatus status);

    long countByAttendantId(Long attendantId);

    @Modifying
    @Query("""
        update ServiceRequest sr
        set sr.attendant = null
        where sr.attendant.id = :attendantId
          and sr.status <> com.flowpay.atendimento.domain.enums.ServiceRequestStatus.IN_PROGRESS
        """)
    void clearInactiveAssignmentsForAttendant(@Param("attendantId") Long attendantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ServiceRequest> findFirstByStatusAndCategoryOrderByCreatedAtAsc(
            ServiceRequestStatus status,
            ServiceCategory category
    );

    List<ServiceRequest> findByStatusOrderByCreatedAtAsc(ServiceRequestStatus status);

    long countByStatus(ServiceRequestStatus status);
}
