package com.pawcare.repository;

import com.pawcare.entity.ClinicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public interface ClinicalRecordRepository extends JpaRepository<ClinicalRecord, Long>, JpaSpecificationExecutor<ClinicalRecord> {

    default Page<ClinicalRecord> search(Long ownerId,
                                        Long petId,
                                        Long veterinarianId,
                                        Long appointmentId,
                                        Pageable pageable) {
        Specification<ClinicalRecord> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (ownerId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.join("pet").join("owner").get("id"), ownerId));
            }
            if (petId != null) {
                predicates.add(criteriaBuilder.equal(root.join("pet").get("id"), petId));
            }
            if (veterinarianId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.join("veterinarian").get("id"), veterinarianId));
            }
            if (appointmentId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.join("appointment").get("id"), appointmentId));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
        return findAll(specification, pageable);
    }

    boolean existsByAppointmentId(Long appointmentId);
}
