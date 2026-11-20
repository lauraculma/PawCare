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
        User owner = resolveOwner(request.ownerCedula(), principal);
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

        if (principal.role() != Role.OWNER && request.ownerCedula() != null
                && !request.ownerCedula().equals(pet.getOwner().getCedula())) {
            User newOwner = findOwnerByCedula(request.ownerCedula());
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

    private User resolveOwner(String requestedOwnerCedula, UserPrincipal principal) {
        if (principal.role() == Role.OWNER) {
            User owner = userRepository.findById(principal.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario", principal.id()));
            if (requestedOwnerCedula != null && !requestedOwnerCedula.equals(owner.getCedula())) {
                throw new ForbiddenOperationException("Solo puede registrar mascotas a su nombre");
            }
            return owner;
        }

        if (requestedOwnerCedula == null || requestedOwnerCedula.isBlank()) {
            throw new BadRequestException("Debe indicar la cédula del propietario de la mascota");
        }
        return findOwnerByCedula(requestedOwnerCedula);
    }

    private User findOwnerByCedula(String cedula) {
        User owner = userRepository.findByCedula(cedula.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Propietario", cedula));
        if (owner.getRole() != Role.OWNER) {
            throw new BadRequestException("La cédula indicada no pertenece a un propietario");
        }
        return owner;
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
