package com.flowpay.atendimento.domain.repository;

import com.flowpay.atendimento.domain.entity.Attendant;
import com.flowpay.atendimento.domain.enums.AttendantStatus;
import com.flowpay.atendimento.domain.enums.ServiceCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttendantRepository extends JpaRepository<Attendant, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select a from Attendant a
        where a.status <> com.flowpay.atendimento.domain.enums.AttendantStatus.INACTIVE
          and a.status <> com.flowpay.atendimento.domain.enums.AttendantStatus.PAUSED
          and :category member of a.categories
          and (
            select count(sr)
            from ServiceRequest sr
            where sr.attendant = a
              and sr.status = com.flowpay.atendimento.domain.enums.ServiceRequestStatus.IN_PROGRESS
          ) < a.maxSimultaneousCustomers
        order by (
            select count(sr2)
            from ServiceRequest sr2
            where sr2.attendant = a
              and sr2.status = com.flowpay.atendimento.domain.enums.ServiceRequestStatus.IN_PROGRESS
        ) asc, a.id asc
        """)
    List<Attendant> findEligible(@Param("category") ServiceCategory category);

    List<Attendant> findByStatus(AttendantStatus status);

    long countByStatus(AttendantStatus status);

    @Query("""
        select a from Attendant a
        where a.status <> com.flowpay.atendimento.domain.enums.AttendantStatus.INACTIVE
        order by a.id asc
        """)
    List<Attendant> findLogged();

    @Query("""
        select count(a) from Attendant a
        where a.status <> com.flowpay.atendimento.domain.enums.AttendantStatus.INACTIVE
        """)
    long countLogged();

    Optional<Attendant> findByBadgeIgnoreCase(String badge);
}
