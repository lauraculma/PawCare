package com.pawcare.dto.vital;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record VitalSignRequest(
        @NotNull(message = "La mascota es obligatoria")
        Long petId,

        @Size(max = 60, message = "El identificador del collar no puede superar 60 caracteres")
        String collarId,

        @Min(value = 20, message = "La frecuencia cardiaca minima es 20 lpm")
        @Max(value = 300, message = "La frecuencia cardiaca maxima es 300 lpm")
        Integer heartRate,

        @Min(value = 0, message = "La saturacion minima es 0")
        @Max(value = 100, message = "La saturacion maxima es 100")
        Integer spo2,

        @DecimalMin(value = "30.0", message = "La temperatura minima es 30 C")
        @DecimalMax(value = "45.0", message = "La temperatura maxima es 45 C")
        Double temperatureC,

        @Size(max = 30, message = "El nivel de actividad no puede superar 30 caracteres")
        String activityLevel,

        Instant recordedAt
) {
}
