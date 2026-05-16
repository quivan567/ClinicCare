package com.clinic.shared.dto;

import java.io.Serializable;

public class MedicalRecordRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String patientId;
    private String doctorId;
    private String diagnosis;
    private String notes;
    private boolean requiresTest;

    public MedicalRecordRequest() {
    }

    public MedicalRecordRequest(String patientId, String doctorId, String diagnosis, String notes, boolean requiresTest) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.diagnosis = diagnosis;
        this.notes = notes;
        this.requiresTest = requiresTest;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isRequiresTest() { return requiresTest; }
    public void setRequiresTest(boolean requiresTest) { this.requiresTest = requiresTest; }
}
