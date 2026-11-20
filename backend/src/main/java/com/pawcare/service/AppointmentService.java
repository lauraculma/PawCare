package com.pawcare.service;

import com.pawcare.dto.appointment.AppointmentRequest;
import com.pawcare.dto.appointment.AppointmentResponse;
import com.pawcare.dto.appointment.AppointmentUpdateRequest;
import com.pawcare.dto.appointment.AvailabilitySlot;
import com.pawcare.dto.appointment.NearestAvailability;
import com.pawcare.dto.appointment.StatusUpdateRequest;
import com.pawcare.dto.appointment.VeterinarianOption;
import com.pawcare.entity.Appointment;
import com.pawcare.entity.AppointmentStatus;
import com.pawcare.entity.Pet;
import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import com.pawcare.exception.BadRequestException;
import com.pawcare.exception.ConflictException;
import com.pawcare.exception.ForbiddenOperationException;
import com.pawcare.exception.ResourceNotFoundException;
import com.pawcare.repository.AppointmentRepository;
import com.pawcare.repository.PetRepository;
import com.pawcare.repository.UserRepository;
import com.pawcare.repository.VeterinarianShiftRepository;
import com.pawcare.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final VeterinarianShiftRepository shiftRepository;
    private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Bogota");

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PetRepository petRepository,
                              UserRepository userRepository,
                              VeterinarianShiftRepository shiftRepository) {
        this.appointmentRepository = appointmentRepository;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.shiftRepository = shiftRepository;
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> list(UserPrincipal principal,
                                          Long veterinarianId,
                                          Long petId,
                                          AppointmentStatus status,
                                          Instant from,
                                          Instant to,
                                          Pageable pageable) {
        Long ownerFilter = principal.role() == Role.OWNER ? principal.id() : null;
        Long vetFilter = resolveVetFilter(veterinarianId, principal);

        return appointmentRepository
                .search(ownerFilter, vetFilter, petId, status, from, to, pageable)
                .map(AppointmentResponse::from);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Long id, UserPrincipal principal) {
        Appointment appointment = findAppointment(id);
        assertAccess(appointment, principal);
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse create(AppointmentRequest request, UserPrincipal principal) {
        Pet pet = petRepository.findByPublicIdAndActiveTrue(request.petPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota", request.petPublicId()));
        assertPetAccess(pet, principal);

        User veterinarian = resolveVeterinarian(request.veterinarianCedula(), principal);
        Instant start = request.scheduledAt();
        int duration = request.durationMinutes();
        Instant end = start.plus(duration, ChronoUnit.MINUTES);

        validateSchedule(start, end, veterinarian, null);

        Appointment appointment = new Appointment();
        appointment.setPet(pet);
        appointment.setVeterinarian(veterinarian);
        appointment.setScheduledAt(start);
        appointment.setDurationMinutes(duration);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setReason(request.reason().trim());
        appointment.setNotes(normalize(request.notes()));

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse update(Long id, AppointmentUpdateRequest request, UserPrincipal principal) {
        if (principal.role() != Role.ADMIN) {
            throw new ForbiddenOperationException("Solo el administrador puede editar citas");
        }
        Appointment appointment = findAppointment(id);
        assertAccess(appointment, principal);
        assertEditable(appointment);

        User veterinarian = appointment.getVeterinarian();
        if (request.veterinarianCedula() != null
                && !request.veterinarianCedula().equals(veterinarian.getCedula())) {
            if (principal.role() == Role.OWNER) {
                throw new ForbiddenOperationException("Solo el personal veterinario puede reasignar la cita");
            }
            veterinarian = resolveVeterinarian(request.veterinarianCedula(), principal);
        }

        Instant start = request.scheduledAt();
        int duration = request.durationMinutes();
        Instant end = start.plus(duration, ChronoUnit.MINUTES);
        validateSchedule(start, end, veterinarian, id);

        appointment.setVeterinarian(veterinarian);
        appointment.setScheduledAt(start);
        appointment.setDurationMinutes(duration);
        appointment.setReason(request.reason().trim());
        appointment.setNotes(normalize(request.notes()));

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse cancel(Long id, UserPrincipal principal) {
        Appointment appointment = findAppointment(id);
        assertAccess(appointment, principal);

        if (appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new BadRequestException("No se puede cancelar una cita finalizada");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse updateStatus(Long id, StatusUpdateRequest request, UserPrincipal principal) {
        if (principal.role() != Role.VETERINARIAN) {
            throw new ForbiddenOperationException("Solo el veterinario asignado puede marcar la cita como asistida");
        }
        if (request.status() != AppointmentStatus.COMPLETED) {
            throw new BadRequestException("El veterinario solo puede marcar una cita como asistida");
        }

        Appointment appointment = findAppointment(id);
        assertAccess(appointment, principal);

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Solo se pueden finalizar citas programadas");
        }
        if (appointment.getEndAt().isAfter(Instant.now())) {
            throw new BadRequestException("La cita solo se puede marcar como asistida cuando haya finalizado");
        }

        appointment.setStatus(request.status());
        appointment.setNotes(normalize(request.notes()));
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    private Long resolveVetFilter(Long requestedVetId, UserPrincipal principal) {
        if (principal.role() == Role.VETERINARIAN) {
            if (requestedVetId != null && !requestedVetId.equals(principal.id())) {
                throw new ForbiddenOperationException("Solo puede consultar su propia agenda");
            }
            return principal.id();
        }
        return requestedVetId;
    }

    private User resolveVeterinarian(String requestedCedula, UserPrincipal principal) {
        String cedula = requestedCedula;
        if (cedula == null || cedula.isBlank()) {
            if (principal.role() == Role.VETERINARIAN) {
                User currentVet = userRepository.findById(principal.id())
                        .orElseThrow(() -> new ResourceNotFoundException("Veterinario", principal.id()));
                cedula = currentVet.getCedula();
            } else {
                throw new BadRequestException("Debe indicar el veterinario de la cita");
            }
        }
        if (cedula == null || cedula.isBlank()) {
            throw new BadRequestException("El veterinario no tiene una cedula registrada");
        }
        String resolvedCedula = cedula.trim();
        User veterinarian = userRepository.findByCedula(resolvedCedula)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario", resolvedCedula));

        if (veterinarian.getRole() != Role.VETERINARIAN) {
            throw new BadRequestException("El usuario seleccionado no es veterinario");
        }
        if (!veterinarian.isActive() || !veterinarian.isApproved()) {
            throw new BadRequestException("El veterinario seleccionado no esta activo y aprobado");
        }
        return veterinarian;
    }

    private void validateSchedule(Instant start, Instant end, User veterinarian, Long excludeId) {
        if (start.isBefore(Instant.now())) {
            throw new BadRequestException("La cita debe programarse en el futuro");
        }
        if (!end.equals(start.plus(1, ChronoUnit.HOURS))) {
            throw new BadRequestException("La cita debe durar exactamente una hora");
        }
        assertWithinAssignedShift(veterinarian, start, end);
        boolean hasConflict = excludeId == null
                ? appointmentRepository
                        .existsByVeterinarian_IdAndStatusNotAndScheduledAtLessThanAndEndAtGreaterThan(
                                veterinarian.getId(), AppointmentStatus.CANCELLED, end, start)
                : appointmentRepository
                        .existsByVeterinarian_IdAndStatusNotAndScheduledAtLessThanAndEndAtGreaterThanAndIdNot(
                                veterinarian.getId(), AppointmentStatus.CANCELLED, end, start, excludeId);

        if (hasConflict) {
            throw new ConflictException("El veterinario ya tiene una cita en ese horario");
        }
    }

    @Transactional(readOnly = true)
    public List<VeterinarianOption> veterinarians() {
        return userRepository.findAllByRoleAndApprovedTrueAndActiveTrueOrderByFullNameAsc(Role.VETERINARIAN)
                .stream().filter(vet -> vet.getCedula() != null && !vet.getCedula().isBlank())
                .map(vet -> new VeterinarianOption(vet.getCedula(), vet.getFullName())).toList();
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlot> availability(LocalDate date, String veterinarianCedula, Long excludeAppointmentId) {
        int weekday = date.getDayOfWeek().getValue();
        List<AvailabilitySlot> available = new ArrayList<>();
        for (var shift : shiftRepository.findAllByDayOfWeek(weekday)) {
            User vet = shift.getVeterinarian();
            if (!vet.isActive() || !vet.isApproved() || vet.getRole() != Role.VETERINARIAN
                    || vet.getCedula() == null || vet.getCedula().isBlank()
                    || (veterinarianCedula != null && !veterinarianCedula.equals(vet.getCedula()))) continue;
            LocalTime shiftEnd = shift.getStartTime().plusHours(5);
            for (LocalTime time = shift.getStartTime(); !time.plusHours(1).isAfter(shiftEnd); time = time.plusHours(1)) {
                Instant start = date.atTime(time).atZone(CLINIC_ZONE).toInstant();
                Instant end = start.plus(1, ChronoUnit.HOURS);
                if (start.isBefore(Instant.now())) continue;
                boolean conflict = excludeAppointmentId == null
                        ? appointmentRepository.existsByVeterinarian_IdAndStatusNotAndScheduledAtLessThanAndEndAtGreaterThan(
                                vet.getId(), AppointmentStatus.CANCELLED, end, start)
                        : appointmentRepository.existsByVeterinarian_IdAndStatusNotAndScheduledAtLessThanAndEndAtGreaterThanAndIdNot(
                                vet.getId(), AppointmentStatus.CANCELLED, end, start, excludeAppointmentId);
                if (!conflict) available.add(new AvailabilitySlot(time.toString(), vet.getCedula(), vet.getFullName()));
            }
        }
        return available.stream().sorted(java.util.Comparator.comparing(AvailabilitySlot::time)
                .thenComparing(AvailabilitySlot::veterinarianName)).toList();
    }

    @Transactional(readOnly = true)
    public List<LocalDate> availableDates(YearMonth month, String veterinarianCedula, Long excludeAppointmentId) {
        LocalDate today = LocalDate.now(CLINIC_ZONE);
        LocalDate first = month.atDay(1).isBefore(today) ? today : month.atDay(1);
        LocalDate last = month.atEndOfMonth();
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
            if (!availability(date, veterinarianCedula, excludeAppointmentId).isEmpty()) dates.add(date);
        }
        return dates;
    }

    @Transactional(readOnly = true)
    public NearestAvailability nearestAvailability(LocalDate fromDate, Long excludeAppointmentId) {
        LocalDate start = fromDate.isBefore(LocalDate.now(CLINIC_ZONE)) ? LocalDate.now(CLINIC_ZONE) : fromDate;
        for (int day = 0; day < 90; day++) {
            LocalDate date = start.plusDays(day);
            List<AvailabilitySlot> slots = availability(date, null, excludeAppointmentId);
            if (!slots.isEmpty()) {
                AvailabilitySlot slot = slots.get(0);
                return new NearestAvailability(date, slot.time(), slot.veterinarianCedula(), slot.veterinarianName());
            }
        }
        throw new BadRequestException("No hay citas disponibles en los próximos 90 días");
    }

    private void assertWithinAssignedShift(User veterinarian, Instant start, Instant end) {
        var localStart = start.atZone(CLINIC_ZONE);
        var localEnd = end.atZone(CLINIC_ZONE);
        var shift = shiftRepository.findByVeterinarian_IdAndDayOfWeek(veterinarian.getId(), localStart.getDayOfWeek().getValue())
                .orElseThrow(() -> new BadRequestException("El veterinario no tiene turno asignado para ese día"));
        LocalTime shiftEnd = shift.getStartTime().plusHours(5);
        LocalTime appointmentEnd = localEnd.toLocalTime();
        long minutesFromShiftStart = java.time.Duration.between(shift.getStartTime(), localStart.toLocalTime()).toMinutes();
        if (localStart.toLocalTime().isBefore(shift.getStartTime())
                || localStart.getSecond() != 0 || localStart.getNano() != 0
                || minutesFromShiftStart % 60 != 0
                || appointmentEnd.isAfter(shiftEnd)
                || !localStart.toLocalDate().equals(localEnd.toLocalDate())) {
            throw new BadRequestException("La hora seleccionada está fuera del turno asignado al veterinario");
        }
    }

    private void assertEditable(Appointment appointment) {
        if (appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new BadRequestException("La cita ya no puede modificarse");
        }
    }

    private void assertAccess(Appointment appointment, UserPrincipal principal) {
        if (principal.role() == Role.OWNER
                && !appointment.getPet().getOwner().getId().equals(principal.id())) {
            throw new ForbiddenOperationException("No tiene acceso a esta cita");
        }
        if (principal.role() == Role.VETERINARIAN
                && !appointment.getVeterinarian().getId().equals(principal.id())) {
            throw new ForbiddenOperationException("No tiene acceso a esta cita");
        }
    }

    private void assertPetAccess(Pet pet, UserPrincipal principal) {
        if (principal.role() == Role.OWNER && !pet.getOwner().getId().equals(principal.id())) {
            throw new ForbiddenOperationException("No tiene acceso a esta mascota");
        }
    }

    private Pet findActivePet(Long petId) {
        return petRepository.findByIdAndActiveTrue(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota", petId));
    }

    private Appointment findAppointment(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", id));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
