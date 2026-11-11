package com.pawcare.service;

import com.pawcare.dto.auth.UserResponse;
import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import com.pawcare.exception.ConflictException;
import com.pawcare.exception.ResourceNotFoundException;
import com.pawcare.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}
