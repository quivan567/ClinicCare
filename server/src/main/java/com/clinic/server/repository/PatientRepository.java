package com.clinic.server.repository;

import com.clinic.server.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<PatientEntity, String> {
    Optional<PatientEntity> findByCode(String code);

    List<PatientEntity> findByAssignedDoctorId(String assignedDoctorId);

    List<PatientEntity> findByAssignedDoctorIdAndStatusIn(String assignedDoctorId, Collection<String> statuses);

    List<PatientEntity> findByStatus(String status);

    List<PatientEntity> findByStatusIn(Collection<String> statuses);

    boolean existsByAssignedDoctorId(String assignedDoctorId);

    @Query("""
            SELECT p FROM PatientEntity p
            WHERE LOWER(p.code) LIKE LOWER(:pattern) ESCAPE '\\'
            """)
    List<PatientEntity> findByCodePattern(@Param("pattern") String pattern);

    @Query("""
            SELECT p FROM PatientEntity p
            WHERE LOWER(p.code) LIKE LOWER(:pattern) ESCAPE '\\'
               OR LOWER(p.fullName) LIKE LOWER(:pattern) ESCAPE '\\'
               OR LOWER(COALESCE(p.phone, '')) LIKE LOWER(:pattern) ESCAPE '\\'
               OR LOWER(COALESCE(p.status, '')) LIKE LOWER(:pattern) ESCAPE '\\'
            """)
    List<PatientEntity> searchByKeyword(@Param("pattern") String pattern);
}
