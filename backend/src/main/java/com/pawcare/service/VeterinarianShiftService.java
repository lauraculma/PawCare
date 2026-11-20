package com.pawcare.service;

import com.pawcare.dto.admin.VeterinarianScheduleResponse;
import com.pawcare.dto.admin.WeeklyShiftUpdateRequest;
import com.pawcare.dto.auth.UserResponse;
import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import com.pawcare.entity.VeterinarianShift;
import com.pawcare.exception.ConflictException;
import com.pawcare.exception.ResourceNotFoundException;
import com.pawcare.repository.UserRepository;
import com.pawcare.repository.VeterinarianShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class VeterinarianShiftService {
    private static final Set<LocalTime> VALID_STARTS = java.util.stream.IntStream.rangeClosed(7, 17)
            .mapToObj(hour -> LocalTime.of(hour, 0)).collect(java.util.stream.Collectors.toSet());
    private final VeterinarianShiftRepository shiftRepository;
    private final UserRepository userRepository;

    public VeterinarianShiftService(VeterinarianShiftRepository shiftRepository, UserRepository userRepository) {
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<VeterinarianScheduleResponse> listSchedules() {
        return userRepository.findAllByRoleAndApprovedTrueAndActiveTrueOrderByFullNameAsc(Role.VETERINARIAN)
                .stream().map(vet -> new VeterinarianScheduleResponse(UserResponse.from(vet),
                        shiftRepository.findAllByVeterinarian_IdOrderByDayOfWeek(vet.getId()).stream()
                                .map(shift -> new VeterinarianScheduleResponse.DayShift(
                                        shift.getDayOfWeek(), shift.getStartTime())).toList()))
                .toList();
    }

    @Transactional
    public VeterinarianScheduleResponse saveSchedule(Long veterinarianId, WeeklyShiftUpdateRequest request) {
        User veterinarian = userRepository.findById(veterinarianId)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario", veterinarianId));
        if (veterinarian.getRole() != Role.VETERINARIAN || !veterinarian.isApproved() || !veterinarian.isActive()) {
            throw new ConflictException("Solo se pueden asignar turnos a veterinarios activos y aprobados");
        }
        if (request.shifts() == null || request.shifts().isEmpty()) {
            throw new ConflictException("Asigna al menos un turno semanal al veterinario");
        }
        Set<Integer> weekdays = new HashSet<>();
        for (var shift : request.shifts()) {
            if (!weekdays.add(shift.dayOfWeek())) {
                throw new ConflictException("No puede asignar más de un turno al mismo día");
            }
            if (!VALID_STARTS.contains(shift.startTime())) {
                throw new ConflictException("El turno debe comenzar en una hora exacta entre las 7:00 y las 17:00");
            }
        }
        shiftRepository.deleteAllByVeterinarian_Id(veterinarianId);
        shiftRepository.flush();
        List<VeterinarianShift> saved = new ArrayList<>();
        for (var entry : request.shifts()) {
            VeterinarianShift shift = new VeterinarianShift();
            shift.setVeterinarian(veterinarian);
            shift.setDayOfWeek(entry.dayOfWeek());
            shift.setStartTime(entry.startTime());
            saved.add(shift);
        }
        shiftRepository.saveAll(saved);
        return new VeterinarianScheduleResponse(UserResponse.from(veterinarian), request.shifts().stream()
                .map(entry -> new VeterinarianScheduleResponse.DayShift(entry.dayOfWeek(), entry.startTime()))
                .sorted(java.util.Comparator.comparingInt(VeterinarianScheduleResponse.DayShift::dayOfWeek)).toList());
    }
}
