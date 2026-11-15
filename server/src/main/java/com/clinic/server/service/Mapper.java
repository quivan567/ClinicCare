package com.clinic.server.service;

import com.clinic.server.entity.MedicalRecordEntity;
import com.clinic.server.entity.PatientEntity;
import com.clinic.server.entity.PrescriptionEntity;
import com.clinic.server.entity.PrescriptionItemEntity;
import com.clinic.server.entity.UserEntity;
import com.clinic.shared.model.MedicalRecord;
import com.clinic.shared.model.Patient;
import com.clinic.shared.model.Prescription;
import com.clinic.shared.model.PrescriptionItem;
import com.clinic.shared.model.User;

import java.util.ArrayList;
import java.util.List;

public final class Mapper {
    private Mapper() {
    }

    public static User toModel(UserEntity e) {
        if (e == null) {
            return null;
        }
        User m = new User();
        m.setId(e.getId());
        m.setUsername(e.getUsername());
        m.setRole(e.getRole());
        m.setFullName(e.getFullName());
        m.setActive(e.isActive());
        return m;
    }

    public static Patient toModel(PatientEntity e) {
        if (e == null) {
            return null;
        }
        Patient m = new Patient();
        m.setId(e.getId());
        m.setCode(e.getCode());
        m.setFullName(e.getFullName());
        m.setAge(e.getAge());
        m.setGender(e.getGender());
        m.setPhone(e.getPhone());
        m.setAddress(e.getAddress());
        m.setAssignedDoctorId(e.getAssignedDoctorId());
        m.setStatus(e.getStatus());
        m.setLastVisitAt(e.getLastVisitAt());
        return m;
    }

    public static Prescription toModel(PrescriptionEntity e) {
        if (e == null) {
            return null;
        }
        Prescription m = new Prescription();
        m.setId(e.getId());
        m.setCode(e.getCode());
        m.setPatientId(e.getPatientId());
        m.setDoctorId(e.getDoctorId());
        m.setItems(toItems(e.getItems()));
        m.setStatus(e.getStatus());
        m.setCreatedAt(e.getCreatedAt());
        return m;
    }

    private static List<PrescriptionItem> toItems(List<PrescriptionItemEntity> items) {
        List<PrescriptionItem> models = new ArrayList<>();
        if (items == null) {
            return models;
        }
        for (PrescriptionItemEntity item : items) {
            models.add(new PrescriptionItem(item.getMedicineName(), item.getDosage(), item.getInstructions()));
        }
        return models;
    }

    public static MedicalRecord toModel(MedicalRecordEntity e) {
        if (e == null) {
            return null;
        }
        MedicalRecord m = new MedicalRecord();
        m.setId(e.getId());
        m.setPatientId(e.getPatientId());
        m.setDoctorId(e.getDoctorId());
        m.setDiagnosis(e.getDiagnosis());
        m.setNotes(e.getNotes());
        m.setCreatedAt(e.getCreatedAt());
        return m;
    }
}
