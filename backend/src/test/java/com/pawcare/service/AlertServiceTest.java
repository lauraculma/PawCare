package com.pawcare.service;

import com.pawcare.dto.vital.AlertResponse;
import com.pawcare.entity.Alert;
import com.pawcare.entity.AlertSeverity;
import com.pawcare.entity.AlertType;
import com.pawcare.entity.Pet;
import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import com.pawcare.exception.ForbiddenOperationException;
import com.pawcare.repository.AlertRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private UserRepository userRepository;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository, userRepository);
    }

    @Test
    void ownerListIsFilteredByOwner() {
        User owner = user(7L, Role.OWNER);
        Pageable pageable = PageRequest.of(0, 50);
        when(alertRepository.search(7L, null, null, pageable)).thenReturn(Page.empty());

        Page<AlertResponse> result = alertService.list(principal(owner), null, null, pageable);

        assertThat(result).isEmpty();
        verify(alertRepository).search(7L, null, null, pageable);
    }

    @Test
    void ownerCannotAcknowledge() {
        User owner = user(7L, Role.OWNER);

        assertThatThrownBy(() -> alertService.acknowledge(1L, principal(owner)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void veterinarianAcknowledgesAlert() {
        User vet = user(2L, Role.VETERINARIAN);
        Alert alert = alert(10L, pet(owner()));
        when(alertRepository.findById(10L)).thenReturn(Optional.of(alert));
        when(userRepository.findById(2L)).thenReturn(Optional.of(vet));
        when(alertRepository.save(alert)).thenReturn(alert);

        AlertResponse response = alertService.acknowledge(10L, principal(vet));

        assertThat(response.acknowledged()).isTrue();
        assertThat(response.acknowledgedBy()).isEqualTo("Usuario 2");
        assertThat(alert.getAcknowledgedAt()).isNotNull();
    }

    private User owner() {
        return user(7L, Role.OWNER);
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

    private Alert alert(Long id, Pet pet) {
        Alert alert = new Alert();
        alert.setId(id);
        alert.setPet(pet);
        alert.setType(AlertType.TACHYCARDIA);
        alert.setSeverity(AlertSeverity.CRITICAL);
        alert.setMessage("Frecuencia cardiaca elevada");
        return alert;
    }
}
