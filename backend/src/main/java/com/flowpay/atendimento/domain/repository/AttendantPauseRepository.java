package com.flowpay.atendimento.domain.repository;

import com.flowpay.atendimento.domain.entity.AttendantPause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AttendantPauseRepository extends JpaRepository<AttendantPause, Long> {

    Optional<AttendantPause> findFirstByAttendantIdAndFinishedAtIsNullOrderByStartedAtDesc(Long attendantId);

    long countByAttendantIdAndStartedAtBetween(Long attendantId, Instant start, Instant end);

    @Query(value = """
        select coalesce(sum(extract(epoch from (coalesce(ap.finished_at, now()) - ap.started_at))), 0)
        from attendant_pauses ap
        where ap.attendant_id = :attendantId
          and ap.started_at >= :start
          and ap.started_at < :end
        """, nativeQuery = true)
    double sumPauseSecondsByAttendantAndStartedAtBetween(
            @Param("attendantId") Long attendantId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}
