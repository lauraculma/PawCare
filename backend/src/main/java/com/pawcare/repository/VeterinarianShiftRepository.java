package com.pawcare.repository;

import com.pawcare.entity.VeterinarianShift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VeterinarianShiftRepository extends JpaRepository<VeterinarianShift, Long> {
    List<VeterinarianShift> findAllByVeterinarian_IdOrderByDayOfWeek(Long veterinarianId);
    List<VeterinarianShift> findAllByDayOfWeek(int dayOfWeek);
    Optional<VeterinarianShift> findByVeterinarian_IdAndDayOfWeek(Long veterinarianId, int dayOfWeek);
    void deleteAllByVeterinarian_Id(Long veterinarianId);
}
