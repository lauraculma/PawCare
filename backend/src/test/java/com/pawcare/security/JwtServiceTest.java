package com.pawcare.security;

import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("pawcare-test-secret-key-with-at-least-32-characters", 60);

    @Test
    void generatesAndValidatesToken() {
        User user = new User();
        user.setId(10L);
        user.setEmail("tutor@pawcare.test");
        user.setRole(Role.OWNER);
        user.setActive(true);

        String token = jwtService.generateToken(user);
        UserPrincipal principal = UserPrincipal.from(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("tutor@pawcare.test");
        assertThat(jwtService.isTokenValid(token, principal)).isTrue();
        assertThat(jwtService.getExpirationMinutes()).isEqualTo(60);
    }

    @Test
    void rejectsTokenFromAnotherKey() {
        User user = new User();
        user.setId(11L);
        user.setEmail("vet@pawcare.test");
        user.setRole(Role.VETERINARIAN);
        user.setActive(true);

        String token = jwtService.generateToken(user);
        JwtService other = new JwtService("another-secret-key-with-at-least-32-characters", 60);

        assertThat(other.isTokenValid(token, UserPrincipal.from(user))).isFalse();
    }
}
