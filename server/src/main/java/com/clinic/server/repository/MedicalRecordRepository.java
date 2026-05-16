package com.clinic.server.repository;

import com.clinic.server.entity.MedicalRecordEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MedicalRecordRepository extends MongoRepository<MedicalRecordEntity, String> {
    List<MedicalRecordEntity> findByPatientId(String patientId);

    List<MedicalRecordEntity> findByDoctorId(String doctorId);
}
