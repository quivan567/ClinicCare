package com.clinic.server.repository;

import com.clinic.server.entity.UserEntity;
import com.clinic.shared.model.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<UserEntity, String> {
    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByUsernameAndPassword(String username, String password);

    boolean existsByUsername(String username);

    List<UserEntity> findByRole(Role role);
}
