package com.pawcare.service;

import com.pawcare.dto.treatment.TreatmentRequest;
import com.pawcare.dto.treatment.TreatmentResponse;
import com.pawcare.entity.ClinicalRecord;
import com.pawcare.entity.Role;
import com.pawcare.entity.Treatment;
import com.pawcare.exception.BadRequestException;
import com.pawcare.exception.ForbiddenOperationException;
import com.pawcare.exception.ResourceNotFoundException;
import com.pawcare.repository.ClinicalRecordRepository;
import com.pawcare.repository.TreatmentRepository;
import com.pawcare.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TreatmentService {

    private final TreatmentRepository treatmentRepository;
    private final ClinicalRecordRepository clinicalRecordRepository;

    public TreatmentService(TreatmentRepository treatmentRepository,
                            ClinicalRecordRepository clinicalRecordRepository) {
        this.treatmentRepository = treatmentRepository;
        this.clinicalRecordRepository = clinicalRecordRepository;
    }

    @Transactional(readOnly = true)
    public Page<TreatmentResponse> list(UserPrincipal principal,
                                        Long clinicalRecordId,
                                        Long petId,
                                        Pageable pageable) {
        Long ownerFilter = principal.role() == Role.OWNER ? principal.id() : null;
        Long veterinarianFilter = principal.role() == Role.VETERINARIAN ? principal.id() : null;

        return treatmentRepository
                .search(ownerFilter, petId, veterinarianFilter, clinicalRecordId, pageable)
                .map(TreatmentResponse::from);
    }

    @Transactional(readOnly = true)
    public TreatmentResponse get(Long id, UserPrincipal principal) {
        Treatment treatment = findTreatment(id);
        assertReadAccess(treatment, principal);
        return TreatmentResponse.from(treatment);
    }

    @Transactional
    public TreatmentResponse create(TreatmentRequest request, UserPrincipal principal) {
        assertStaff(principal);
        ClinicalRecord record = findClinicalRecord(request.clinicalRecordId());
        assertWriteAccess(record, principal);

        Treatment treatment = new Treatment();
        treatment.setClinicalRecord(record);
        apply(treatment, request);

        return TreatmentResponse.from(treatmentRepository.save(treatment));
    }

    @Transactional
    public TreatmentResponse update(Long id, TreatmentRequest request, UserPrincipal principal) {
        assertStaff(principal);
        Treatment treatment = findTreatment(id);
        assertWriteAccess(treatment.getClinicalRecord(), principal);

        if (!treatment.getClinicalRecord().getId().equals(request.clinicalRecordId())) {
            throw new BadRequestException("No es posible cambiar el registro clinico asociado al tratamiento");
        }

        apply(treatment, request);
        return TreatmentResponse.from(treatmentRepository.save(treatment));
    }

    private void apply(Treatment treatment, TreatmentRequest request) {
        treatment.setMedication(request.medication().trim());
        treatment.setDosage(request.dosage().trim());
        treatment.setFrequency(request.frequency().trim());
        treatment.setDuration(normalize(request.duration()));
        treatment.setInstructions(normalize(request.instructions()));
    }

    private void assertReadAccess(Treatment treatment, UserPrincipal principal) {
        ClinicalRecord record = treatment.getClinicalRecord();
        if (principal.role() == Role.OWNER
                && !record.getPet().getOwner().getId().equals(principal.id())) {
            throw new ForbiddenOperationException("No tiene acceso a este tratamiento");
        }
        if (principal.role() == Role.VETERINARIAN
                && !record.getVeterinarian().getId().equals(principal.id())) {
            throw new ForbiddenOperationException("No tiene acceso a este tratamiento");
        }
    }

    private void assertWriteAccess(ClinicalRecord record, UserPrincipal principal) {
        if (principal.role() == Role.VETERINARIAN
                && !record.getVeterinarian().getId().equals(principal.id())) {
            throw new ForbiddenOperationException("Solo puede registrar tratamientos de sus propias consultas");
        }
    }

    private void assertStaff(UserPrincipal principal) {
        if (principal.role() == Role.OWNER) {
            throw new ForbiddenOperationException("Solo el personal veterinario puede gestionar tratamientos");
        }
    }

    private Treatment findTreatment(Long id) {
        return treatmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tratamiento", id));
    }

    private ClinicalRecord findClinicalRecord(Long id) {
        return clinicalRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro clinico", id));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
