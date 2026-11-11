package com.pawcare.repository;

import com.pawcare.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

    default Page<Alert> search(Long ownerId, Long petId, Boolean acknowledged, Pageable pageable) {
        Specification<Alert> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (ownerId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.join("pet").join("owner").get("id"), ownerId));
            }
            if (petId != null) {
                predicates.add(criteriaBuilder.equal(root.join("pet").get("id"), petId));
            }
            if (acknowledged != null) {
                predicates.add(criteriaBuilder.equal(root.get("acknowledged"), acknowledged));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
        return findAll(specification, pageable);
    }
}
