package com.pawcare.dto.appointment;

import com.pawcare.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StatusUpdateRequest(
        @NotNull(message = "El estado es obligatorio")
        AppointmentStatus status,

        @NotBlank(message = "Las notas de finalizacion son obligatorias")
        @Size(max = 5000, message = "Las notas no pueden superar 5000 caracteres")
        String notes
) {
    public StatusUpdateRequest(AppointmentStatus status) {
        this(status, null);
    }
}
