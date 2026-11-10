package com.pawcare.service;

import com.pawcare.dto.pet.PetRequest;
import com.pawcare.dto.pet.PetResponse;
import com.pawcare.entity.Pet;
import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import com.pawcare.exception.BadRequestException;
import com.pawcare.exception.ConflictException;
import com.pawcare.exception.ForbiddenOperationException;
import com.pawcare.exception.ResourceNotFoundException;
import com.pawcare.repository.PetRepository;
import com.pawcare.repository.UserRepository;
import com.pawcare.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PetService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;

    public PetService(PetRepository petRepository, UserRepository userRepository) {
        this.petRepository = petRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<PetResponse> list(UserPrincipal principal, String search, Pageable pageable) {
        Long ownerId = principal.role() == Role.OWNER ? principal.id() : null;
        String term = normalize(search);
        return petRepository.search(ownerId, term, pageable).map(PetResponse::from);
    }

    @Transactional(readOnly = true)
    public PetResponse get(Long id, UserPrincipal principal) {
        Pet pet = findActive(id);
        assertAccess(pet, principal);
        return PetResponse.from(pet);
    }

    @Transactional
    public PetResponse create(PetRequest request, UserPrincipal principal) {
        User owner = resolveOwner(request.ownerId(), principal);
        normalizeAndValidateCollar(request.collarId(), null);

        Pet pet = new Pet();
        pet.setOwner(owner);
        apply(pet, request);
        return PetResponse.from(petRepository.save(pet));
    }

    @Transactional
    public PetResponse update(Long id, PetRequest request, UserPrincipal principal) {
        Pet pet = findActive(id);
        assertAccess(pet, principal);
        normalizeAndValidateCollar(request.collarId(), id);

        if (principal.role() != Role.OWNER && request.ownerId() != null
                && !request.ownerId().equals(pet.getOwner().getId())) {
            User newOwner = userRepository.findById(request.ownerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario", request.ownerId()));
            pet.setOwner(newOwner);
        }

        apply(pet, request);
        return PetResponse.from(petRepository.save(pet));
    }

    @Transactional
    public void delete(Long id, UserPrincipal principal) {
        Pet pet = findActive(id);
        assertAccess(pet, principal);
        pet.setActive(false);
        pet.setCollarId(null);
        petRepository.save(pet);
    }

    private void apply(Pet pet, PetRequest request) {
        pet.setName(request.name().trim());
        pet.setSpecies(request.species().trim());
        pet.setBreed(normalize(request.breed()));
        pet.setBirthDate(request.birthDate());
        pet.setWeightKg(request.weightKg());
        pet.setSex(normalize(request.sex()));
        pet.setCollarId(normalize(request.collarId()));
        pet.setNotes(normalize(request.notes()));
    }

    private User resolveOwner(Long requestedOwnerId, UserPrincipal principal) {
        if (principal.role() == Role.OWNER) {
            if (requestedOwnerId != null && !requestedOwnerId.equals(principal.id())) {
                throw new ForbiddenOperationException("Solo puede registrar mascotas a su nombre");
            }
            return userRepository.findById(principal.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario", principal.id()));
        }

        if (requestedOwnerId == null) {
            throw new BadRequestException("Debe indicar el propietario de la mascota");
        }
        return userRepository.findById(requestedOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", requestedOwnerId));
    }

    private Pet findActive(Long id) {
        return petRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota", id));
    }

    private void assertAccess(Pet pet, UserPrincipal principal) {
        if (principal.role() == Role.OWNER && !pet.getOwner().getId().equals(principal.id())) {
            throw new ForbiddenOperationException("No tiene acceso a esta mascota");
        }
    }

    private void normalizeAndValidateCollar(String collarId, Long petId) {
        String normalized = normalize(collarId);
        if (normalized == null) {
            return;
        }

        boolean exists = petId == null
                ? petRepository.existsByCollarIdIgnoreCase(normalized)
                : petRepository.existsByCollarIdIgnoreCaseAndIdNot(normalized, petId);

        if (exists) {
            throw new ConflictException("El identificador del collar ya esta asociado a otra mascota");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
