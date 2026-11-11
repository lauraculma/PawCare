package com.pawcare.dto.vital;

import com.pawcare.entity.Alert;
import com.pawcare.entity.AlertSeverity;
import com.pawcare.entity.AlertType;

import java.time.Instant;

public record AlertResponse(
        Long id,
        Long petId,
        String petName,
        Long vitalSignId,
        AlertType type,
        AlertSeverity severity,
        String message,
        boolean acknowledged,
        String acknowledgedBy,
        Instant acknowledgedAt,
        Instant createdAt
) {
    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getPet().getId(),
                alert.getPet().getName(),
                alert.getVitalSign() == null ? null : alert.getVitalSign().getId(),
                alert.getType(),
                alert.getSeverity(),
                alert.getMessage(),
                alert.isAcknowledged(),
                alert.getAcknowledgedBy() == null ? null : alert.getAcknowledgedBy().getFullName(),
                alert.getAcknowledgedAt(),
                alert.getCreatedAt()
        );
    }
}
