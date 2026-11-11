package com.pawcare.service;

import com.pawcare.dto.appointment.AppointmentRequest;
import com.pawcare.dto.appointment.AppointmentResponse;
import com.pawcare.dto.appointment.StatusUpdateRequest;
import com.pawcare.entity.Appointment;
import com.pawcare.entity.AppointmentStatus;
import com.pawcare.entity.Pet;
import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import com.pawcare.exception.BadRequestException;
import com.pawcare.exception.ConflictException;
import com.pawcare.exception.ForbiddenOperationException;
import com.pawcare.repository.AppointmentRepository;
import com.pawcare.repository.PetRepository;
import com.pawcare.repository.UserRepository;
import com.pawcare.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private UserRepository userRepository;

    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(appointmentRepository, petRepository, userRepository);
    }

    @Test
    void ownerCreatesAppointmentForOwnPet() {
        User owner = user(7L, Role.OWNER);
        User vet = user(2L, Role.VETERINARIAN);
        Pet pet = pet(1L, owner);
        Instant start = futureStart();

        when(petRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(pet));
        when(userRepository.findById(2L)).thenReturn(Optional.of(vet));
        when(appointmentRepository
                .existsByVeterinarian_IdAndStatusNotAndScheduledAtLessThanAndEndAtGreaterThan(
                        2L, AppointmentStatus.CANCELLED, start.plus(30, ChronoUnit.MINUTES), start))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            appointment.setId(10L);
            return appointment;
        });

        AppointmentResponse response = appointmentService.create(request(1L, 2L, start), principal(owner));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(response.endAt()).isEqualTo(start.plus(30, ChronoUnit.MINUTES));
        assertThat(response.veterinarianId()).isEqualTo(2L);
    }

    @Test
    void ownerCannotBookForAnotherOwnersPet() {
        User owner = user(7L, Role.OWNER);
        User another = user(8L, Role.OWNER);
        Pet pet = pet(1L, another);
        when(petRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(pet));

        assertThatThrownBy(() -> appointmentService.create(request(1L, 2L, futureStart()), principal(owner)))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void rejectsOverlappingAppointment() {
        User owner = user(7L, Role.OWNER);
        User vet = user(2L, Role.VETERINARIAN);
        Pet pet = pet(1L, owner);
        Instant start = futureStart();

        when(petRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(pet));
        when(userRepository.findById(2L)).thenReturn(Optional.of(vet));
        when(appointmentRepository
                .existsByVeterinarian_IdAndStatusNotAndScheduledAtLessThanAndEndAtGreaterThan(
                        2L, AppointmentStatus.CANCELLED, start.plus(30, ChronoUnit.MINUTES), start))
                .thenReturn(true);

        assertThatThrownBy(() -> appointmentService.create(request(1L, 2L, start), principal(owner)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void veterinarianDefaultsToSelf() {
        User vet = user(2L, Role.VETERINARIAN);
        Pet pet = pet(1L, user(7L, Role.OWNER));
        Instant start = futureStart();

        when(petRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(pet));
        when(userRepository.findById(2L)).thenReturn(Optional.of(vet));
        when(appointmentRepository
                .existsByVeterinarian_IdAndStatusNotAndScheduledAtLessThanAndEndAtGreaterThan(
                        2L, AppointmentStatus.CANCELLED, start.plus(30, ChronoUnit.MINUTES), start))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.create(request(1L, null, start), principal(vet));

        assertThat(response.veterinarianId()).isEqualTo(2L);
    }

    @Test
    void cannotCancelCompletedAppointment() {
        User owner = user(7L, Role.OWNER);
        Appointment appointment = appointment(5L, pet(1L, owner), user(2L, Role.VETERINARIAN));
        appointment.setStatus(AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.cancel(5L, principal(owner)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void ownerCannotChangeStatus() {
        User owner = user(7L, Role.OWNER);

        assertThatThrownBy(() -> appointmentService.updateStatus(5L,
                new StatusUpdateRequest(AppointmentStatus.CONFIRMED), principal(owner)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void ownerListIsFilteredByOwner() {
        User owner = user(7L, Role.OWNER);
        Pageable pageable = PageRequest.of(0, 50);
        when(appointmentRepository.search(7L, null, null, null, null, null, pageable))
                .thenReturn(Page.empty());

        Page<AppointmentResponse> result = appointmentService.list(
                principal(owner), null, null, null, null, null, pageable);

        assertThat(result).isEmpty();
        verify(appointmentRepository).search(7L, null, null, null, null, null, pageable);
    }

    private AppointmentRequest request(Long petId, Long vetId, Instant start) {
        return new AppointmentRequest(petId, vetId, start, 30, "Control general", "Sin novedades");
    }

    private Instant futureStart() {
        return Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setFullName("Usuario " + id);
        user.setEmail("user" + id + "@pawcare.test");
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private UserPrincipal principal(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), null, user.getFullName(), user.getRole(), true);
    }

    private Pet pet(Long id, User owner) {
        Pet pet = new Pet();
        pet.setId(id);
        pet.setOwner(owner);
        pet.setName("Luna");
        pet.setSpecies("Perro");
        pet.setActive(true);
        return pet;
    }

    private Appointment appointment(Long id, Pet pet, User vet) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setPet(pet);
        appointment.setVeterinarian(vet);
        appointment.setScheduledAt(futureStart());
        appointment.setDurationMinutes(30);
        appointment.setReason("Control");
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointment;
    }
}
