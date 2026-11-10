package com.pawcare.service;

import com.pawcare.dto.vital.VitalSignRequest;
import com.pawcare.dto.vital.VitalSignResponse;
import com.pawcare.entity.Alert;
import com.pawcare.entity.AlertSeverity;
import com.pawcare.entity.AlertType;
import com.pawcare.entity.Pet;
import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import com.pawcare.entity.VitalSign;
import com.pawcare.exception.BadRequestException;
import com.pawcare.exception.ForbiddenOperationException;
import com.pawcare.repository.AlertRepository;
import com.pawcare.repository.PetRepository;
import com.pawcare.repository.VitalSignRepository;
import com.pawcare.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VitalSignServiceTest {

    @Mock
    private VitalSignRepository vitalSignRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private PetRepository petRepository;

    private VitalSignService vitalSignService;

    @BeforeEach
    void setUp() {
        vitalSignService = new VitalSignService(vitalSignRepository, alertRepository, petRepository);
    }

    @Test
    void normalValuesDoNotGenerateAlerts() {
        User owner = user(7L, Role.OWNER);
        Pet pet = pet(owner);
        when(petRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(pet));
        when(vitalSignRepository.save(any(VitalSign.class))).thenAnswer(invocation -> {
            VitalSign vital = invocation.getArgument(0);
            vital.setId(100L);
            return vital;
        });

        VitalSignResponse response = vitalSignService.ingest(
                new VitalSignRequest(1L, "PAW-001", 90, 98, 38.2, "REPOSO", null), principal(owner));

        assertThat(response.alerts()).isEmpty();
        verify(alertRepository, never()).saveAll(anyList());
    }

    @Test
    void abnormalValuesGenerateAlertsByThreshold() {
        User owner = user(7L, Role.OWNER);
        Pet pet = pet(owner);
        when(petRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(pet));
        when(vitalSignRepository.save(any(VitalSign.class))).thenAnswer(invocation -> {
            VitalSign vital = invocation.getArgument(0);
            vital.setId(101L);
            return vital;
        });
        when(alertRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        VitalSignResponse response = vitalSignService.ingest(
                new VitalSignRequest(1L, "PAW-001", 200, 88, 40.8, "ACTIVO", null), principal(owner));

        assertThat(response.alerts()).hasSize(3);
        assertThat(response.alerts()).extracting(a -> a.type())
                .containsExactlyInAnyOrder(AlertType.TACHYCARDIA, AlertType.LOW_SPO2, AlertType.FEVER);
        assertThat(response.alerts()).extracting(a -> a.severity())
                .containsOnly(AlertSeverity.CRITICAL);
    }

    @Test
    void ownerCannotIngestForAnotherPet() {
        User owner = user(7L, Role.OWNER);
        Pet pet = pet(user(8L, Role.OWNER));
        when(petRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(pet));

        assertThatThrownBy(() -> vitalSignService.ingest(
                new VitalSignRequest(1L, "PAW-001", 90, 98, 38.2, null, null), principal(owner)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void rejectsCollarMismatch() {
        User owner = user(7L, Role.OWNER);
        Pet pet = pet(owner);
        when(petRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(pet));

        assertThatThrownBy(() -> vitalSignService.ingest(
                new VitalSignRequest(1L, "OTRO-COLLAR", 90, 98, 38.2, null, null), principal(owner)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requiresAtLeastOneMetric() {
        User owner = user(7L, Role.OWNER);

        assertThatThrownBy(() -> vitalSignService.ingest(
                new VitalSignRequest(1L, null, null, null, null, null, null), principal(owner)))
                .isInstanceOf(BadRequestException.class);
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
        pet.setCollarId("PAW-001");
        pet.setActive(true);
        return pet;
    }
}
