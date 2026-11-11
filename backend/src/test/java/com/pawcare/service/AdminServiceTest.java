package com.pawcare.service;

import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import com.pawcare.exception.ConflictException;
import com.pawcare.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void approvesPendingVeterinarian() {
        User veterinarian = new User();
        veterinarian.setId(8L);
        veterinarian.setFullName("Veterinaria Demo");
        veterinarian.setEmail("vet@pawcare.test");
        veterinarian.setRole(Role.VETERINARIAN);
        veterinarian.setActive(true);
        veterinarian.setApproved(false);
        when(userRepository.findById(8L)).thenReturn(Optional.of(veterinarian));
        when(userRepository.save(veterinarian)).thenReturn(veterinarian);

        var result = adminService.approveVeterinarian(8L);

        assertThat(result.approved()).isTrue();
        assertThat(result.role()).isEqualTo(Role.VETERINARIAN);
    }

    @Test
    void refusesToApproveNonVeterinarian() {
        User owner = new User();
        owner.setId(9L);
        owner.setRole(Role.OWNER);
        when(userRepository.findById(9L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> adminService.approveVeterinarian(9L))
                .isInstanceOf(ConflictException.class);
    }
}
