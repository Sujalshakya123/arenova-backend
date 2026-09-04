package com.arenova.respositories;

import com.arenova.dtos.enums.Role;
import com.arenova.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail( String email);

    List<User> findByRole(Role role);

    long countByRole(Role role);

    long countByRoleAndStatus(Role role, com.arenova.dtos.enums.UserStatus status);
}
