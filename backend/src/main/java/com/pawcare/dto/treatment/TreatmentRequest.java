package com.pawcare.dto.treatment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TreatmentRequest(
        @NotNull(message = "El registro clinico es obligatorio")
        Long clinicalRecordId,

        @NotBlank(message = "El medicamento es obligatorio")
        @Size(max = 120, message = "El medicamento no puede superar 120 caracteres")
        String medication,

        @NotBlank(message = "La dosis es obligatoria")
        @Size(max = 120, message = "La dosis no puede superar 120 caracteres")
        String dosage,

        @NotBlank(message = "La frecuencia es obligatoria")
        @Size(max = 120, message = "La frecuencia no puede superar 120 caracteres")
        String frequency,

        @Size(max = 120, message = "La duracion no puede superar 120 caracteres")
        String duration,

        String instructions
) {
}
