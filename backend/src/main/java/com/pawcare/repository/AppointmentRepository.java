package com.pawcare.repository;

import com.pawcare.entity.Appointment;
import com.pawcare.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
            select a from Appointment a
            where a.pet.owner.id = coalesce(:ownerId, a.pet.owner.id)
              and a.veterinarian.id = coalesce(:veterinarianId, a.veterinarian.id)
              and a.pet.id = coalesce(:petId, a.pet.id)
              and a.status = coalesce(:status, a.status)
              and a.scheduledAt >= coalesce(:from, a.scheduledAt)
              and a.scheduledAt <= coalesce(:to, a.scheduledAt)
            """)
    Page<Appointment> search(@Param("ownerId") Long ownerId,
                             @Param("veterinarianId") Long veterinarianId,
                             @Param("petId") Long petId,
                             @Param("status") AppointmentStatus status,
                             @Param("from") Instant from,
                             @Param("to") Instant to,
                             Pageable pageable);

    @Query("""
            select (count(a) > 0) from Appointment a
            where a.veterinarian.id = :veterinarianId
              and a.status <> :cancelled
              and a.id <> coalesce(:excludeId, a.id)
              and a.scheduledAt < :end
              and a.endAt > :start
            """)
    boolean existsConflict(@Param("veterinarianId") Long veterinarianId,
                           @Param("start") Instant start,
                           @Param("end") Instant end,
                           @Param("excludeId") Long excludeId,
                           @Param("cancelled") AppointmentStatus cancelled);
}
