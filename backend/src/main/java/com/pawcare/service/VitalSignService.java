package com.pawcare.service;

import com.pawcare.dto.vital.VitalSignRequest;
import com.pawcare.dto.vital.VitalSignResponse;
import com.pawcare.entity.Alert;
import com.pawcare.entity.AlertSeverity;
import com.pawcare.entity.AlertType;
import com.pawcare.entity.Pet;
import com.pawcare.entity.Role;
import com.pawcare.entity.VitalSign;
import com.pawcare.exception.BadRequestException;
import com.pawcare.exception.ForbiddenOperationException;
import com.pawcare.exception.ResourceNotFoundException;
import com.pawcare.repository.AlertRepository;
import com.pawcare.repository.PetRepository;
import com.pawcare.repository.VitalSignRepository;
import com.pawcare.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class VitalSignService {

    private final VitalSignRepository vitalSignRepository;
    private final AlertRepository alertRepository;
    private final PetRepository petRepository;

    public VitalSignService(VitalSignRepository vitalSignRepository,
                            AlertRepository alertRepository,
                            PetRepository petRepository) {
        this.vitalSignRepository = vitalSignRepository;
        this.alertRepository = alertRepository;
        this.petRepository = petRepository;
    }

    @Transactional
    public VitalSignResponse ingest(VitalSignRequest request, UserPrincipal principal) {
        if (request.heartRate() == null && request.spo2() == null && request.temperatureC() == null) {
            throw new BadRequestException("Debe enviar al menos un signo vital");
        }

        Pet pet = findActivePet(request.petId());
        assertPetAccess(pet, principal);

        if (pet.getCollarId() != null && request.collarId() != null
                && !pet.getCollarId().equalsIgnoreCase(request.collarId().trim())) {
            throw new BadRequestException("El collar no corresponde a la mascota indicada");
        }

        VitalSign vital = new VitalSign();
        vital.setPet(pet);
        vital.setCollarId(normalize(request.collarId()));
        vital.setHeartRate(request.heartRate());
        vital.setSpo2(request.spo2());
        vital.setTemperatureC(request.temperatureC());
        vital.setActivityLevel(normalize(request.activityLevel()));
        vital.setRecordedAt(request.recordedAt() == null ? Instant.now() : request.recordedAt());

        VitalSign saved = vitalSignRepository.save(vital);
        List<Alert> alerts = evaluate(saved);
        List<Alert> savedAlerts = alerts.isEmpty() ? List.of() : alertRepository.saveAll(alerts);

        return VitalSignResponse.from(saved, savedAlerts);
    }

    @Transactional(readOnly = true)
    public Page<VitalSignResponse> list(Long petId, Instant from, Instant to,
                                        UserPrincipal principal, Pageable pageable) {
        Pet pet = findActivePet(petId);
        assertPetAccess(pet, principal);
        Instant fromEffective = from == null ? Instant.parse("0001-01-01T00:00:00Z") : from;
        Instant toEffective = to == null ? Instant.parse("9999-12-31T23:59:59Z") : to;
        return vitalSignRepository.search(petId, fromEffective, toEffective, pageable)
                .map(vital -> VitalSignResponse.from(vital, List.of()));
    }

    private List<Alert> evaluate(VitalSign vital) {
        List<Alert> alerts = new ArrayList<>();

        Integer heartRate = vital.getHeartRate();
        if (heartRate != null) {
            if (heartRate < 50) {
                alerts.add(alert(vital, AlertType.BRADYCARDIA, AlertSeverity.WARNING,
                        "Frecuencia cardiaca baja: " + heartRate + " lpm"));
            } else if (heartRate > 180) {
                alerts.add(alert(vital, AlertType.TACHYCARDIA, AlertSeverity.CRITICAL,
                        "Frecuencia cardiaca elevada: " + heartRate + " lpm"));
            }
        }

        Integer spo2 = vital.getSpo2();
        if (spo2 != null) {
            if (spo2 < 90) {
                alerts.add(alert(vital, AlertType.LOW_SPO2, AlertSeverity.CRITICAL,
                        "Saturacion de oxigeno critica: " + spo2 + "%"));
            } else if (spo2 < 94) {
                alerts.add(alert(vital, AlertType.LOW_SPO2, AlertSeverity.WARNING,
                        "Saturacion de oxigeno baja: " + spo2 + "%"));
            }
        }

        Double temperature = vital.getTemperatureC();
        if (temperature != null) {
            if (temperature < 37.0) {
                alerts.add(alert(vital, AlertType.HYPOTHERMIA, AlertSeverity.WARNING,
                        "Temperatura corporal baja: " + temperature + " C"));
            } else if (temperature > 40.5) {
                alerts.add(alert(vital, AlertType.FEVER, AlertSeverity.CRITICAL,
                        "Fiebre alta: " + temperature + " C"));
            } else if (temperature > 39.5) {
                alerts.add(alert(vital, AlertType.FEVER, AlertSeverity.WARNING,
                        "Temperatura corporal elevada: " + temperature + " C"));
            }
        }

        return alerts;
    }

    private Alert alert(VitalSign vital, AlertType type, AlertSeverity severity, String message) {
        Alert alert = new Alert();
        alert.setPet(vital.getPet());
        alert.setVitalSign(vital);
        alert.setType(type);
        alert.setSeverity(severity);
        alert.setMessage(message);
        return alert;
    }

    private Pet findActivePet(Long petId) {
        return petRepository.findByIdAndActiveTrue(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota", petId));
    }

    private void assertPetAccess(Pet pet, UserPrincipal principal) {
        if (principal.role() == Role.OWNER && !pet.getOwner().getId().equals(principal.id())) {
            throw new ForbiddenOperationException("No tiene acceso a esta mascota");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
