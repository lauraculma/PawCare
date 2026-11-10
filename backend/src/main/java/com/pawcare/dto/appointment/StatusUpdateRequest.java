package com.pawcare.dto.appointment;

import com.pawcare.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "El estado es obligatorio")
        AppointmentStatus status
) {
}
