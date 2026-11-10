package com.pawcare.dto.pet;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PetRequest(
        @NotBlank(message = "El nombre de la mascota es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String name,

        @NotBlank(message = "La especie es obligatoria")
        @Size(max = 40, message = "La especie no puede superar 40 caracteres")
        String species,

        @Size(max = 80, message = "La raza no puede superar 80 caracteres")
        String breed,

        @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
        LocalDate birthDate,

        @DecimalMin(value = "0.01", message = "El peso debe ser mayor que cero")
        @DecimalMax(value = "500.00", message = "El peso no es valido")
        BigDecimal weightKg,

        @Size(max = 20, message = "El sexo no puede superar 20 caracteres")
        String sex,

        @Size(max = 60, message = "El identificador del collar no puede superar 60 caracteres")
        String collarId,

        String notes,

        Long ownerId
) {
}
