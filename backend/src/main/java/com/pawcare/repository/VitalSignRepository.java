package com.pawcare.repository;

import com.pawcare.entity.VitalSign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface VitalSignRepository extends JpaRepository<VitalSign, Long> {

    @Query("""
            select v from VitalSign v
            where v.pet.id = :petId
              and v.recordedAt >= :from
              and v.recordedAt <= :to
            """)
    Page<VitalSign> search(@Param("petId") Long petId,
                           @Param("from") Instant from,
                           @Param("to") Instant to,
                           Pageable pageable);
}
