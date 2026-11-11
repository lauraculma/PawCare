package com.pawcare.service;

import com.pawcare.dto.auth.AuthResponse;
import com.pawcare.dto.auth.RegistrationResponse;
import com.pawcare.dto.auth.LoginRequest;
import com.pawcare.dto.auth.RegisterRequest;
import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import com.pawcare.exception.ConflictException;
import com.pawcare.exception.ForbiddenOperationException;
import com.pawcare.repository.UserRepository;
import com.pawcare.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registersOwnerAndReturnsToken() {
        when(userRepository.existsByEmailIgnoreCase("nuevo@pawcare.test")).thenReturn(false);
        when(passwordEncoder.encode("secreto123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("token");
        when(jwtService.getExpirationMinutes()).thenReturn(120L);

        RegistrationResponse response = authService.register(new RegisterRequest(
                "Tutor Nuevo", "Nuevo@PawCare.test", "secreto123", "3000000000", null));

        assertThat(response.requiresApproval()).isFalse();
        assertThat(response.token()).isEqualTo("token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("nuevo@pawcare.test");
        assertThat(response.user().role()).isEqualTo(Role.OWNER);
        assertThat(response.user().approved()).isTrue();
    }


    @Test
    void registersVeterinarianWithoutIssuingTokenUntilApproved() {
        when(userRepository.existsByEmailIgnoreCase("vet@pawcare.test")).thenReturn(false);
        when(passwordEncoder.encode("secreto123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        RegistrationResponse response = authService.register(new RegisterRequest(
                "Veterinario", "vet@pawcare.test", "secreto123", null, Role.VETERINARIAN));

        assertThat(response.requiresApproval()).isTrue();
        assertThat(response.token()).isNull();
        assertThat(response.user().role()).isEqualTo(Role.VETERINARIAN);
        assertThat(response.user().approved()).isFalse();
    }

    @Test
    void pendingVeterinarianCannotLogin() {
        User vet = activeUser();
        vet.setRole(Role.VETERINARIAN);
        vet.setApproved(false);
        when(userRepository.findByEmailIgnoreCase("vet@pawcare.test")).thenReturn(Optional.of(vet));
        when(passwordEncoder.matches("secreto123", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("vet@pawcare.test", "secreto123")))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("pendiente de aprobacion");
    }

    @Test
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("dup@pawcare.test")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest(
                "Duplicado", "dup@pawcare.test", "secreto123", null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsAdminSelfRegistration() {
        when(userRepository.existsByEmailIgnoreCase("admin@pawcare.test")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(new RegisterRequest(
                "Admin", "admin@pawcare.test", "secreto123", null, Role.ADMIN)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = activeUser();
        when(userRepository.findByEmailIgnoreCase("tutor@pawcare.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("incorrecta", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("tutor@pawcare.test", "incorrecta")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = activeUser();
        when(userRepository.findByEmailIgnoreCase("tutor@pawcare.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt");
        when(jwtService.getExpirationMinutes()).thenReturn(120L);

        AuthResponse response = authService.login(new LoginRequest("tutor@pawcare.test", "secreto123"));

        assertThat(response.token()).isEqualTo("jwt");
        assertThat(response.user().role()).isEqualTo(Role.OWNER);
    }

    private User activeUser() {
        User user = new User();
        user.setId(5L);
        user.setFullName("Tutor");
        user.setEmail("tutor@pawcare.test");
        user.setPasswordHash("hash");
        user.setRole(Role.OWNER);
        user.setActive(true);
        return user;
    }
}
