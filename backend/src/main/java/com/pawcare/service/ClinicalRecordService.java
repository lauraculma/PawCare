package com.pawcare.service;

import com.pawcare.dto.clinical.ClinicalRecordRequest;
import com.pawcare.dto.clinical.ClinicalRecordResponse;
import com.pawcare.entity.Appointment;
import com.pawcare.entity.AppointmentStatus;
import com.pawcare.entity.ClinicalRecord;
import com.pawcare.entity.Role;
import com.pawcare.entity.User;
import com.pawcare.exception.BadRequestException;
import com.pawcare.exception.ConflictException;
import com.pawcare.exception.ForbiddenOperationException;
import com.pawcare.exception.ResourceNotFoundException;
import com.pawcare.repository.AppointmentRepository;
import com.pawcare.repository.ClinicalRecordRepository;
import com.pawcare.repository.UserRepository;
import com.pawcare.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClinicalRecordService {

    private final ClinicalRecordRepository clinicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public ClinicalRecordService(ClinicalRecordRepository clinicalRecordRepository,
                                 AppointmentRepository appointmentRepository,
                                 UserRepository userRepository) {
        this.clinicalRecordRepository = clinicalRecordRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<ClinicalRecordResponse> list(UserPrincipal principal,
                                             Long petId,
                                             Long appointmentId,
                                             Pageable pageable) {
        Long ownerFilter = null;
        Long vetFilter = null;

        if (principal.role() == Role.OWNER) {
            ownerFilter = principal.id();
        } else if (principal.role() == Role.VETERINARIAN && petId == null && appointmentId == null) {
            vetFilter = principal.id();
        }

        return clinicalRecordRepository
                .search(ownerFilter, petId, vetFilter, appointmentId, pageable)
                .map(ClinicalRecordResponse::from);
    }

    @Transactional(readOnly = true)
    public ClinicalRecordResponse get(Long id, UserPrincipal principal) {
        ClinicalRecord record = findRecord(id);
        assertReadAccess(record, principal);
        return ClinicalRecordResponse.from(record);
    }

    @Transactional
    public ClinicalRecordResponse create(ClinicalRecordRequest request, UserPrincipal principal) {
        assertStaff(principal);

        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Cita", request.appointmentId()));

        if (principal.role() == Role.VETERINARIAN
                && !appointment.getVeterinarian().getId().equals(principal.id())) {
            throw new ForbiddenOperationException("Solo puede registrar la historia de sus propias citas");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BadRequestException("No se puede registrar historia de una cita cancelada");
        }
        if (clinicalRecordRepository.existsByAppointmentId(appointment.getId())) {
            throw new ConflictException("La cita ya tiene un registro clinico");
        }

        User author = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", principal.id()));

        ClinicalRecord record = new ClinicalRecord();
        record.setAppointment(appointment);
        record.setPet(appointment.getPet());
        record.setVeterinarian(author);
        apply(record, request);

        return ClinicalRecordResponse.from(clinicalRecordRepository.save(record));
    }

    @Transactional
    public ClinicalRecordResponse update(Long id, ClinicalRecordRequest request, UserPrincipal principal) {
        assertStaff(principal);
        ClinicalRecord record = findRecord(id);

        if (principal.role() == Role.VETERINARIAN
                && !record.getVeterinarian().getId().equals(principal.id())) {
            throw new ForbiddenOperationException("Solo el autor puede modificar este registro");
        }
        if (!record.getAppointment().getId().equals(request.appointmentId())) {
            throw new BadRequestException("No es posible cambiar la cita asociada al registro");
        }

        apply(record, request);
        return ClinicalRecordResponse.from(clinicalRecordRepository.save(record));
    }

    private void apply(ClinicalRecord record, ClinicalRecordRequest request) {
        record.setDiagnosis(request.diagnosis().trim());
        record.setTreatment(normalize(request.treatment()));
        record.setPrescription(normalize(request.prescription()));
        record.setNotes(normalize(request.notes()));
    }

    private void assertReadAccess(ClinicalRecord record, UserPrincipal principal) {
        if (principal.role() == Role.OWNER
                && !record.getPet().getOwner().getId().equals(principal.id())) {
            throw new ForbiddenOperationException("No tiene acceso a este registro clinico");
        }
    }

    private void assertStaff(UserPrincipal principal) {
        if (principal.role() == Role.OWNER) {
            throw new ForbiddenOperationException("Solo el personal veterinario puede gestionar historias clinicas");
        }
    }

    private ClinicalRecord findRecord(Long id) {
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
