package com.pawcare.repository;

import com.pawcare.entity.User;
import com.pawcare.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCedula(String cedula);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByCedulaAndIdNot(String cedula, Long id);

    Page<User> findByRoleIn(Collection<Role> roles, Pageable pageable);

    Optional<User> findByCedula(String cedula);

    Page<User> findAllByRoleAndApprovedFalseOrderByCreatedAtAsc(Role role, Pageable pageable);

    java.util.List<User> findAllByRoleAndApprovedTrueAndActiveTrueOrderByFullNameAsc(Role role);
}
