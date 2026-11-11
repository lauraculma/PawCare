package com.pawcare.repository;

import com.pawcare.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {

    @Query("""
            select p from Pet p
            where p.active = true
              and (:ownerId is null or p.owner.id = :ownerId)
              and (:search is null or lower(p.name) like :search)
            """)
    Page<Pet> search(@Param("ownerId") Long ownerId,
                     @Param("search") String search,
                     Pageable pageable);

    Optional<Pet> findByIdAndActiveTrue(Long id);

    boolean existsByCollarIdIgnoreCaseAndIdNot(String collarId, Long id);

    boolean existsByCollarIdIgnoreCase(String collarId);
}
