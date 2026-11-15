package com.clinic.server.repository;

import com.clinic.server.entity.PrescriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<PrescriptionEntity, String> {
    List<PrescriptionEntity> findByPatientId(String patientId);

    List<PrescriptionEntity> findByPatientIdIn(Collection<String> patientIds);

    List<PrescriptionEntity> findByDoctorId(String doctorId);

    List<PrescriptionEntity> findByDoctorIdIn(Collection<String> doctorIds);

    List<PrescriptionEntity> findByPatientIdInAndDoctorIdIn(Collection<String> patientIds, Collection<String> doctorIds);

    List<PrescriptionEntity> findByStatus(String status);

    @Query("""
            SELECT p FROM PrescriptionEntity p
            WHERE LOWER(COALESCE(p.status, '')) LIKE LOWER(:pattern) ESCAPE '\\'
            """)
    List<PrescriptionEntity> findByStatusPattern(@Param("pattern") String pattern);

    boolean existsByPatientId(String patientId);

    boolean existsByDoctorId(String doctorId);
}
