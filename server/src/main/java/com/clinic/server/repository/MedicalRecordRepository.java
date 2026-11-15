package com.clinic.server.repository;

import com.clinic.server.entity.MedicalRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecordEntity, String> {
    List<MedicalRecordEntity> findByPatientId(String patientId);

    List<MedicalRecordEntity> findByDoctorId(String doctorId);

    boolean existsByPatientId(String patientId);

    boolean existsByDoctorId(String doctorId);
}
