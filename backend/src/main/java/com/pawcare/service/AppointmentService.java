package com.pawcare.service;

import com.pawcare.dto.appointment.AppointmentRequest;
import com.pawcare.dto.appointment.AppointmentResponse;
import com.pawcare.dto.appointment.AppointmentUpdateRequest;
import com.pawcare.dto.appointment.StatusUpdateRequest;
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
import com.pawcare.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PetRepository petRepository,
                              UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
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
        Pet pet = findActivePet(request.petId());
        assertPetAccess(pet, principal);

        User veterinarian = resolveVeterinarian(request.veterinarianId(), principal);
        Instant start = request.scheduledAt();
        int duration = request.durationMinutes();
        Instant end = start.plus(duration, ChronoUnit.MINUTES);

        validateSchedule(start, end, veterinarian.getId(), null);

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
        Appointment appointment = findAppointment(id);
        assertAccess(appointment, principal);
        assertEditable(appointment);

        User veterinarian = appointment.getVeterinarian();
        if (request.veterinarianId() != null
                && !request.veterinarianId().equals(veterinarian.getId())) {
            if (principal.role() == Role.OWNER) {
                throw new ForbiddenOperationException("Solo el personal veterinario puede reasignar la cita");
            }
            veterinarian = resolveVeterinarian(request.veterinarianId(), principal);
        }

        Instant start = request.scheduledAt();
        int duration = request.durationMinutes();
        Instant end = start.plus(duration, ChronoUnit.MINUTES);
        validateSchedule(start, end, veterinarian.getId(), id);

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
        if (principal.role() == Role.OWNER) {
            throw new ForbiddenOperationException("Solo el personal veterinario puede cambiar el estado");
        }

        Appointment appointment = findAppointment(id);
        assertAccess(appointment, principal);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED
                && request.status() != AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Una cita cancelada no puede reactivarse");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED
                && request.status() != AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Una cita completada no puede cambiar de estado");
        }

        appointment.setStatus(request.status());
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

    private User resolveVeterinarian(Long requestedVetId, UserPrincipal principal) {
        Long vetId = requestedVetId;
        if (vetId == null) {
            if (principal.role() == Role.VETERINARIAN) {
                vetId = principal.id();
            } else {
                throw new BadRequestException("Debe indicar el veterinario de la cita");
            }
        }

        Long finalVetId = vetId;
        User veterinarian = userRepository.findById(finalVetId)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario", finalVetId));

        if (veterinarian.getRole() != Role.VETERINARIAN && veterinarian.getRole() != Role.ADMIN) {
            throw new BadRequestException("El usuario seleccionado no es veterinario");
        }
        if (!veterinarian.isActive()) {
            throw new BadRequestException("El veterinario seleccionado esta inactivo");
        }
        return veterinarian;
    }

    private void validateSchedule(Instant start, Instant end, Long veterinarianId, Long excludeId) {
        if (start.isBefore(Instant.now())) {
            throw new BadRequestException("La cita debe programarse en el futuro");
        }
        if (!end.isAfter(start)) {
            throw new BadRequestException("La duracion de la cita no es valida");
        }
        boolean hasConflict = excludeId == null
                ? appointmentRepository
                        .existsByVeterinarian_IdAndStatusNotAndScheduledAtLessThanAndEndAtGreaterThan(
                                veterinarianId, AppointmentStatus.CANCELLED, end, start)
                : appointmentRepository
                        .existsByVeterinarian_IdAndStatusNotAndScheduledAtLessThanAndEndAtGreaterThanAndIdNot(
                                veterinarianId, AppointmentStatus.CANCELLED, end, start, excludeId);

        if (hasConflict) {
            throw new ConflictException("El veterinario ya tiene una cita en ese horario");
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
