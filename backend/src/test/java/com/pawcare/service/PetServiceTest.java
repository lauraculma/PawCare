package com.pawcare.service;

import com.pawcare.dto.pet.PetRequest;
import com.pawcare.dto.pet.PetResponse;
import com.pawcare.entity.Pet;
import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import com.pawcare.exception.BadRequestException;
import com.pawcare.exception.ConflictException;
import com.pawcare.exception.ForbiddenOperationException;
import com.pawcare.repository.PetRepository;
import com.pawcare.repository.UserRepository;
import com.pawcare.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetRepository petRepository;
    @Mock
    private UserRepository userRepository;

    private PetService petService;

    @BeforeEach
    void setUp() {
        petService = new PetService(petRepository, userRepository);
    }

    @Test
    void ownerOnlySeesOwnPets() {
        User owner = user(7L, Role.OWNER);
        UserPrincipal principal = principal(owner);
        Pageable pageable = PageRequest.of(0, 20);
        Pet pet = pet(1L, owner);
        when(petRepository.search(7L, null, pageable)).thenReturn(new PageImpl<>(List.of(pet)));

        Page<PetResponse> result = petService.list(principal, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Luna");
    }

    @Test
    void ownerCannotReadAnotherOwnersPet() {
        User owner = user(7L, Role.OWNER);
        User another = user(8L, Role.OWNER);
        Pet pet = pet(1L, another);
        when(petRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(pet));

        assertThatThrownBy(() -> petService.get(1L, principal(owner)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void ownerCreatesPetForHimself() {
        User owner = user(7L, Role.OWNER);
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
        when(petRepository.existsByCollarIdIgnoreCase("PAW-001")).thenReturn(false);
        when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> {
            Pet saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        PetResponse response = petService.create(petRequest(null), principal(owner));

        assertThat(response.owner().id()).isEqualTo(7L);
        assertThat(response.collarId()).isEqualTo("PAW-001");
    }

    @Test
    void veterinarianMustProvideOwner() {
        User vet = user(2L, Role.VETERINARIAN);

        assertThatThrownBy(() -> petService.create(petRequest(null), principal(vet)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsDuplicatedCollar() {
        User owner = user(7L, Role.OWNER);
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
        when(petRepository.existsByCollarIdIgnoreCase("PAW-001")).thenReturn(true);

        assertThatThrownBy(() -> petService.create(petRequest(null), principal(owner)))
                .isInstanceOf(ConflictException.class);

        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    void deleteIsSoftAndClearsCollar() {
        User owner = user(7L, Role.OWNER);
        Pet pet = pet(1L, owner);
        when(petRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(pet));
        when(petRepository.save(pet)).thenReturn(pet);

        petService.delete(1L, principal(owner));

        assertThat(pet.isActive()).isFalse();
        assertThat(pet.getCollarId()).isNull();
    }

    @Test
    void onlyStaffCanSeeAllPets() {
        User vet = user(2L, Role.VETERINARIAN);
        Pageable pageable = PageRequest.of(0, 20);
        when(petRepository.search(eq(null), eq("luna"), eq(pageable))).thenReturn(Page.empty());

        petService.list(principal(vet), "luna", pageable);

        verify(petRepository).search(null, "luna", pageable);
    }

    private PetRequest petRequest(Long ownerId) {
        return new PetRequest("Luna", "Perro", "Mestiza", null, null, "Hembra", "PAW-001", null, ownerId);
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
        pet.setCollarId("PAW-001");
        pet.setActive(true);
        return pet;
    }
}
