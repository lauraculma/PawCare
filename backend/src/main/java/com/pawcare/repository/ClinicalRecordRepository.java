package com.pawcare.repository;

import com.pawcare.entity.ClinicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicalRecordRepository extends JpaRepository<ClinicalRecord, Long> {

    @Query("""
            select c from ClinicalRecord c
            where (:ownerId is null or c.pet.owner.id = :ownerId)
              and (:petId is null or c.pet.id = :petId)
              and (:veterinarianId is null or c.veterinarian.id = :veterinarianId)
              and (:appointmentId is null or c.appointment.id = :appointmentId)
            """)
    Page<ClinicalRecord> search(@Param("ownerId") Long ownerId,
                                @Param("petId") Long petId,
                                @Param("veterinarianId") Long veterinarianId,
                                @Param("appointmentId") Long appointmentId,
                                Pageable pageable);

    boolean existsByAppointmentId(Long appointmentId);
}
