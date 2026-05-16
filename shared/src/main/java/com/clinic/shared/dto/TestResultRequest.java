package com.clinic.shared.dto;

import java.io.Serializable;

public class TestResultRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String patientId;
    private String doctorId;
    private String testingStaffId;
    private String testName;
    private String result;
    private String notes;

    public TestResultRequest() {
    }

    public TestResultRequest(String patientId, String doctorId, String testingStaffId, String testName, String result, String notes) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.testingStaffId = testingStaffId;
        this.testName = testName;
        this.result = result;
        this.notes = notes;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getTestingStaffId() { return testingStaffId; }
    public void setTestingStaffId(String testingStaffId) { this.testingStaffId = testingStaffId; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
