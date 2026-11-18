package com.pawcare.dto.treatment;

import com.pawcare.dto.pet.OwnerSummary;
import com.pawcare.entity.ClinicalRecord;
import com.pawcare.entity.Treatment;

import java.time.Instant;

public record TreatmentResponse(
        Long id,
        Long clinicalRecordId,
        Long petId,
        String petName,
        OwnerSummary owner,
        Long veterinarianId,
        String veterinarianName,
        String medication,
        String dosage,
        String frequency,
        String duration,
        String instructions,
        Instant createdAt,
        Instant updatedAt
) {
    public static TreatmentResponse from(Treatment treatment) {
        ClinicalRecord record = treatment.getClinicalRecord();
        return new TreatmentResponse(
                treatment.getId(),
                record.getId(),
                record.getPet().getId(),
                record.getPet().getName(),
                OwnerSummary.from(record.getPet().getOwner()),
                record.getVeterinarian().getId(),
                record.getVeterinarian().getFullName(),
                treatment.getMedication(),
                treatment.getDosage(),
                treatment.getFrequency(),
                treatment.getDuration(),
                treatment.getInstructions(),
                treatment.getCreatedAt(),
                treatment.getUpdatedAt()
        );
    }
}
