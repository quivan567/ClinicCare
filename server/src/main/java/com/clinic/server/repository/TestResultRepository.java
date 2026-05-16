package com.clinic.server.repository;

import com.clinic.server.entity.TestResultEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TestResultRepository extends MongoRepository<TestResultEntity, String> {
    List<TestResultEntity> findByPatientId(String patientId);
}
