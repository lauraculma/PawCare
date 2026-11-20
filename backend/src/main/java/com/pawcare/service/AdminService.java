package com.pawcare.service;

import com.pawcare.dto.admin.AdminUserUpdateRequest;
import com.pawcare.dto.auth.UserResponse;
import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import com.pawcare.exception.ConflictException;
import com.pawcare.exception.ResourceNotFoundException;
import com.pawcare.repository.AlertRepository;
import com.pawcare.repository.AppointmentRepository;
import com.pawcare.repository.ClinicalRecordRepository;
import com.pawcare.repository.PetRepository;
import com.pawcare.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AlertRepository alertRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicalRecordRepository clinicalRecordRepository;
    private final PetRepository petRepository;

    public AdminService(UserRepository userRepository, AlertRepository alertRepository,
                        AppointmentRepository appointmentRepository,
                        ClinicalRecordRepository clinicalRecordRepository, PetRepository petRepository) {
        this.userRepository = userRepository;
        this.alertRepository = alertRepository;
        this.appointmentRepository = appointmentRepository;
        this.clinicalRecordRepository = clinicalRecordRepository;
        this.petRepository = petRepository;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> pendingVeterinarians(Pageable pageable) {
        return userRepository.findAllByRoleAndApprovedFalseOrderByCreatedAtAsc(Role.VETERINARIAN, pageable)
                .map(UserResponse::from);
    }

    @Transactional
    public UserResponse approveVeterinarian(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));
        if (user.getRole() != Role.VETERINARIAN) {
            throw new ConflictException("El usuario seleccionado no es una cuenta veterinaria");
        }
        if (!user.isActive()) {
            throw new ConflictException("No se puede aprobar una cuenta deshabilitada");
        }
        user.setApproved(true);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> users(Pageable pageable) {
        return userRepository.findByRoleIn(List.of(Role.OWNER, Role.VETERINARIAN), pageable)
                .map(UserResponse::from);
    }

    @Transactional
    public UserResponse updateUser(Long userId, AdminUserUpdateRequest request) {
        User user = findManageableUser(userId);
        String email = request.email().trim().toLowerCase();
        String cedula = request.cedula().trim();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new ConflictException("Ya existe un usuario registrado con ese correo");
        }
        if (userRepository.existsByCedulaAndIdNot(cedula, userId)) {
            throw new ConflictException("Ya existe un usuario registrado con esa cedula");
        }
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setCedula(cedula);
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = findManageableUser(userId);
        alertRepository.clearAcknowledgedBy(userId);
        if (user.getRole() == Role.VETERINARIAN) {
            clinicalRecordRepository.deleteAllByVeterinarianId(userId);
            appointmentRepository.deleteAllByVeterinarianId(userId);
        }
        if (user.getRole() == Role.OWNER) {
            petRepository.deleteAllByOwnerId(userId);
        }
        userRepository.delete(user);
    }

    private User findManageableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));
        if (user.getRole() != Role.OWNER && user.getRole() != Role.VETERINARIAN) {
            throw new ConflictException("Solo se pueden administrar cuentas de propietarios y veterinarios");
        }
        return user;
    }
}
