package com.pawcare.dto.appointment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AppointmentUpdateRequest(
        @NotNull(message = "La fecha y hora son obligatorias")
        Instant scheduledAt,

        @NotNull(message = "La duracion es obligatoria")
        @Min(value = 60, message = "La cita debe durar 60 minutos")
        @Max(value = 60, message = "La cita debe durar 60 minutos")
        Integer durationMinutes,

        @NotBlank(message = "El motivo de la cita es obligatorio")
        @Size(max = 240, message = "El motivo no puede superar 240 caracteres")
        String reason,

        String notes,

        String veterinarianCedula
) {
}
