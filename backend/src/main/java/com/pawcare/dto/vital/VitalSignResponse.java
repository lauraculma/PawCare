package com.pawcare.dto.vital;

import com.pawcare.entity.Alert;
import com.pawcare.entity.VitalSign;

import java.time.Instant;
import java.util.List;

public record VitalSignResponse(
        Long id,
        Long petId,
        String petName,
        String collarId,
        Integer heartRate,
        Integer spo2,
        Double temperatureC,
        String activityLevel,
        Instant recordedAt,
        Instant createdAt,
        List<AlertResponse> alerts
) {
    public static VitalSignResponse from(VitalSign vital, List<Alert> alerts) {
        return new VitalSignResponse(
                vital.getId(),
                vital.getPet().getId(),
                vital.getPet().getName(),
                vital.getCollarId(),
                vital.getHeartRate(),
                vital.getSpo2(),
                vital.getTemperatureC(),
                vital.getActivityLevel(),
                vital.getRecordedAt(),
                vital.getCreatedAt(),
                alerts.stream().map(AlertResponse::from).toList()
        );
    }
}
