package com.pawcare.dto.clinical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClinicalRecordRequest(
        @NotNull(message = "La cita es obligatoria")
        Long appointmentId,

        @NotBlank(message = "El diagnostico es obligatorio")
        @Size(max = 240, message = "El diagnostico no puede superar 240 caracteres")
        String diagnosis,

        String treatment,

        String prescription,

        String notes
) {
}
