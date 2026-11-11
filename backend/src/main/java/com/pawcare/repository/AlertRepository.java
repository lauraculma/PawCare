package com.pawcare.repository;

import com.pawcare.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    @Query("""
            select a from Alert a
            where a.pet.owner.id = coalesce(:ownerId, a.pet.owner.id)
              and a.pet.id = coalesce(:petId, a.pet.id)
              and a.acknowledged = coalesce(:acknowledged, a.acknowledged)
            """)
    Page<Alert> search(@Param("ownerId") Long ownerId,
                       @Param("petId") Long petId,
                       @Param("acknowledged") Boolean acknowledged,
                       Pageable pageable);
}
