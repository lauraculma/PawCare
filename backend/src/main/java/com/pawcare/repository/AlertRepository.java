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
            where (:ownerId is null or a.pet.owner.id = :ownerId)
              and (:petId is null or a.pet.id = :petId)
              and (:acknowledged is null or a.acknowledged = :acknowledged)
            """)
    Page<Alert> search(@Param("ownerId") Long ownerId,
                       @Param("petId") Long petId,
                       @Param("acknowledged") Boolean acknowledged,
                       Pageable pageable);
}
