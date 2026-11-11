package com.pawcare.config;

import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import com.pawcare.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrapConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapConfiguration.class);

    @Bean
    @ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
    ApplicationRunner bootstrapAdmin(UserRepository users, PasswordEncoder passwordEncoder,
                                     org.springframework.core.env.Environment environment) {
        return args -> {
            String email = environment.getProperty("app.bootstrap-admin.email", "").trim().toLowerCase();
            String password = environment.getProperty("app.bootstrap-admin.password", "");
            String fullName = environment.getProperty("app.bootstrap-admin.full-name", "Administrador PawCare");

            if (email.isBlank() || password.length() < 12) {
                throw new IllegalStateException("Para crear el administrador inicial define BOOTSTRAP_ADMIN_EMAIL y una BOOTSTRAP_ADMIN_PASSWORD de al menos 12 caracteres.");
            }

            var existing = users.findByEmailIgnoreCase(email);
            if (existing.isPresent()) {
                if (existing.get().getRole() != Role.ADMIN) {
                    throw new IllegalStateException("BOOTSTRAP_ADMIN_EMAIL ya pertenece a una cuenta que no tiene rol ADMIN. Usa un correo distinto.");
                }
                log.info("La cuenta administradora indicada para bootstrap ya existe; no se modifico.");
                return;
            }

            User admin = new User();
            admin.setFullName(fullName);
            admin.setEmail(email);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            admin.setApproved(true);
            users.save(admin);
            log.info("Se creo la cuenta inicial de administrador: {}", email);
        };
    }
}
