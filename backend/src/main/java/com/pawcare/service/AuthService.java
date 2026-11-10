package com.pawcare.service;

import com.pawcare.dto.auth.AuthResponse;
import com.pawcare.dto.auth.LoginRequest;
import com.pawcare.dto.auth.RegisterRequest;
import com.pawcare.dto.auth.UserResponse;
import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import com.pawcare.exception.ConflictException;
import com.pawcare.exception.ForbiddenOperationException;
import com.pawcare.exception.ResourceNotFoundException;
import com.pawcare.repository.UserRepository;
import com.pawcare.security.JwtService;
import com.pawcare.security.UserPrincipal;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Ya existe un usuario registrado con ese correo");
        }

        Role role = request.role() == null ? Role.OWNER : request.role();
        if (role == Role.ADMIN) {
            throw new ForbiddenOperationException("No es posible registrarse con el rol de administrador");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setPhone(request.phone() == null ? null : request.phone().trim());
        user.setActive(true);

        User saved = userRepository.save(user);
        return buildResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));

        if (!user.isActive()) {
            throw new BadCredentialsException("La cuenta esta deshabilitada");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales invalidas");
        }

        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(UserPrincipal principal) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", principal.id()));
        return UserResponse.from(user);
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, jwtService.getExpirationMinutes(), UserResponse.from(user));
    }
}
