package com.pawcare.service;

import com.pawcare.dto.treatment.TreatmentRequest;
import com.pawcare.dto.treatment.TreatmentResponse;
import com.pawcare.entity.Appointment;
import com.pawcare.entity.AppointmentStatus;
import com.pawcare.entity.ClinicalRecord;
import com.pawcare.entity.Pet;
import com.pawcare.entity.Role;
import com.pawcare.entity.Treatment;
import com.pawcare.entity.User;
import com.pawcare.exception.BadRequestException;
import com.pawcare.exception.ForbiddenOperationException;
import com.pawcare.repository.ClinicalRecordRepository;
import com.pawcare.repository.TreatmentRepository;
import com.pawcare.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceTest {

    @Mock
    private TreatmentRepository treatmentRepository;
    @Mock
    private ClinicalRecordRepository clinicalRecordRepository;

    private TreatmentService treatmentService;

    @BeforeEach
    void setUp() {
        treatmentService = new TreatmentService(treatmentRepository, clinicalRecordRepository);
    }

    @Test
    void veterinarianCreatesTreatmentForOwnRecord() {
        User vet = user(2L, Role.VETERINARIAN);
        ClinicalRecord record = record(30L, user(7L, Role.OWNER), vet);
        when(clinicalRecordRepository.findById(30L)).thenReturn(Optional.of(record));
        when(treatmentRepository.save(any(Treatment.class))).thenAnswer(invocation -> {
            Treatment treatment = invocation.getArgument(0);
            treatment.setId(5L);
            return treatment;
        });

        TreatmentResponse response = treatmentService.create(request(30L), principal(vet));

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.clinicalRecordId()).isEqualTo(30L);
        assertThat(response.petId()).isEqualTo(1L);
        assertThat(response.veterinarianId()).isEqualTo(2L);
        assertThat(response.medication()).isEqualTo("Amoxicilina");
        assertThat(response.dosage()).isEqualTo("250 mg");
        assertThat(response.frequency()).isEqualTo("cada 8 horas");
    }

    @Test
    void ownerCannotCreateTreatment() {
        User owner = user(7L, Role.OWNER);

        assertThatThrownBy(() -> treatmentService.create(request(30L), principal(owner)))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(treatmentRepository, never()).save(any(Treatment.class));
    }

    @Test
    void veterinarianCannotCreateForAnotherVetsRecord() {
        User vet = user(2L, Role.VETERINARIAN);
        User otherVet = user(3L, Role.VETERINARIAN);
        ClinicalRecord record = record(30L, user(7L, Role.OWNER), otherVet);
        when(clinicalRecordRepository.findById(30L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> treatmentService.create(request(30L), principal(vet)))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(treatmentRepository, never()).save(any(Treatment.class));
    }

    @Test
    void ownerCannotReadTreatmentOfAnotherPet() {
        User anotherOwner = user(8L, Role.OWNER);
        Treatment treatment = treatment(5L, record(30L, user(7L, Role.OWNER),
                user(2L, Role.VETERINARIAN)));
        when(treatmentRepository.findById(5L)).thenReturn(Optional.of(treatment));

        assertThatThrownBy(() -> treatmentService.get(5L, principal(anotherOwner)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void ownerCanReadTreatmentOfOwnPet() {
        User owner = user(7L, Role.OWNER);
        Treatment treatment = treatment(5L, record(30L, owner, user(2L, Role.VETERINARIAN)));
        when(treatmentRepository.findById(5L)).thenReturn(Optional.of(treatment));

        TreatmentResponse response = treatmentService.get(5L, principal(owner));

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.owner().id()).isEqualTo(7L);
    }

    @Test
    void onlyAuthorCanUpdateTreatment() {
        User vet = user(2L, Role.VETERINARIAN);
        User otherVet = user(3L, Role.VETERINARIAN);
        Treatment treatment = treatment(5L, record(30L, user(7L, Role.OWNER), vet));
        when(treatmentRepository.findById(5L)).thenReturn(Optional.of(treatment));

        assertThatThrownBy(() -> treatmentService.update(5L, request(30L), principal(otherVet)))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(treatmentRepository, never()).save(any(Treatment.class));
    }

    @Test
    void cannotChangeClinicalRecordOnUpdate() {
        User vet = user(2L, Role.VETERINARIAN);
        Treatment treatment = treatment(5L, record(30L, user(7L, Role.OWNER), vet));
        when(treatmentRepository.findById(5L)).thenReturn(Optional.of(treatment));

        assertThatThrownBy(() -> treatmentService.update(5L, request(31L), principal(vet)))
                .isInstanceOf(BadRequestException.class);

        verify(treatmentRepository, never()).save(any(Treatment.class));
    }

    @Test
    void ownerListIsScopedToOwnPets() {
        User owner = user(7L, Role.OWNER);
        when(treatmentRepository.search(eq(7L), isNull(), isNull(), eq(30L), any(Pageable.class)))
                .thenReturn(Page.empty());

        treatmentService.list(principal(owner), 30L, null, Pageable.unpaged());

        verify(treatmentRepository).search(eq(7L), isNull(), isNull(), eq(30L), any(Pageable.class));
    }

    @Test
    void veterinarianListIsScopedToOwnRecords() {
        User vet = user(2L, Role.VETERINARIAN);
        when(treatmentRepository.search(isNull(), isNull(), eq(2L), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        treatmentService.list(principal(vet), null, null, Pageable.unpaged());

        verify(treatmentRepository).search(isNull(), isNull(), eq(2L), isNull(), any(Pageable.class));
    }

    private TreatmentRequest request(Long clinicalRecordId) {
        return new TreatmentRequest(clinicalRecordId, "Amoxicilina", "250 mg",
                "cada 8 horas", "7 dias", "Administrar con agua");
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

    private Appointment appointment(Pet pet, User vet) {
        Appointment appointment = new Appointment();
        appointment.setId(1L);
        appointment.setPet(pet);
        appointment.setVeterinarian(vet);
        appointment.setScheduledAt(Instant.now().plus(1, ChronoUnit.DAYS));
        appointment.setDurationMinutes(30);
        appointment.setReason("Control");
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointment;
    }

    private ClinicalRecord record(Long id, User owner, User vet) {
        ClinicalRecord record = new ClinicalRecord();
        record.setId(id);
        record.setAppointment(appointment(pet(owner), vet));
        record.setPet(record.getAppointment().getPet());
        record.setVeterinarian(vet);
        record.setDiagnosis("Diagnostico");
        return record;
    }

    private Treatment treatment(Long id, ClinicalRecord record) {
        Treatment treatment = new Treatment();
        treatment.setId(id);
        treatment.setClinicalRecord(record);
        treatment.setMedication("Amoxicilina");
        treatment.setDosage("250 mg");
        treatment.setFrequency("cada 8 horas");
        treatment.setDuration("7 dias");
        treatment.setInstructions("Administrar con agua");
        return treatment;
    }
}
