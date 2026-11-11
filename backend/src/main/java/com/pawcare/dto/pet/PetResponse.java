package com.pawcare.dto.pet;

import com.pawcare.entity.Pet;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PetResponse(
        Long id,
        OwnerSummary owner,
        String name,
        String species,
        String breed,
        LocalDate birthDate,
        BigDecimal weightKg,
        String sex,
        String collarId,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static PetResponse from(Pet pet) {
        return new PetResponse(
                pet.getId(),
                OwnerSummary.from(pet.getOwner()),
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getBirthDate(),
                pet.getWeightKg(),
                pet.getSex(),
                pet.getCollarId(),
                pet.getNotes(),
                pet.getCreatedAt(),
                pet.getUpdatedAt()
        );
    }
}
