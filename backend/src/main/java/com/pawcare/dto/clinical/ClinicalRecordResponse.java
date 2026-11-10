package com.pawcare.dto.clinical;

import com.pawcare.dto.pet.OwnerSummary;
import com.pawcare.entity.ClinicalRecord;

import java.time.Instant;

public record ClinicalRecordResponse(
        Long id,
        Long appointmentId,
        Long petId,
        String petName,
        OwnerSummary owner,
        Long veterinarianId,
        String veterinarianName,
        String diagnosis,
        String treatment,
        String prescription,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static ClinicalRecordResponse from(ClinicalRecord record) {
        return new ClinicalRecordResponse(
                record.getId(),
                record.getAppointment().getId(),
                record.getPet().getId(),
                record.getPet().getName(),
                OwnerSummary.from(record.getPet().getOwner()),
                record.getVeterinarian().getId(),
                record.getVeterinarian().getFullName(),
                record.getDiagnosis(),
                record.getTreatment(),
                record.getPrescription(),
                record.getNotes(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
