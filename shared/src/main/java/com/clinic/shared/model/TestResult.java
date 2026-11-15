package com.clinic.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class TestResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String patientId;
    private String patientCode;
    private String patientName;
    private String doctorId;
    private String doctorCode;
    private String doctorName;
    private String testingStaffId;
    private String testName;
    private String result;
    private String notes;
    private LocalDateTime createdAt;

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
    public String getDoctorCode() { return doctorCode; }
    public void setDoctorCode(String doctorCode) { this.doctorCode = doctorCode; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getTestingStaffId() { return testingStaffId; }
    public void setTestingStaffId(String testingStaffId) { this.testingStaffId = testingStaffId; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
