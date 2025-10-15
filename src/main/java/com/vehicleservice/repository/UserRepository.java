package com.vehicleservice.repository;

// Import statements for user repository functionality
import com.vehicleservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@org.springframework.context.annotation.Scope("singleton")
public interface UserRepository extends JpaRepository<User, Long> {
        Optional<User> findByUsername(String username);

        Optional<User> findByEmail(String email);

        Optional<User> findByPhoneNumber(String phoneNumber);

        boolean existsByUsername(String username);

        boolean existsByEmail(String email);

        boolean existsByPhoneNumber(String phoneNumber);

        @Query("SELECT u FROM User u WHERE u.role.roleName = :roleName")
        List<User> findByRoleName(@Param("roleName") String roleName);

        List<User> findByRoleRoleName(String roleName);

        @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.email LIKE %:keyword%")
        List<User> findByUsernameOrEmailContaining(@Param("keyword") String keyword);

        @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.email LIKE %:keyword%")
        Page<User> findByUsernameOrEmailContaining(@Param("keyword") String keyword, Pageable pageable);

        // Filter by role with pagination
        @Query("SELECT u FROM User u WHERE u.role.roleName = :roleName")
        Page<User> findByRoleRoleName(@Param("roleName") String roleName, Pageable pageable);

        // Filter by status with pagination
        @Query("SELECT u FROM User u WHERE u.isActive = :isActive")
        Page<User> findByIsActive(@Param("isActive") boolean isActive, Pageable pageable);

        // Filter by role and status with pagination
        @Query("SELECT u FROM User u WHERE u.role.roleName = :roleName AND u.isActive = :isActive")
        Page<User> findByRoleRoleNameAndIsActive(@Param("roleName") String roleName,
                        @Param("isActive") boolean isActive,
                        Pageable pageable);

        // Search with role filter
        @Query("SELECT u FROM User u WHERE (u.username LIKE %:keyword% OR u.email LIKE %:keyword%) AND u.role.roleName = :roleName")
        Page<User> findByUsernameOrEmailContainingAndRoleRoleName(@Param("keyword") String keyword,
                        @Param("roleName") String roleName, Pageable pageable);

        // Search with status filter
        @Query("SELECT u FROM User u WHERE (u.username LIKE %:keyword% OR u.email LIKE %:keyword%) AND u.isActive = :isActive")
        Page<User> findByUsernameOrEmailContainingAndIsActive(@Param("keyword") String keyword,
                        @Param("isActive") boolean isActive, Pageable pageable);

        // Search with role and status filters
        @Query("SELECT u FROM User u WHERE (u.username LIKE %:keyword% OR u.email LIKE %:keyword%) AND u.role.roleName = :roleName AND u.isActive = :isActive")
        Page<User> findByUsernameOrEmailContainingAndRoleRoleNameAndIsActive(@Param("keyword") String keyword,
                        @Param("roleName") String roleName, @Param("isActive") boolean isActive, Pageable pageable);
}
