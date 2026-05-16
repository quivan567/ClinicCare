package com.clinic.server.repository;

import com.clinic.server.entity.PatientEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface PatientRepository extends MongoRepository<PatientEntity, String> {
    Optional<PatientEntity> findByCode(String code);
}
