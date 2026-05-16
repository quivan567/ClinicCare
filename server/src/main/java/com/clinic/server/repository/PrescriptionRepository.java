package com.clinic.server.repository;

import com.clinic.server.entity.PrescriptionEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface PrescriptionRepository extends MongoRepository<PrescriptionEntity, String> {
    List<PrescriptionEntity> findByPatientId(String patientId);
}
