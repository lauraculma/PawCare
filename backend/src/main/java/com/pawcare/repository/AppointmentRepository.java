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
            where (:ownerId is null or a.pet.owner.id = :ownerId)
              and (:veterinarianId is null or a.veterinarian.id = :veterinarianId)
              and (:petId is null or a.pet.id = :petId)
              and (:status is null or a.status = :status)
              and (:from is null or a.scheduledAt >= :from)
              and (:to is null or a.scheduledAt <= :to)
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
              and (:excludeId is null or a.id <> :excludeId)
              and a.scheduledAt < :end
              and a.endAt > :start
            """)
    boolean existsConflict(@Param("veterinarianId") Long veterinarianId,
                           @Param("start") Instant start,
                           @Param("end") Instant end,
                           @Param("excludeId") Long excludeId,
                           @Param("cancelled") AppointmentStatus cancelled);
}
