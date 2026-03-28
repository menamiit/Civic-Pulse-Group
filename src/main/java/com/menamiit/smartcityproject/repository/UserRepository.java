package com.menamiit.smartcityproject.repository;

import com.menamiit.smartcityproject.entity.Department;
import com.menamiit.smartcityproject.entity.User;
import com.menamiit.smartcityproject.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findAllByRole(UserRole role);
    List<User> findAllByRoleAndVerifiedTrueAndDepartment(UserRole role, Department department);
}
