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
            where c.pet.owner.id = coalesce(:ownerId, c.pet.owner.id)
              and c.pet.id = coalesce(:petId, c.pet.id)
              and c.veterinarian.id = coalesce(:veterinarianId, c.veterinarian.id)
              and c.appointment.id = coalesce(:appointmentId, c.appointment.id)
            """)
    Page<ClinicalRecord> search(@Param("ownerId") Long ownerId,
                                @Param("petId") Long petId,
                                @Param("veterinarianId") Long veterinarianId,
                                @Param("appointmentId") Long appointmentId,
                                Pageable pageable);

    boolean existsByAppointmentId(Long appointmentId);
}
