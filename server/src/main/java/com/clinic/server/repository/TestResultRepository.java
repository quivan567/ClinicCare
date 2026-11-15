package com.clinic.server.repository;

import com.clinic.server.entity.TestResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestResultRepository extends JpaRepository<TestResultEntity, String> {
    List<TestResultEntity> findByPatientId(String patientId);

    List<TestResultEntity> findByDoctorId(String doctorId);

    boolean existsByPatientId(String patientId);

    boolean existsByDoctorId(String doctorId);

    boolean existsByTestingStaffId(String testingStaffId);
}
