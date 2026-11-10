package com.pawcare.service;

import com.pawcare.dto.vital.AlertResponse;
import com.pawcare.entity.Alert;
import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import com.pawcare.exception.ForbiddenOperationException;
import com.pawcare.exception.ResourceNotFoundException;
import com.pawcare.repository.AlertRepository;
import com.pawcare.repository.UserRepository;
import com.pawcare.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    public AlertService(AlertRepository alertRepository, UserRepository userRepository) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<AlertResponse> list(UserPrincipal principal, Long petId, Boolean acknowledged,
                                    Pageable pageable) {
        Long ownerFilter = principal.role() == Role.OWNER ? principal.id() : null;
        return alertRepository.search(ownerFilter, petId, acknowledged, pageable)
                .map(AlertResponse::from);
    }

    @Transactional
    public AlertResponse acknowledge(Long id, UserPrincipal principal) {
        if (principal.role() == Role.OWNER) {
            throw new ForbiddenOperationException("Solo el personal veterinario puede reconocer alertas");
        }

        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta", id));

        if (!alert.isAcknowledged()) {
            User user = userRepository.findById(principal.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario", principal.id()));
            alert.setAcknowledged(true);
            alert.setAcknowledgedBy(user);
            alert.setAcknowledgedAt(Instant.now());
            alert = alertRepository.save(alert);
        }

        return AlertResponse.from(alert);
    }
}
