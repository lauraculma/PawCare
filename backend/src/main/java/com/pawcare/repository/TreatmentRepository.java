package com.pawcare.repository;

import com.pawcare.entity.Treatment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public interface TreatmentRepository extends JpaRepository<Treatment, Long>, JpaSpecificationExecutor<Treatment> {

    default Page<Treatment> search(Long ownerId,
                                   Long petId,
                                   Long veterinarianId,
                                   Long clinicalRecordId,
                                   Pageable pageable) {
        Specification<Treatment> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (ownerId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.join("clinicalRecord").join("pet").join("owner").get("id"), ownerId));
            }
            if (petId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.join("clinicalRecord").join("pet").get("id"), petId));
            }
            if (veterinarianId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.join("clinicalRecord").join("veterinarian").get("id"), veterinarianId));
            }
            if (clinicalRecordId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.join("clinicalRecord").get("id"), clinicalRecordId));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
        return findAll(specification, pageable);
    }
}
