package com.clinic.server.repository;

import com.clinic.server.entity.UserEntity;
import com.clinic.shared.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByUsernameAndPassword(String username, String password);

    boolean existsByUsername(String username);

    List<UserEntity> findByRole(Role role);

    @Query("""
            SELECT u FROM UserEntity u
            WHERE LOWER(u.username) LIKE LOWER(:pattern) ESCAPE '\\'
               OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(:pattern) ESCAPE '\\'
            """)
    List<UserEntity> findByUsernameOrFullName(@Param("pattern") String pattern);

    @Query("""
            SELECT u FROM UserEntity u
            WHERE LOWER(u.username) LIKE LOWER(:pattern) ESCAPE '\\'
               OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(:pattern) ESCAPE '\\'
               OR LOWER(CAST(u.role AS String)) LIKE LOWER(:pattern) ESCAPE '\\'
            """)
    List<UserEntity> searchByKeyword(@Param("pattern") String pattern);
}
