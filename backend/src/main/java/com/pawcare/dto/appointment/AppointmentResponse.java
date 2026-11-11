package com.pawcare.dto.appointment;

import com.pawcare.dto.pet.OwnerSummary;
import com.pawcare.entity.Appointment;
import com.pawcare.entity.AppointmentStatus;

import java.time.Instant;

public record AppointmentResponse(
        Long id,
        Long petId,
        String petName,
        String petSpecies,
        OwnerSummary owner,
        Long veterinarianId,
        String veterinarianName,
        Instant scheduledAt,
        Instant endAt,
        int durationMinutes,
        AppointmentStatus status,
        String reason,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPet().getId(),
                appointment.getPet().getName(),
                appointment.getPet().getSpecies(),
                OwnerSummary.from(appointment.getPet().getOwner()),
                appointment.getVeterinarian().getId(),
                appointment.getVeterinarian().getFullName(),
                appointment.getScheduledAt(),
                appointment.getEndAt(),
                appointment.getDurationMinutes(),
                appointment.getStatus(),
                appointment.getReason(),
                appointment.getNotes(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }
}
