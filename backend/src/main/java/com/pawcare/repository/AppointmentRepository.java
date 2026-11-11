package com.pawcare.repository;

import com.pawcare.entity.Appointment;
import com.pawcare.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    default Page<Appointment> search(Long ownerId,
                                     Long veterinarianId,
                                     Long petId,
                                     AppointmentStatus status,
                                     Instant from,
                                     Instant to,
                                     Pageable pageable) {
        Specification<Appointment> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (ownerId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.join("pet").join("owner").get("id"), ownerId));
            }
            if (veterinarianId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.join("veterinarian").get("id"), veterinarianId));
            }
            if (petId != null) {
                predicates.add(criteriaBuilder.equal(root.join("pet").get("id"), petId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("scheduledAt"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("scheduledAt"), to));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
        return findAll(specification, pageable);
    }

    boolean existsByVeterinarian_IdAndStatusNotAndScheduledAtLessThanAndEndAtGreaterThan(
            Long veterinarianId,
            AppointmentStatus excludedStatus,
            Instant scheduledAtBefore,
            Instant endAtAfter);

    boolean existsByVeterinarian_IdAndStatusNotAndScheduledAtLessThanAndEndAtGreaterThanAndIdNot(
            Long veterinarianId,
            AppointmentStatus excludedStatus,
            Instant scheduledAtBefore,
            Instant endAtAfter,
            Long excludedId);
}
