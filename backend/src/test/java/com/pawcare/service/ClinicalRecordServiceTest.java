package com.pawcare.service;

import com.pawcare.dto.clinical.ClinicalRecordRequest;
import com.pawcare.dto.clinical.ClinicalRecordResponse;
import com.pawcare.entity.Appointment;
import com.pawcare.entity.AppointmentStatus;
import com.pawcare.entity.ClinicalRecord;
import com.pawcare.entity.Pet;
import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import com.pawcare.exception.ConflictException;
import com.pawcare.exception.ForbiddenOperationException;
import com.pawcare.repository.AppointmentRepository;
import com.pawcare.repository.ClinicalRecordRepository;
import com.pawcare.repository.UserRepository;
import com.pawcare.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalRecordServiceTest {

    @Mock
    private ClinicalRecordRepository clinicalRecordRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private UserRepository userRepository;

    private ClinicalRecordService clinicalRecordService;

    @BeforeEach
    void setUp() {
        clinicalRecordService = new ClinicalRecordService(
                clinicalRecordRepository, appointmentRepository, userRepository);
    }

    @Test
    void veterinarianCreatesRecordForOwnAppointment() {
        User owner = user(7L, Role.OWNER);
        User vet = user(2L, Role.VETERINARIAN);
        Appointment appointment = appointment(1L, pet(owner), vet);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(clinicalRecordRepository.existsByAppointmentId(1L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(vet));
        when(clinicalRecordRepository.save(any(ClinicalRecord.class))).thenAnswer(invocation -> {
            ClinicalRecord record = invocation.getArgument(0);
            record.setId(20L);
            return record;
        });

        ClinicalRecordResponse response = clinicalRecordService.create(request(1L), principal(vet));

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.petId()).isEqualTo(1L);
        assertThat(response.veterinarianId()).isEqualTo(2L);
        assertThat(response.diagnosis()).isEqualTo("Dermatitis leve");
    }

    @Test
    void ownerCannotCreateRecord() {
        User owner = user(7L, Role.OWNER);

        assertThatThrownBy(() -> clinicalRecordService.create(request(1L), principal(owner)))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(clinicalRecordRepository, never()).save(any(ClinicalRecord.class));
    }

    @Test
    void veterinarianCannotWriteForAnotherVetsAppointment() {
        User owner = user(7L, Role.OWNER);
        User vet = user(2L, Role.VETERINARIAN);
        User otherVet = user(3L, Role.VETERINARIAN);
        Appointment appointment = appointment(1L, pet(owner), vet);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> clinicalRecordService.create(request(1L), principal(otherVet)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void rejectsDuplicatedRecordForAppointment() {
        User owner = user(7L, Role.OWNER);
        User vet = user(2L, Role.VETERINARIAN);
        Appointment appointment = appointment(1L, pet(owner), vet);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(clinicalRecordRepository.existsByAppointmentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> clinicalRecordService.create(request(1L), principal(vet)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void ownerCannotReadRecordOfAnotherPet() {
        User anotherOwner = user(8L, Role.OWNER);
        ClinicalRecord record = record(30L, appointment(1L, pet(user(7L, Role.OWNER)),
                user(2L, Role.VETERINARIAN)));
        when(clinicalRecordRepository.findById(30L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> clinicalRecordService.get(30L, principal(anotherOwner)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void onlyAuthorCanUpdateRecord() {
        User vet = user(2L, Role.VETERINARIAN);
        User otherVet = user(3L, Role.VETERINARIAN);
        ClinicalRecord record = record(30L, appointment(1L, pet(user(7L, Role.OWNER)), vet));
        when(clinicalRecordRepository.findById(30L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> clinicalRecordService.update(30L, request(1L), principal(otherVet)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    private ClinicalRecordRequest request(Long appointmentId) {
        return new ClinicalRecordRequest(appointmentId, "Dermatitis leve", "Antibiotico topico",
                "Crema cada 12 horas", "Control en 7 dias");
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

    private Pet pet(User owner) {
        Pet pet = new Pet();
        pet.setId(1L);
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
        appointment.setScheduledAt(Instant.now().plus(1, ChronoUnit.DAYS));
        appointment.setDurationMinutes(30);
        appointment.setReason("Control");
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointment;
    }

    private ClinicalRecord record(Long id, Appointment appointment) {
        ClinicalRecord record = new ClinicalRecord();
        record.setId(id);
        record.setAppointment(appointment);
        record.setPet(appointment.getPet());
        record.setVeterinarian(appointment.getVeterinarian());
        record.setDiagnosis("Diagnostico");
        return record;
    }
}
