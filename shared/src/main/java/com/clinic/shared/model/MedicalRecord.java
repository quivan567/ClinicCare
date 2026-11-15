package com.clinic.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class MedicalRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String patientId;
    private String patientCode;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private String diagnosis;
    private String notes;
    private LocalDateTime createdAt;

    public MedicalRecord() {
    }

    public MedicalRecord(String id, String patientId, String doctorId, String diagnosis, String notes, LocalDateTime createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.diagnosis = diagnosis;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getPatientCode() { return patientCode; }
    public void setPatientCode(String patientCode) { this.patientCode = patientCode; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
